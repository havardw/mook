package mook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ApplicationScoped
public class PermissionsService {
    
    private static final Logger log = LoggerFactory.getLogger(PermissionsService.class.getName());
    
    private final DataSource ds;
    
    @Inject
    public PermissionsService(DataSource ds) {
        this.ds = ds;
    }
    
    /**
     * Checks if the user has admin or edit access to the site with the given slug.
     * 
     * @param siteSlug Slug of the site to check
     * @param userId ID of the user to check
     * @return The site ID if the user has access
     * @throws SecurityException if the user does not have access or the site does not exist
     */
    public int checkUserHasAccess(String siteSlug, int userId) {
        try (Connection conn = ds.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT s.id FROM sites s " +
                "JOIN permissions p ON s.id = p.siteId " +
                "WHERE s.slug = ? AND p.userId = ? AND p.permission IN ('admin', 'edit')")
        ) {
            stmt.setString(1, siteSlug);
            stmt.setInt(2, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                
                throw new RuntimeException("User does not have access to this site");
            }
        } catch (SQLException e) {
            log.error("Error checking site access permissions", e);
            throw new RuntimeException("Could not verify site access", e);
        }
    }
}
