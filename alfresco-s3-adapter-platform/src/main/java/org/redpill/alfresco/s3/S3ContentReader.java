package org.redpill.alfresco.s3;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.alfresco.repo.content.AbstractContentReader;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * MinIO Content Reader
 */
public class S3ContentReader extends AbstractContentReader implements AutoCloseable {

    private static final Log LOG = LogFactory.getLog(S3ContentReader.class);

    private final String key;
    private final MinioClient minioClient;
    private final String bucket;
    private StatObjectResponse s3ObjectMetadata;

    /**
     * @param key        the key to use when looking up data
     * @param minioClient   the MinIO client to use for the connection
     * @param contentUrl the content URL - this should be relative to the root of
     *                   the store
     * @param bucket     the MinIO bucket name
     */
    protected S3ContentReader(String key, String contentUrl, MinioClient minioClient, String bucket) {
        super(contentUrl);
        this.key = key;
        this.minioClient = minioClient;
        this.bucket = bucket;
        LOG.trace("S3ContentReader initialized with key: " + key + "bucketName:" + bucket);
    }

    /**
     * Close file object
     *
     * @throws IOException Throws exception on error
     */
    protected void closeFileObject() throws IOException {
        // MinIO objects are closed automatically after being read, so nothing is needed here
    }

    /**
     * Lazy initialize the file metadata
     */
    protected void lazyInitFileMetadata() {
        if (s3ObjectMetadata == null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Lazy init for file metadata for " + bucket + " - " + key);
            }
            try {
                s3ObjectMetadata = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucket)
                                .object(key)
                                .build()
                );
            } catch (Exception e) {
                LOG.error("Unable to fetch metadata for MinIO object", e);
            }
        }
    }

    @Override
    protected ContentReader createReader() throws ContentIOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Called createReader for contentUrl -> " + getContentUrl() + ", Key: " + key);
        }
        return new S3ContentReader(key, getContentUrl(), minioClient, bucket);
    }

    @Override
    protected ReadableByteChannel getDirectReadableChannel() throws ContentIOException {
        try {
            lazyInitFileMetadata();
            // Fetch the object stream from MinIO
            return Channels.newChannel(minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            ));
        } catch (Exception e) {
            throw new ContentIOException("Unable to retrieve content object from MinIO", e);
        }
    }

    @Override
    public boolean exists() {
        try {
            lazyInitFileMetadata();
        } catch (Exception e) {
            LOG.trace("Could not fetch metadata of object. It is probably removed.", e);
            return false;
        }
        return s3ObjectMetadata != null;
    }

    @Override
    public long getLastModified() {
        try {
            lazyInitFileMetadata();
        } catch (Exception e) {
            LOG.trace("Could not fetch metadata of object. It is probably removed.", e);
            return 0L;
        }
        if (!exists()) {
            return 0L;
        }
        return s3ObjectMetadata.lastModified().toInstant().toEpochMilli();
    }

    @Override
    public long getSize() {
        try {
            lazyInitFileMetadata();
        } catch (Exception e) {
            LOG.trace("Could not fetch metadata of object. It is probably removed.", e);
            return 0L;
        }
        if (!exists()) {
            return 0L;
        }
        return s3ObjectMetadata.size();
    }

    @Override
    public void close() throws Exception {
        closeFileObject();
    }
}
