package mook;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.sql.DataSource;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Data service for images.
 */
@ApplicationScoped
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    
    private static final byte[] MAGIC_PNG = { -119, 0x50, 0x4E, 0x47 };

    /* Magic bytes common for all JPG formats. Hex FF D8 FF. */
    private static final byte[] MAGIC_JPG = { -1, -40, -1  };

    private static final String MIME_PNG = "image/png";

    private static final String MIME_JPG = "image/jpg";

    private final ImageStorage storage;

    private final DataSource ds;

    /** Executor service for thumbnails. */
    private final ExecutorService resizeExecutor;


    @Inject
    public ImageService(ImageStorage imageStorage, DataSource ds, @Named("thumbnailExecutor") ExecutorService executor) {
        this.storage = imageStorage;
        this.ds = ds;
        this.resizeExecutor = executor;
    }

    public Image saveImage(byte[] data, int userId) {

        String mimeType = checkMimeType(data);
        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);

            int id;
            try (PreparedStatement ps = con.prepareStatement("insert into image (userId, mimeType) values (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, mimeType);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    id = rs.getInt(1);
                }
            }

            String ext = extensionFromMimeType(mimeType);
            String fileName = String.format("%d.%s", id, ext);

            storage.storeImage(fileName, data);

            con.commit();

            log.info("Saved image {}", fileName);

            return new Image(id, fileName, null);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to save file to file system", ioe);
        } catch (SQLException sqle) {
            throw new RuntimeException("Database error when saving file", sqle);
        }
    }

    public byte[] readImage(String name) {
        try {
            return storage.readImage(name).orElse(null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image");
        }
    }

    public void deleteImage(String name, int userId) {
        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);

            int id = Integer.parseInt(name.substring(0, name.indexOf('.')));

            try (PreparedStatement ps = con.prepareStatement("delete from image where id=? and userId=?")) {
                ps.setInt(1, id);
                ps.setInt(2, userId);
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    throw new RuntimeException("Failed to delete image with id " + id + " from database");
                }
            }

            storage.deleteImage(name);

            con.commit();
            log.info("Deleted image {}", name);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to delete image", ioe);
        } catch (SQLException sqle) {
            throw new RuntimeException("Database error when deleting image", sqle);
        }
    }

    public byte[] getResizedImage(int size, String name) {
        try {
            var existingResize = storage.readResizedImage(name, size);
            if (existingResize.isPresent()) {
                return existingResize.get();
            }

            var existingImage = storage.readImage(name);
            if (existingImage.isEmpty()) {
                return null;
            }

            String ext = name.substring(name.lastIndexOf('.') + 1);
            int originalSize = getImageMaxDimension(existingImage.get(), ext);
            if (originalSize >= size) {
                log.info("Added resize to queue size {} for {}", size, name);
                // Run resize in executor queue so that we don't use all available memory
                Future<ByteArrayOutputStream> imageResize = resizeExecutor.submit(() -> {
                    log.info("Creating resized image size {} for {}", size, name);
                    ByteArrayOutputStream out = new ByteArrayOutputStream(size * size);
                    Thumbnails.of(new ByteArrayInputStream(existingImage.get()))
                            .size(size, size)
                            .toOutputStream(out);
                    return out;
                });

                // Wait for completion
                try {
                    ByteArrayOutputStream result = imageResize.get();
                    byte[] data = result.toByteArray();
                    storage.storeResizedImage(name, size, data);
                    return data;
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Image resize failed", e);
                }
            } else {
                log.info("Image {} has size {}, using original for size {}", name, originalSize, size);
                storage.storeResizedImage(name, size, existingImage.get());
                return existingImage.get();
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to read resized image", e);
        }
    }

    static int getImageMaxDimension(byte[] data, String ext) throws IOException {
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(ext);
        if (iter.hasNext()) {
            ImageReader reader = iter.next();

            try (ImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))){
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                return Math.max(width, height);
            } finally {
                reader.dispose();
            }
        } else {
            throw new IllegalStateException("No image reader for extension " + ext);
        }
    }

    private static String checkMimeType(byte[] data) {
        if (arrayStartsWith(data, MAGIC_PNG)) {
            return MIME_PNG;
        } else if (arrayStartsWith(data, MAGIC_JPG)) {
            return MIME_JPG;
        }

        throw new IllegalArgumentException("Mime type not supported");
    }

    static String extensionFromMimeType(String mimeType) {
        return switch (mimeType) {
            case MIME_PNG -> "png";
            case MIME_JPG -> "jpg";
            default -> throw new IllegalArgumentException(String.format("Unknown mime type '%s'", mimeType));
        };
    }

    private static boolean arrayStartsWith(byte[] data, byte[] start) {
        for (int i = 0; i < start.length; i++) {
            if (data[i] != start[i]) {
                return false;
            }
        }

        return true;
    }

    public MediaType getMimeTypeFromName(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "png" -> new MediaType("image", "png");
            case "jpg" -> new MediaType("image", "jpg");
            default -> MediaType.APPLICATION_OCTET_STREAM_TYPE;
        };
    }

}
