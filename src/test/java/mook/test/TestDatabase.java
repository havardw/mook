package mook.test;

import org.hsqldb.jdbc.JDBCDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database helper for unit tests.
 */
public class TestDatabase {
    
    private TestDatabase() {}

    /**
     * Create a new in-memory database with default schema.
     *
     * @param name The name for the database, typically the name of the test.
     * @return Data source with empty database
     */
    public static DataSource get(String name) {
        JDBCDataSource ds = new JDBCDataSource();
        ds.setURL("jdbc:hsqldb:mem:ImageServiceTest;sql.syntax_mys=true");
        ds.setUser("sa");
        ds.setPassword("");

        try {
            String sql = new String(Files.readAllBytes(Paths.get("mook.sql")), StandardCharsets.UTF_8);
            try (Connection conn = ds.getConnection()) {
                conn.createStatement().executeUpdate(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("failed to create database");
        }

        return ds;
    }
}
