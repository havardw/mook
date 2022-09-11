package mook.test;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Database helper for unit tests.
 */
public class TestDatabase {
    
    private TestDatabase() {}

    public static void insert(DataSource ds, String query, Object... params) {
        try (Connection conn = ds.getConnection())  {
            try (PreparedStatement ps = conn.prepareStatement(query))  {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        switch (params[i].getClass().getSimpleName()) {
                            case "String" -> ps.setString(i + 1, (String) params[i]);
                            case "Integer" -> ps.setInt(i + 1, (Integer) params[i]);
                            case "Long" -> ps.setLong(i + 1, (Long) params[i]);
                            default -> ps.setObject(i + 1, params[i]);
                        }
                    }
                }
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update database", e);
        }
    }

    public static Map<String, Object> querySingleRow(DataSource ds, String table, String column, Object value) {
        try (Connection conn = ds.getConnection()) {
            String query = "select * from " + table + " where " + column + "=?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setObject(1, value);
                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, Object> result = new HashMap<>();
                    if (rs.next()) {
                        ResultSetMetaData meta = rs.getMetaData();

                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            String label = meta.getColumnLabel(i).toLowerCase();
                            result.put(label, rs.getObject(label));
                        }
                    } else {
                        throw new IllegalStateException("No rows found for query");
                    }
                    if (rs.next()) {
                        throw new IllegalStateException("More than one row found for query");
                    }

                    return result;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query");
        }
    }
}
