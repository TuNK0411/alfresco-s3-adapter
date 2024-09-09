package org.redpill.alfresco.s3;

import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * Stream listener which is used to copy the temp file contents into MinIO
 *
 * @author TuNK
 */
public class S3WriteStreamListener implements ContentStreamListener {

  private static final Log LOG = LogFactory.getLog(S3WriteStreamListener.class);

  private final S3ContentWriter writer;

  public S3WriteStreamListener(S3ContentWriter writer) {
    this.writer = writer;
  }

  @Override
  public void contentStreamClosed() throws ContentIOException {
//    File file = writer.getTempFile();
//    if (file == null || !file.exists()) {
//      throw new ContentIOException("Temporary file does not exist or is not set.");
//    }
//
//    long size = file.length();
//    writer.setSize(size);
//
//    if (LOG.isDebugEnabled()) {
//      LOG.debug("Preparing to upload file to MinIO: bucket=" + writer.getBucketName() + ", key=" + writer.getKey());
//    }
//
//    // Get the MinioClient from the writer
//    MinioClient minioClient = writer.getS3Connector();
//
//    try {
//      if (LOG.isTraceEnabled()) {
//        LOG.trace("Uploading file to MinIO: bucket=" + writer.getBucketName() + ", key=" + writer.getKey());
//      }
//
//      // Use Minio SDK to upload the file
//      minioClient.uploadObject(
//              UploadObjectArgs.builder()
//                      .bucket(writer.getBucketName())
//                      .object(writer.getKey())
//                      .filename(file.getAbsolutePath())
//                      .build()
//      );
//
//      if (LOG.isTraceEnabled()) {
//        LOG.trace("Upload completed for bucket " + writer.getBucketName() + " with key " + writer.getKey());
//      }
//    } catch (Exception e) {
//      LOG.error("Failed to upload file to MinIO: bucket=" + writer.getBucketName() + ", key=" + writer.getKey(), e);
//      throw new ContentIOException("S3WriteStreamListener Failed to Upload File for bucket "
//              + writer.getBucketName() + " with key " + writer.getKey(), e);
//    } finally {
//      // Remove the temp file
//      if (file != null && file.exists()) {
//        boolean deleted = file.delete();
//        if (!deleted && LOG.isWarnEnabled()) {
//          LOG.warn("Failed to delete temp file: " + file.getAbsolutePath());
//        }
//      }
//    }
  }
}
