package mook;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Singleton
@Slf4j
public class EntryService {

    private final DataSource ds;

    @Inject
    public EntryService(DataSource ds) {
        this.ds = ds;
    }

    public List<Entry> getEntries() {
        ArrayList<Entry> result = new ArrayList<>();

        try (Connection con = ds.getConnection())  {
            ResultSet rs = con.createStatement().executeQuery("SELECT e.id, e.entrydate, e.entryText, u.name " +
                                                              "FROM entry e,user u WHERE e.userId = u.id " +
                                                              "ORDER BY e.entrydate DESC LIMIT 30");
            while (rs.next()) {
                result.add(new Entry(rs.getString("u.name"), rs.getString("e.entrytext"), rs.getDate("e.entrydate")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query for entries failed", e);
        }

        return result;
    }
    
    public void saveEntry(String text, Date date, int userId) {
        try (Connection con = ds.getConnection()){
            PreparedStatement ps = con.prepareStatement("insert into entry (entryDate, entryText, userId) values(?, ?, ?)");
            ps.setDate(1, new java.sql.Date(date.getTime()));
            ps.setString(2, text);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save entry", e);
        }
    }
}