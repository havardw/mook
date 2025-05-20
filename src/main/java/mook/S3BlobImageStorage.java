package mook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Optional;

public class S3BlobImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(S3BlobImageStorage.class);

    private final S3Client s3Client;
    private final String bucketName;

    public S3BlobImageStorage(String accessKey, String secretKey, String region, String bucketName, Optional<String> host) {

        this.bucketName = bucketName;

        // Initialize S3 client with credentials and host
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));
        host.ifPresent(s -> builder.endpointOverride(URI.create(s)));

        this.s3Client = builder.build();

        // Log initialization
        log.info("Initialized S3BlobImageStorage with bucket: {}, host: {}", bucketName, host);
    }

    @Override
    public Optional<byte[]> readImage(String name) throws IOException {
        String key = "original/" + name;
        return getObject(key);
    }

    @Override
    public void storeImage(String name, byte[] data) throws IOException {
        String key = "original/" + name;
        putObject(key, data);
    }

    @Override
    public void deleteImage(String name) throws IOException {
        String originalKey = "original/" + name;
        deleteObject(originalKey);
    }

    @Override
    public Optional<byte[]> readResizedImage(String name, int size) throws IOException {
        String key = size + "/" + name;
        return getObject(key);
    }

    @Override
    public void storeResizedImage(String name, int size, byte[] data) throws IOException {
        String key = size + "/" + name;
        putObject(key, data);
    }

    private Optional<byte[]> getObject(String key) throws IOException {
        try {
            // Create a GetObjectRequest
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Try to get the object
            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest)) {
                // Read the object content into a byte array
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = response.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return Optional.of(baos.toByteArray());
            }
        } catch (NoSuchKeyException e) {
            // Object doesn't exist
            return Optional.empty();
        } catch (S3Exception e) {
            throw new IOException("Failed to get object from S3: " + e.getMessage(), e);
        }
    }

    private void putObject(String key, byte[] data) throws IOException {
        try {
            // Determine content type based on file extension
            String contentType = "application/octet-stream";
            if (key.endsWith(".jpg") || key.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (key.endsWith(".png")) {
                contentType = "image/png";
            } else if (key.endsWith(".gif")) {
                contentType = "image/gif";
            }

            // Create a PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            // Upload the object
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
            log.info("Successfully uploaded {} to S3", key);
        } catch (S3Exception e) {
            throw new IOException("Failed to put object to S3: " + e.getMessage(), e);
        }
    }

    private void deleteObject(String key) throws IOException {
        try {
            // Create a DeleteObjectRequest
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Delete the object
            s3Client.deleteObject(deleteObjectRequest);
            log.info("Deleted {}", key);
        } catch (S3Exception e) {
            throw new IOException("Failed to delete object from S3: " + e.getMessage(), e);
        }
    }
}