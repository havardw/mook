package mook;

import io.quarkus.test.junit.QuarkusTest;
import mook.test.DeleteFiles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ImageService.
 */
@QuarkusTest
public class ImageServiceTest {

    private ImageService service;

    private Path base;

    @Inject
    DataSource ds;

    @BeforeEach
    public void setUp() throws Exception {
        base = Files.createTempDirectory("ImageServiceTest");
        service = new ImageService(base.toString(), ds, null);

    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.walkFileTree(base, new DeleteFiles());
    }

    @Test
    public void savePng() throws Exception {
        URL imageUrl = getClass().getResource("/image.png");

        byte[] img = Files.readAllBytes(Paths.get(imageUrl.toURI()));

        Image result = service.saveImage(img, 2);

        assertThat(result.name()).matches("\\d+\\.png");
        Path imgPath = Paths.get(base.toString(), "original", result.name());
        assertThat(imgPath).exists();
    }

    @Test
    public void saveJpg() throws Exception {
        URL imageUrl = getClass().getResource("/image.jpg");
        byte[] img = Files.readAllBytes(Paths.get(imageUrl.toURI()));

        Image result = service.saveImage(img, 2);

        assertThat(result.name()).matches("\\d+\\.jpg");
        Path imgPath = Paths.get(base.toString(), "original", result.name());
        assertThat(imgPath).exists();
    }


    @Test
    public void saveUnsupported() throws Exception {
        URL imageUrl = getClass().getResource("/image.gif");
        byte[] img = Files.readAllBytes(Paths.get(imageUrl.toURI()));

        assertThatThrownBy(() -> service.saveImage(img, 2)).isInstanceOf(IllegalArgumentException.class);
    }
}
