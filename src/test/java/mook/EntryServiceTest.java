package mook;

import io.quarkus.test.junit.QuarkusTest;
import mook.test.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import javax.sql.DataSource;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for EntryService.
 */
@QuarkusTest
public class EntryServiceTest {

    private EntryService service;

    @Inject
    DataSource ds;

    @BeforeEach
    public void setUp() {
        service = new EntryService(ds);

    }

    @Test
    public void saveEntry() {
        TestDatabase.insert(ds, "insert into image (userId, siteId, mimeType) values (?, ?, ?)", 1, 1, "image/test");

        int id = service.saveEntry("Entry", new Date(), Collections.singletonList(new Image(1, null, "Test")), 1, 1);

        Map<String, Object> entry = TestDatabase.querySingleRow(ds, "entry", "id", id);
        assertThat(entry.get("entrytext")).isEqualTo("Entry");

        Map<String, Object> image = TestDatabase.querySingleRow(ds, "image", "id", 1);
        assertThat(image.get("entryid")).isEqualTo(id);
        assertThat(image.get("caption")).isEqualTo("Test");
    }

    @Test()
    public void saveEntryWithMissingImage() {
        assertThatThrownBy(() -> service.saveEntry("Entry", new Date(), Collections.singletonList(new Image(2, null, "Test")), 1, 1))
                .isInstanceOf(IllegalStateException.class);
    }

}