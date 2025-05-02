package mook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

@Singleton
public class EntryService {

    private static final Logger log = LoggerFactory.getLogger(EntryService.class);
    
    private final DataSource ds;

    @Inject
    public EntryService(DataSource ds) {
        this.ds = ds;
    }

    public Collection<Entry> getEntries(int siteId, int offset, int count) {
        Map<Integer, Entry> result = new HashMap<>();
    
        try (Connection con = ds.getConnection())  {
            PreparedStatement ps = con.prepareStatement("SELECT e.id, e.entrydate, e.entryText, u.name, e.siteId " +
                                                   "FROM entry e, users u " + 
                                                   "WHERE e.userId = u.id AND e.siteId = ? " +
                                                   "ORDER BY e.entrydate DESC, id DESC " +
                                                   "LIMIT ? OFFSET ?");
            ps.setInt(1, siteId);
            ps.setInt(2, count);
            ps.setInt(3, offset);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                result.put(id, new Entry(id, rs.getString("name"), rs.getString("entrytext"), rs.getDate("entrydate"), new ArrayList<>()));
            }
            
            // May get an empty result with an offset larger than item count
            if (!result.isEmpty()) {
                String ids = result.keySet().stream().map(i -> Integer.toString(i)).collect(Collectors.joining(", "));

                rs = con.createStatement().executeQuery(String.format("select * from image where entryId in (%s)", ids));
                while (rs.next()) {
                    int id = rs.getInt("id");
                    Image img = new Image(id,
                            id + "." + ImageService.extensionFromMimeType(rs.getString("mimeType")),
                            rs.getString("caption"));
                    int entryId = rs.getInt("entryId");
                    result.get(entryId).images().add(img);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query for entries failed", e);
        }

        return result.values();
    }

    public int saveEntry(String text, Date date, List<Image> images, int userId, int siteId) {
        try (Connection con = ds.getConnection()){
            con.setAutoCommit(false);
            PreparedStatement ps = con.prepareStatement("insert into entry (entryDate, entryText, userId, siteId) values(?, ?, ?, ?)",
                                                        Statement.RETURN_GENERATED_KEYS);
            ps.setDate(1, new java.sql.Date(date.getTime()));
            ps.setString(2, text);
            ps.setInt(3, userId);
            ps.setInt(4, siteId);
            ps.executeUpdate();
    
            int entryId;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                entryId = rs.getInt(1);
            }
    
            if (images != null && !images.isEmpty()) {
                PreparedStatement ips = con.prepareStatement("update image set entryId = ?, caption = ?, siteId = ? where id = ?");
                for (Image image : images) {
                    ips.setInt(1, entryId);
                    ips.setString(2, image.caption());
                    ips.setInt(3, siteId);
                    ips.setInt(4, image.id());
                    int changed = ips.executeUpdate();
                    if (changed == 0) {
                        throw new IllegalStateException(String.format("Image not updated, no image with ID %d?", image.id()));
                    }
                }

                log.info("Saved new entry ID {} with {} image(s) for site {}", entryId, images.size(), siteId);
            } else {
                log.info("Saved new entry ID {} without images for site {}", entryId, siteId);
            }

            con.commit();

            return entryId;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save entry", e);
        }
    }
}