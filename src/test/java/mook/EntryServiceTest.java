package mook;

import mook.test.TestDatabase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for EntryService.
 */
public class EntryServiceTest {

    private EntryService service;

    private static DataSource ds;

    @BeforeClass
    public static void initDb() {
        ds = TestDatabase.get("EntryServiceTest");
    }

    @Before
    public void setUp() throws Exception {
        service = new EntryService(ds);

    }

    @Test
    public void saveEntry() throws Exception {
        TestDatabase.insert(ds, "insert into image (userId, mimeType) values (?, ?)", 1, "image/test");

        int id = service.saveEntry("Entry", new Date(), Collections.singletonList(new Image(1, null, "Test")), 7);

        Map<String, Object> entry = TestDatabase.querySingleRow(ds, "entry", "id", id);
        assertEquals("Entry", entry.get("entrytext"));

        Map<String, Object> image = TestDatabase.querySingleRow(ds, "image", "id", 1);
        assertEquals(id, image.get("entryid"));
        assertEquals("Test", image.get("caption"));
    }

    @Test(expected = IllegalStateException.class)
    public void saveEntryWithMissingImage() throws Exception {
        service.saveEntry("Entry", new Date(), Collections.singletonList(new Image(2, null, "Test")), 7);
    }

}