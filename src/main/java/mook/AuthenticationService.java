package mook;
import java.util.ArrayList;
import java.util.List;
import io.quarkus.scheduler.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
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
                        int userId = rs.getInt("id");

                        // Check if user has permissions for any sites
                        List<SitePermission> permissions = getSitePermissionsForUser(con, userId);
                        if (permissions.isEmpty()) {
                            log.warn("Login for user {} failed: no site permissions", loginData.email());
                            throw new AuthenticationException(AuthenticationException.Reason.PASSWORD_MISMATCH);
                        }

                        auth = createSession(con, userId, loginData.email(), rs.getString("name"));
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
                        int userId = rs.getInt("id");

                        // Check if user has permissions for any sites
                        List<SitePermission> permissions = getSitePermissionsForUser(con, userId);
                        if (permissions.isEmpty()) {
                            log.warn("OIDC login for user {} failed: no site permissions", email);
                            throw new AuthenticationException(AuthenticationException.Reason.OAUTH_NOT_REGISTERED, email);
                        }

                        return createSession(con, userId, email, rs.getString("name"));
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
        UUID uuid = UUID.randomUUID();

        try (PreparedStatement ps = con.prepareStatement("insert into userSession (uuid, userId, expires) values(?, ?, ?)")) {
            long expiresSeconds = ZonedDateTime.now().plus(1, ChronoUnit.WEEKS).toEpochSecond();

            ps.setObject(1, uuid);
            ps.setInt(2, userId);
            ps.setTimestamp(3, new Timestamp(expiresSeconds * 1000));

            ps.executeUpdate();
        }

        return new AuthenticationData(userId, email, name, uuid, getSitePermissionsForUser(con, userId));
    }

    public boolean isAuthenticated(String token) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "select count(*) from userSession where uuid=? and expires > CURRENT_TIMESTAMP";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setObject(1, UUID.fromString(token));
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
        UUID uuid = UUID.fromString(token);
        try (Connection conn = dataSource.getConnection()) {
                // Get user data
                String query = "select u.id, u.email, u.name " +
                        "from users u, userSession us " +
                        "where us.expires > CURRENT_TIMESTAMP and us.uuid=? and us.userId=u.id;";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setObject(1, uuid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int userId = rs.getInt("id");
                            String email = rs.getString("email");
                            String name = rs.getString("name");

                            // Get permissions for this user
                            List<SitePermission> permissions = getSitePermissionsForUser(conn, userId);

                            return new AuthenticationData(userId, email, name, uuid, permissions);
                        } else {
                            throw new AuthenticationException(AuthenticationException.Reason.SESSION_EXPIRED);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Database error", e);
            }
        }

        private List<SitePermission> getSitePermissionsForUser(Connection conn, int userId) throws SQLException {
            List<SitePermission> permissions = new ArrayList<>();

            String permQuery = "SELECT s.name, s.slug, p.permission " +
                               "FROM permissions p " +
                               "JOIN sites s ON p.siteId = s.id " +
                               "WHERE p.userId = ?";

            try (PreparedStatement ps = conn.prepareStatement(permQuery)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        String slug = rs.getString("slug");
                        String permission = rs.getString("permission");

                        AuthenticationData.Permission permEnum = null;
                        if (permission.equalsIgnoreCase("admin")) {
                            permEnum = AuthenticationData.Permission.ADMIN;
                        } else if (permission.equalsIgnoreCase("edit")) {
                            permEnum = AuthenticationData.Permission.EDIT;
                        }

                        if (permEnum != null) {
                            permissions.add(new SitePermission(name, slug, permEnum));
                        }
                    }
                }
            }

            return permissions;
    }

    public MookPrincipal getPrincipal(String token) {
        AuthenticationData a = getAuthenticationData(token);
        return new MookPrincipal(a.id(), a.email(), a.displayName());
    }

    public void logout(String token) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "DELETE FROM userSession WHERE uuid=?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setObject(1, UUID.fromString(token));
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    log.info("User logged out, session deleted");
                } else {
                    log.warn("Logout attempted for non-existent session");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during logout", e);
        }
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
