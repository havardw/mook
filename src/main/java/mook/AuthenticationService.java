package mook;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

@Singleton
@Slf4j
public class AuthenticationService {
    
    private final DataSource dataSource;

    private final String googleClientId;

    @Inject
    public AuthenticationService(DataSource dataSource, @Named("Google client ID") String googleClientId) {
        this.dataSource = dataSource;
        this.googleClientId = googleClientId;
    }

    public AuthenticationData login(LoginData loginData)  {
        log.info("Login attempt for user {}", loginData.getEmail());

        try (Connection con = dataSource.getConnection()) {
            AuthenticationData auth;

            // Verify password and get user data
            try (PreparedStatement ps = con.prepareStatement("select id, name from user where email=? and hash=SHA2(?, 512)")) {
                ps.setString(1, loginData.getEmail());
                ps.setString(2, loginData.getPassword());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        auth = createSession(con, rs.getInt("id"), loginData.getEmail(), rs.getString("name"));
                    } else {
                        log.warn("Login for user {} failed", loginData.getEmail());
                        throw new AuthenticationException(AuthenticationException.Reason.PASSWORD_MISMATCH);
                    }
                }
            }

            return auth;
        } catch (SQLException se) {
            throw new RuntimeException("Database error", se);
        }
    }

    public AuthenticationData verifyGoogleLogin(String tokenId) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                // Specify the CLIENT_ID of the app that accesses the backend:
                .setAudience(Collections.singletonList(googleClientId))
                // Or, if multiple clients access the backend:
                //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
                .build();

        log.debug("Google ID token: {}", tokenId);
        try {
            GoogleIdToken idToken = verifier.verify(tokenId);

            String email = idToken.getPayload().getEmail();
            log.info("Verified Google login for {}", email);

            // Get user data
            try (Connection con = dataSource.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("select id, name from user where email=?")) {
                    ps.setString(1, email);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return createSession(con, rs.getInt("id"), email, rs.getString("name"));
                        } else {
                            log.warn("Google user {} not in database", email);
                            throw new AuthenticationException(AuthenticationException.Reason.OAUTH_NOT_REGISTERED);
                        }
                    }
                }
            } catch (SQLException se) {
                throw new RuntimeException("Database error", se);
            }
        } catch (GeneralSecurityException e) {
            log.error("Security exception from Google token verify", e);
            throw new AuthenticationException(AuthenticationException.Reason.OAUTH_CONFIG);
        } catch (IOException e) {
            log.warn("IO exception from Google token verify", e);
            throw new AuthenticationException(AuthenticationException.Reason.NETWORK);
        }


    }

    private AuthenticationData createSession(Connection con, int userId, String email, String name) throws SQLException {
        String uuid = UUID.randomUUID().toString();

        try (PreparedStatement ps = con.prepareStatement("insert into userSession (uuid, userId, expires) values(?, ?, ?)")) {
            long expiresSeconds = ZonedDateTime.now().plus(1, ChronoUnit.WEEKS).toEpochSecond();

            ps.setString(1, uuid);
            ps.setInt(2, userId);
            ps.setTimestamp(3, new Timestamp(expiresSeconds * 1000));

            ps.executeUpdate();
        }

        return new AuthenticationData(userId, email, name, uuid);
    }

    public boolean isAuthenticated(String token) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "select count(*) from userSession where uuid=? and expires > now()";
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
                    "from user u, userSession us " +
                    "where us.expires > now() and us.uuid=? and us.userId=u.id;";
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
        return new MookPrincipal(a.getId(), a.getEmail(), a.getDisplayName());
    }

    public void cleanExpiredTokens() {
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                int deleted = statement.executeUpdate("delete from userSession where expires < now()");
                if (deleted > 0) {
                    log.debug("Cleared {} expired sessions", deleted);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }
}
