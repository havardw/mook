package mook;

import mook.test.DeleteFiles;
import mook.test.TestDatabase;
import org.junit.*;

import javax.sql.DataSource;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.jcabi.matchers.RegexMatchers.matchesPattern;
import static org.junit.Assert.*;

/**
 * Unit tests for ImageService.
 */
public class ImageServiceTest {

    private ImageService service;

    private Path base;

    private static DataSource ds;

    @BeforeClass
    public static void setUpDb() throws Exception {
        ds = TestDatabase.get("ImageServiceTest");
    }

    @Before
    public void setUp() throws Exception {
        base = Files.createTempDirectory("ImageServiceTest");
        service = new ImageService(base.toString(), ds);

    }

    @After
    public void tearDown() throws Exception {
        Files.walkFileTree(base, new DeleteFiles());
    }

    @Test
    public void savePng() throws Exception {
        URL imageUrl = getClass().getResource("/image.png");

        byte[] img = Files.readAllBytes(Paths.get(imageUrl.toURI()));

        Image result = service.saveImage(img, 2);

        assertThat(result.getName(), matchesPattern("\\d+\\.png"));
        Path imgPath = Paths.get(base.toString(), "original", result.getName());
        assertTrue(String.format("Path '%s' does not exist", imgPath), Files.exists(imgPath));
    }

    @Test
    public void saveJpg() throws Exception {
        URL imageUrl = getClass().getResource("/image.jpg");
        byte[] img = Files.readAllBytes(Paths.get(imageUrl.toURI()));

        Image result = service.saveImage(img, 2);

        assertThat(result.getName(), matchesPattern("\\d+\\.jpg"));
        Path imgPath = Paths.get(base.toString(), "original", result.getName());
        assertTrue(String.format("Path '%s' does not exist", imgPath), Files.exists(imgPath));
    }


    @Test(expected = IllegalArgumentException.class)
    public void saveUnsupported() throws Exception {
        URL imageUrl = getClass().getResource("/image.gif");
        byte[] img = Files.readAllBytes(Paths.get(imageUrl.toURI()));

        service.saveImage(img, 2);
    }
}
