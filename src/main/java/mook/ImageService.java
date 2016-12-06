package mook;

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

/**
 * Data service for images.
 */
public class ImageService {

    private static final byte[] MAGIC_PNG = { -119, 0x50, 0x4E, 0x47 };

    /* Magic bytes common for all JPG formats. Hex FF D8 FF. */
    private static final byte[] MAGIC_JPG = { -1, -40, -1  };

    private static final String MIME_PNG = "image/png";

    private static final String MIME_JPG = "image/jpg";

    /** Base path for saving images. */
    private final String basePath;

    private final DataSource ds;


    @Inject
    public ImageService(@Named("imagePath") String basePath, DataSource ds) {
        this.basePath = basePath;
        this.ds = ds;
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

            return new Image(id, fileName, null);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to save file to file system", ioe);
        } catch (SQLException sqle) {
            throw new RuntimeException("Database error when saving file", sqle);
        }
    }

    public byte[] readImage(String name) {
        Path image = Paths.get(basePath,  "original", name);
        if (image.toFile().exists()) {
            try {
                return Files.readAllBytes(image);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read image file", e);
            }
        } else {
            return null;
        }
    }

    private Path prepareDir(String... path) throws IOException {
        Path target = Paths.get(basePath, path);
        if (Files.exists(target)) {
            if (Files.isWritable(target)) {
                return target;
            } else {
                throw new IllegalStateException(String.format("Directory '%s' is not writable", target.toString()));
            }
        } else {
            Files.createDirectory(target);
            return target;
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
