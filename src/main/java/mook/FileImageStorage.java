package mook;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public class FileImageStorage implements ImageStorage {
    private static final Logger log = LoggerFactory.getLogger(FileImageStorage.class);

    private final String basePath;


    public FileImageStorage(@ConfigProperty(name = "mook.image.path") String basePath) {
        this.basePath = basePath;
    }

    @Override
    public Optional<byte[]> readImage(String name) throws IOException {
        Path image = Paths.get(basePath,  "original", name);
        return getBytes(image);
    }

    @Override
    public void storeImage(String name, byte[] data) throws IOException {
        Files.write(prepareDir("original").resolve(name), data, StandardOpenOption.CREATE_NEW);
    }

    @Override
    public void deleteImage(String name) throws IOException {
        Path image = Paths.get(basePath, "original", name);
        Files.delete(image);
        log.info("Deleted original image {}", image);

        try (var paths = Files.find(Paths.get(basePath), 2, (path, basicFileAttributes) -> path.endsWith(name))) {
            paths.forEach(p -> {
                try {
                    Files.delete(p);
                    log.info("Deleted resized image " + p.subpath(p.getParent().getNameCount() - 2, p.getParent().getNameCount()));
                } catch (IOException e) {
                    log.warn("Failed to delete resized image " + p);
                }
            });
        }
    }

    @Override
    public Optional<byte[]> readResizedImage(String name, int size) throws IOException {
        Path resized = Paths.get(basePath, Integer.toString(size), name);
        return getBytes(resized);
    }

    @Override
    public void storeResizedImage(String name, int size, byte[] data) throws IOException {
        Files.write(prepareDir(Integer.toString(size)).resolve(name), data, StandardOpenOption.CREATE_NEW);
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

    private Optional<byte[]> getBytes(Path image) throws IOException {
        if (Files.exists(image)) {
            return Optional.of(Files.readAllBytes(image));
        } else {
            return Optional.empty();
        }
    }
}
