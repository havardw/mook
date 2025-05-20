package mook;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class BeanProducer {

    private static final Logger log = LoggerFactory.getLogger(BeanProducer.class);

    @ConfigProperty(name = "mook.image.threads", defaultValue = "2")
    int thumbnailThreads;


    @Singleton
    @Named("thumbnailExecutor")
    public ExecutorService thumbnailExecutor() {
        return Executors.newFixedThreadPool(thumbnailThreads);
    }

    @Singleton
    public ImageStorage imageStorage(Config config) {
        String storageType = config.getOptionalValue("mook.storage.type", String.class).orElse("file");
        switch (storageType.toLowerCase()) {
            case "azure":
                Optional<String> azureConnectString = config.getOptionalValue("azure.blob.connect", String.class);
                if (azureConnectString.isEmpty()) {
                    throw new IllegalStateException("Azure blob storage selected but 'azure.blob.connect' is not configured");
                }
                log.info("Using Azure blob storage for images");
                return new AzureBlobImageStorage(azureConnectString.get());

            case "s3":
                Optional<String> accessKey = config.getOptionalValue("mook.s3.access-key", String.class);
                Optional<String> secretKey = config.getOptionalValue("mook.s3.secret-key", String.class);
                Optional<String> host = config.getOptionalValue("mook.s3.host", String.class);
                Optional<String> bucket = config.getOptionalValue("mook.s3.bucket", String.class);
                Optional<String> region = config.getOptionalValue("mook.s3.region", String.class);

                if (accessKey.isEmpty() || secretKey.isEmpty() || bucket.isEmpty() || region.isEmpty()) {
                    throw new IllegalStateException("S3 storage selected but one or more required properties are missing: " +
                            "'mook.s3.access-key', 'mook.s3.secret-key', 'mook.s3.bucket', 'mook.s3.region'");
                }

                log.info("Using S3 blob storage for images in bucket: {}, region: {}",
                        bucket.get(), region.get());
                return new S3BlobImageStorage(accessKey.get(), secretKey.get(), region.get(), bucket.get(), host);

            case "file":
            default:
                Optional<String> imagePath = config.getOptionalValue("mook.image.path", String.class);
                if (imagePath.isEmpty()) {
                    throw new IllegalStateException("File storage selected but 'mook.image.path' is not configured");
                }
                log.info("Using file storage for images in {}", imagePath.get());
                return new FileImageStorage(imagePath.get());
        }
    }
}