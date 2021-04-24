package mook;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Data service for images.
 */
@Slf4j
public class ImageService {

    private static final byte[] MAGIC_PNG = { -119, 0x50, 0x4E, 0x47 };

    /* Magic bytes common for all JPG formats. Hex FF D8 FF. */
    private static final byte[] MAGIC_JPG = { -1, -40, -1  };

    private static final String MIME_PNG = "image/png";

    private static final String MIME_JPG = "image/jpg";

    /** Base path for saving images. */
    private final String basePath;

    private final DataSource ds;

    /** Executor service for thumbnails. */
    private final ExecutorService resizeExecutor;


    @Inject
    public ImageService(@Named("imagePath") String basePath, DataSource ds, @Named("thumbnailExecutor") ExecutorService executor) {
        this.basePath = basePath;
        this.ds = ds;
        this.resizeExecutor = executor;
    }

    public Image saveImage(byte[] data, int userId) throws IOException {

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

            Files.write(prepareDir("original").resolve(fileName), data, StandardOpenOption.CREATE_NEW);

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
        Path image = Paths.get(basePath,  "original", name);
        if (Files.exists(image)) {
            try {
                return Files.readAllBytes(image);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read image file", e);
            }
        } else {
            return null;
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

            Path image = Paths.get(basePath, "original", name);
            Files.delete(image);
            log.info("Deleted original image {}", image);

            Files.find(Paths.get(basePath), 2, (path, basicFileAttributes) -> path.endsWith(name))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            log.info("Deleted resized image " + p.subpath(p.getParent().getNameCount() - 2, p.getParent().getNameCount()));
                        } catch (IOException e) {
                            log.warn("Failed to delete resized image " + p);
                        }
                    });

        } catch (IOException ioe) {
            throw new RuntimeException("Failed to delete file from file system", ioe);
        } catch (SQLException sqle) {
            throw new RuntimeException("Database error when deleting file", sqle);
        }
    }

    public byte[] getResizedImage(int size, String name) {
        Path original = Paths.get(basePath,  "original", name);
        if (!Files.exists(original)) {
            return null;
        }

        // Create resized image if it doesn't exist
        Path resized = Paths.get(basePath, Integer.toString(size), name);
        if (!Files.exists(resized)) {
            try {
                prepareDir(Integer.toString(size));

                int originalSize = getImageMaxDimension(name);
                if (originalSize >= size) {
                    log.info("Added resize to queue size {} for {}", size, name);
                    // Run resize in executor queue so that we don't use all available memory
                    Future<Void> imageResize = resizeExecutor.submit(() -> {
                        log.info("Creating resized image size {} for {}", size, name);
                        Thumbnails.of(original.toFile()).size(size, size).asFiles(resized.getParent().toFile(), Rename.NO_CHANGE);
                        return null;
                    });

                    // Wait for completion
                    imageResize.get();
                } else {
                    log.info("Image {} has size {}, using original for size {}", name, originalSize, size);
                    Files.copy(original, resized);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to resize image", e);
            }
        }

        try {
            return Files.readAllBytes(resized);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resized image file", e);
        }
    }

    private int getImageMaxDimension(String name) throws IOException {
        String ext = name.substring(name.lastIndexOf('.') + 1);

        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(ext);
        if (iter.hasNext()) {
            ImageReader reader = iter.next();

            try (ImageInputStream stream = new FileImageInputStream(Paths.get(basePath, "original", name).toFile())){
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                return Math.max(width, height);
            } finally {
                reader.dispose();
            }
        } else {
            throw new IllegalStateException("No image reader for file " + name);
        }
    }

    private Path prepareDir(String... path) throws IOException {
        Path target = Paths.get(basePath, path);
        if (Files.exists(target)) {
            if (Files.isWritable(target)) {
                return target;
            } else {
                throw new IllegalStateException(String.format("Directory '%s' is not writable", target));
            }
        } else {
            return Files.createDirectories(target);
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
        switch (mimeType) {
            case MIME_PNG: return "png";
            case MIME_JPG: return "jpg";
            default: throw new IllegalArgumentException(String.format("Unknown mime type '%s'", mimeType));
        }
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
        switch (ext) {
            case "png": return new MediaType("image", "png");
            case "jpg": return new MediaType("image", "jpg");
            default: return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
    }

}
