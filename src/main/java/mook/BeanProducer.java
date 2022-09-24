package mook;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.inject.Singleton;
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
        Optional<String> azureConnectString = config.getOptionalValue("azure.blob.connect", String.class);
        if (azureConnectString.isPresent()) {
            log.info("Using Azure blob storage for images");
            return new AzureBlobImageStorage(azureConnectString.get());
        } else {
            Optional<String> imagePath = config.getOptionalValue("mook.image.path", String.class);
            if (imagePath.isEmpty()) {
                throw new IllegalStateException("Image storage is not configured, must set either 'azure.blob.connect' or 'mook.image.path'");
            }
            log.info("Using file storage for images in {}", imagePath.get());
            return new FileImageStorage(imagePath.get());
        }
    }
}