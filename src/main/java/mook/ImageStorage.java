package mook;

import java.io.IOException;
import java.util.Optional;

/**
 * Backed-independent image storage.
 */
public interface ImageStorage {

    /**
     * Read an image at original size.
     *
     * @param name Image name, with extension
     * @return Image bytes, or empty if not found
     * @throws IOException If reading image fails
     */
    Optional<byte[]> readImage(String name) throws IOException;

    /**
     * Store a new original image.
     *
     * @param name Image name, with extension
     * @param data Image data
     * @throws IOException If reading storing fails
     */
    void storeImage(String name, byte[] data) throws IOException;

    /**
     * Delete an original image and all resized versions.
     *
     * @param name Image name, with extension
     * @throws IOException If IO for deletion fails
     */
    void deleteImage(String name) throws IOException;

    /**
     * Read an image at the given size.
     *
     * @param name Image name, with extension
     * @param size Max size (width or height) for resized image
     * @return Image bytes, or empty if not found
     * @throws IOException If reading image fails
     */
    Optional<byte[]> readResizedImage(String name, int size) throws IOException;

    /**
     * Store a resized image.
     *
     * @param name Image name, with extension
     * @param size Size for resized image (max for width or height)
     * @param data Image data
     * @throws IOException If reading storing fails
     */
    void storeResizedImage(String name, int size, byte[] data) throws IOException;

}
