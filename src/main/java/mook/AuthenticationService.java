package mook;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.*;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Singleton
@Slf4j
public class AuthenticationService {
    
    //private static HashMap<String, AuthenticationData> sessions = new HashMap<>();

    private final DataSource dataSource;

    @Inject
    public AuthenticationService(DataSource dataSource) {
        this.dataSource = dataSource;
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
                        String uuid = UUID.randomUUID().toString();
                        auth = new AuthenticationData(rs.getInt("id"), loginData.getEmail(), rs.getString("name"), uuid);
                    } else {
                        log.warn("Login for user {} failed", loginData.getEmail());
                        throw new AuthenticationException("Login failed");
                    }
                }
            }

            // Save session to database
            try (PreparedStatement ps = con.prepareStatement("insert into userSession (uuid, userId, expires) values(?, ?, ?)")) {
                long expiresSeconds = ZonedDateTime.now().plus(1, ChronoUnit.WEEKS).toEpochSecond();

                ps.setString(1, auth.getToken());
                ps.setInt(2, auth.getId());
                ps.setTimestamp(3, new Timestamp(expiresSeconds * 1000));

                ps.executeUpdate();
            }

            return auth;
        } catch (SQLException se) {
            throw new RuntimeException("Database error", se);
        }
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
                        throw new AuthenticationException("No matching session for token");
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
