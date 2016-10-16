package mook;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

@Singleton
@Slf4j
public class AuthenticationService {
    
    private static HashMap<String, AuthenticationData> sessions = new HashMap<>();

    private final DataSource dataSource;

    @Inject
    public AuthenticationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public AuthenticationData login(LoginData loginData)  {
        log.info("Login attempt for user {}", loginData.getEmail());

        try (Connection con = dataSource.getConnection()) {
            PreparedStatement ps = con.prepareStatement("select id, name from user where email=? and hash=SHA2(?, 512)");
            ps.setString(1, loginData.getEmail());
            ps.setString(2, loginData.getPassword());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String uuid = UUID.randomUUID().toString();
                AuthenticationData auth = new AuthenticationData(rs.getInt("id"), loginData.getEmail(), rs.getString("name"), uuid);
                sessions.put(uuid, auth);
                return auth;
            } else {
                log.warn("Login for user {} failed", loginData.getEmail());
                throw new AuthenticationException("Login failed");
            }
        } catch (SQLException se) {
            log.error("Database error", se);
            throw new RuntimeException(se);
        }
    }
    
    public boolean isAuthenticated(String token) {
        AuthenticationData a = sessions.get(token);
        return a != null;
    }
    
    public AuthenticationData getAuthenticationData(String token) {
        AuthenticationData a = sessions.get(token);
        if (a != null) {
            return a;
        } else {
            throw new AuthenticationException("No matching session for token");
        }        
    }
    
    public MookPrincipal getPrincipal(String token) {
        AuthenticationData a = getAuthenticationData(token);
        return new MookPrincipal(a.getId(), a.getEmail(), a.getDisplayName());
    }
}
