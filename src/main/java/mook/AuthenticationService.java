package mook;

import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final DataSource dataSource;

    @Inject
    public AuthenticationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public AuthenticationData login(LoginData loginData)  {
        log.info("Login attempt for user {}", loginData.email());

        try (Connection con = dataSource.getConnection()) {
            AuthenticationData auth;

            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(loginData.password().getBytes(StandardCharsets.UTF_8));

            // Verify password and get user data
            try (PreparedStatement ps = con.prepareStatement("select id, name from users where email=? and hash=?")) {
                ps.setString(1, loginData.email());
                ps.setBytes(2, hash);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        auth = createSession(con, rs.getInt("id"), loginData.email(), rs.getString("name"));
                    } else {
                        log.warn("Login for user {} failed", loginData.email());
                        throw new AuthenticationException(AuthenticationException.Reason.PASSWORD_MISMATCH);
                    }
                }
            }

            return auth;
        } catch (SQLException se) {
            throw new RuntimeException("Database error for login", se);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to use SHA-512");
        }
    }

    public AuthenticationData verifyOidc(String accessToken) {
        String email;
        try {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("https://openidconnect.googleapis.com/v1/userinfo");
            Invocation.Builder request = target.request();
            request.header("Authorization", "Bearer " + accessToken);

            Response response = request.get();
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                log.error("Received status " + response.getStatus() + " from userinfo: " + response.readEntity(String.class));
                throw new AuthenticationException(AuthenticationException.Reason.UNKNOWN);
            }

            Map<String, Object> result =  response.readEntity(new GenericType<Map<String, Object>>() { });

            if (!result.containsKey("email")) {
                log.error("No email in userinfo response: " + result);
                throw new AuthenticationException(AuthenticationException.Reason.OAUTH_CONFIG);
            }

            email = result.get("email").toString();
        } catch (ProcessingException e) {
            log.warn("IO exception from Google token verify", e);
            throw new AuthenticationException(AuthenticationException.Reason.NETWORK);
        }

        log.info("Verified OIDC login for {}", email);

        // Get user data
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select id, name from users where email=?")) {
                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return createSession(con, rs.getInt("id"), email, rs.getString("name"));
                    } else {
                        log.warn("OIDC user {} not in database", email);
                        throw new AuthenticationException(AuthenticationException.Reason.OAUTH_NOT_REGISTERED, email);
                    }
                }
            }
        } catch (SQLException se) {
            throw new RuntimeException("Database error", se);
        }
    }

    private AuthenticationData createSession(Connection con, int userId, String email, String name) throws SQLException {
        String uuid = UUID.randomUUID().toString();

        try (PreparedStatement ps = con.prepareStatement("insert into userSession (uuid, userId, expires) values(?, ?, ?)")) {
            long expiresSeconds = ZonedDateTime.now().plus(1, ChronoUnit.WEEKS).toEpochSecond();

            ps.setString(1, uuid);
            ps.setInt(2, userId);
            ps.setDate(3, new Date(expiresSeconds * 1000));

            ps.executeUpdate();
        }

        return new AuthenticationData(userId, email, name, uuid);
    }

    public boolean isAuthenticated(String token) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "select count(*) from userSession where uuid=? and expires > CURRENT_TIMESTAMP";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    int count = rs.getInt(1);
                    return count != 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }
    
    public AuthenticationData getAuthenticationData(String token) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "select u.id, u.email, u.name " +
                    "from users u, userSession us " +
                    "where us.expires > CURRENT_TIMESTAMP and us.uuid=? and us.userId=u.id;";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new AuthenticationData(rs.getInt("id"),
                                                      rs.getString("email"),
                                                      rs.getString("name"),
                                                      token);
                    } else {
                        throw new AuthenticationException(AuthenticationException.Reason.SESSION_EXPIRED);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }
    
    public MookPrincipal getPrincipal(String token) {
        AuthenticationData a = getAuthenticationData(token);
        return new MookPrincipal(a.id(), a.email(), a.displayName());
    }

    @Scheduled(every = "1h")
    public void cleanExpiredTokens() {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                int deleted = statement.executeUpdate("delete from userSession where expires < CURRENT_TIMESTAMP");
                if (deleted > 0) {
                    log.debug("Cleared {} expired sessions", deleted);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }
}
