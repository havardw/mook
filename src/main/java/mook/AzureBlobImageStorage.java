package mook;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AzureBlobImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobImageStorage.class);

    private final BlobServiceClient service;

    public AzureBlobImageStorage(String connectString) {
        service = new BlobServiceClientBuilder().connectionString(connectString).buildClient();
    }

    @Override
    public Optional<byte[]> readImage(String name) {
        var blob = getClient("original", name);
        if (blob.exists()) {
            return Optional.of(blob.downloadContent().toBytes());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void storeImage(String name, byte[] data) {
        var blob = getClient("original", name);
        blob.upload(BinaryData.fromBytes(data));
    }

    @Override
    public void deleteImage(String name) {
        PagedIterable<BlobContainerItem> containers = service.listBlobContainers();
        containers.forEach(item -> {
            var container = service.getBlobContainerClient(item.getName());
            var blob = container.getBlobClient(name);
            if (blob.deleteIfExists()) {
                log.info("Deleted {}/{}", item.getName(), name);
            }
        });
    }

    @Override
    public Optional<byte[]> readResizedImage(String name, int size) {
        var blob = getClient(Integer.toString(size), name);
        if (blob.exists()) {
            return Optional.of(blob.downloadContent().toBytes());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void storeResizedImage(String name, int size, byte[] data) {
        var blob = getClient(Integer.toString(size), name);
        blob.upload(BinaryData.fromBytes(data));
    }

    private BlobClient getClient(String containerName, String fileName) {
        return service.createBlobContainerIfNotExists(containerName).getBlobClient(fileName);
    }
}
