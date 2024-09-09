package org.redpill.alfresco.s3;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import org.alfresco.repo.content.AbstractContentWriter;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.util.GUID;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import javax.servlet.http.HttpServletRequest;

/**
 * MinIO content writer
 *
 * @author TuNK, updated for MinIO
 */
public class S3ContentWriter extends AbstractContentWriter {

  private static final Log LOG = LogFactory.getLog(S3ContentWriter.class);

  private final MinioClient s3Connector;
  private final String key;
  private final String bucketName;
  private File tempFile;
  private long size;
  private String uuid;

  public S3ContentWriter(String bucketName, String key, String contentUrl, ContentReader existingContentReader, MinioClient s3Connector) {
    super(contentUrl, existingContentReader);
    this.key = key;
    this.s3Connector = s3Connector;
    this.bucketName = bucketName;
    this.uuid=GUID.generate();
    LOG.info("S3ContentWriter initialized with bucket: " + bucketName + ", key: " + key);
    addListener(new S3WriteStreamListener(this));
  }

  @Override
  protected ContentReader createReader() throws ContentIOException {
    return new S3ContentReader(key, getContentUrl(), s3Connector, bucketName);
  }

// @Override
// public WritableByteChannel getDirectWritableChannel() throws ContentIOException {
//   LOG.info("GetDirectWritableChannel =================> START ");
//   try {
//     String uuid = GUID.generate();
//     if (LOG.isDebugEnabled()) {
//       LOG.debug("MinioContentWriter Creating Temp File: uuid=" + uuid);
//     }
//     tempFile = TempFileProvider.createTempFile(uuid, ".bin");
//     OutputStream os = Files.newOutputStream(tempFile.toPath());
//     LOG.debug("Return channel to temp file: " + tempFile.getAbsolutePath());
//     if (LOG.isDebugEnabled()) {
//       LOG.debug("MinioContentWriter Returning Channel to Temp File: uuid=" + uuid);
//     }
//     WritableByteChannel channel = Channels.newChannel(os);
//
//     // Đảm bảo uploadToMinio được gọi sau khi ghi dữ liệu
//     CompletableFuture<Void> uploadFuture = uploadToMinio();
//
//     // Đảm bảo rằng việc upload hoàn tất trước khi kết thúc
//     uploadFuture.join();
//
//     return channel;
//   } catch (Throwable e) {
//     throw new ContentIOException("MinioContentWriter.getDirectWritableChannel(): Failed to open channel. " + this, e);
//   }
// }

//  @Override
//  public WritableByteChannel getDirectWritableChannel() throws ContentIOException {
//    LOG.info("GetDirectWritableChannel =================> START ");
//    try {
//      // Sử dụng ByteArrayOutputStream để giữ dữ liệu tạm thời
//      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//      WritableByteChannel channel = Channels.newChannel(byteArrayOutputStream);
//
//      // Đảm bảo uploadToMinio được gọi sau khi ghi dữ liệu
//      CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
//        try {
//          // Upload dữ liệu từ ByteArrayOutputStream lên MinIO
//          String uuid = GUID.generate();
//          String key = "alfresco-content/" + uuid + ".bin";
//          MinioClient minioClient = getS3Connector();
//          minioClient.putObject(
//                  PutObjectArgs.builder()
//                          .bucket(getBucketName())
//                          .object(key)
//                          .stream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), byteArrayOutputStream.size(), -1)
//                          .build()
//          );
//          LOG.info("Upload completed for bucket " + getBucketName() + " with key " + key);
//        } catch (Exception e) {
//          LOG.error("Failed to upload data to MinIO: bucket=" + getBucketName(), e);
//        }
//      });
//
//      // Đảm bảo rằng việc upload hoàn tất trước khi kết thúc
//      uploadFuture.join();
//
//      return channel;
//    } catch (Throwable e) {
//      throw new ContentIOException("MinioContentWriter.getDirectWritableChannel(): Failed to open channel. " + this, e);
//    }
//  }

//   @Override
//   public WritableByteChannel getDirectWritableChannel() throws ContentIOException {
//     LOG.info("GetDirectWritableChannel =================> START ");
//     try {
//       // Thay vì tạo file tạm thời, sử dụng ByteArrayOutputStream để ghi dữ liệu
//       ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//       WritableByteChannel channel = Channels.newChannel(outputStream);
//
//       // Upload dữ liệu lên MinIO sau khi hoàn tất việc ghi
//       CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
//         try {
//           // Upload dữ liệu trực tiếp từ ByteArrayOutputStream lên MinIO
//           byte[] data = outputStream.toByteArray();
//           InputStream inputStream = new ByteArrayInputStream(data);
//
//           MinioClient minioClient = getS3Connector();
//           minioClient.putObject(
//                   PutObjectArgs.builder()
//                           .bucket(getBucketName())
//                           .object(getKey())
//                           .stream(inputStream, data.length, -1)  // Upload trực tiếp từ stream
//                           .build()
//           );
//           LOG.info("Upload completed for bucket " + getBucketName() + " with key " + getKey());
//         } catch (Exception e) {
//           LOG.error("Failed to upload data to MinIO: bucket=" + getBucketName(), e);
//         }
//       });
//
//       // Đảm bảo rằng upload hoàn tất trước khi kết thúc phương thức
//       uploadFuture.join();
//
//       return channel;
//     } catch (Throwable e) {
//       throw new ContentIOException("MinioContentWriter.getDirectWritableChannel(): Failed to open channel. " + this, e);
//     }
//   }

  @Override
  public WritableByteChannel getDirectWritableChannel() throws ContentIOException {
    LOG.info("GetDirectWritableChannel =================> START ");
    try {
      // Thay vì tạo file tạm thời, sử dụng ByteArrayOutputStream để ghi dữ liệu
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      WritableByteChannel channel = Channels.newChannel(outputStream);

      // Upload dữ liệu lên MinIO sau khi hoàn tất việc ghi
      CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
        try {
          // Upload dữ liệu trực tiếp từ ByteArrayOutputStream lên MinIO
          byte[] data = outputStream.toByteArray();
          InputStream inputStream = new ByteArrayInputStream(data);

          MinioClient minioClient = getS3Connector();
          minioClient.putObject(
                  PutObjectArgs.builder()
                          .bucket(getBucketName())
                          .object(getKey())
                          .stream(inputStream, data.length, -1)  // Upload trực tiếp từ stream
                          .build()
          );
          LOG.info("Upload completed for bucket " + getBucketName() + " with key " + getKey());

          // Sau khi upload, thực hiện xử lý file tạm nếu có
          if (tempFile != null) {
            uploadToMinio().join(); // Gọi phương thức upload file tạm
          }

        } catch (Exception e) {
          LOG.error("Failed to upload data to MinIO: bucket=" + getBucketName(), e);
        }
      });

      // Đảm bảo rằng upload hoàn tất trước khi kết thúc phương thức
      uploadFuture.join();

      return channel;
    } catch (Throwable e) {
      throw new ContentIOException("MinioContentWriter.getDirectWritableChannel(): Failed to open channel. " + this, e);
    }
  }

  /**
   * Upload the temp file to Minio once writing is complete.
   */
  public CompletableFuture<Void> uploadToMinio() {
    return CompletableFuture.runAsync(() -> {
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Uploading file to Minio: bucket=" + bucketName + ", key=" + key);
        }

        s3Connector.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(key)
                        .filename(tempFile.getAbsolutePath())
                        .build()
        );
        LOG.trace("Upload to Minio completed: key=" + key + ", bucketName=" + bucketName + ", filename=" + tempFile.getAbsolutePath());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Upload to Minio completed: key=" + key);
        }

        // Xóa file tạm sau khi upload thành công
        try {
          Files.deleteIfExists(tempFile.toPath());
          if (LOG.isDebugEnabled()) {
            LOG.debug("Temporary file deleted: " + tempFile.getAbsolutePath());
          }
        } catch (Exception e) {
          LOG.error("Failed to delete temporary file: " + tempFile.getAbsolutePath(), e);
        }

      } catch (MinioException e) {
        LOG.error("MinioException: Failed to upload file to Minio: " + key, e);
        throw new ContentIOException("Failed to upload file to Minio: " + key, e);
      } catch (Exception e) {
        LOG.error("Exception: Failed to upload file to Minio: " + key, e);
        throw new ContentIOException("Failed to upload file to Minio: " + key, e);
      }
    });
  }

  @Override
  public long getSize() {
    return this.size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getBucketName() {
    return bucketName;
  }

  public String getKey() {
    return key;
  }

  public File getTempFile() {
    return tempFile;
  }

  public MinioClient getS3Connector() {
    return s3Connector;
  }

  public void setTempFile(File tempFile) {
    this.tempFile = tempFile;
  }
}
