package org.redpill.alfresco.s3;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.messages.Bucket;
import org.alfresco.repo.content.*;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.util.GUID;
import org.alfresco.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Map;


import org.alfresco.repo.content.AbstractContentStore;
import org.alfresco.repo.content.ContentStore;

import javax.servlet.http.HttpServletRequest;

/**
 * A MinIO content store replacing the previous S3 content store.
 */
public class S3ContentStore extends AbstractContentStore
        implements ApplicationContextAware, ApplicationListener<ApplicationEvent>, InitializingBean {

  private static final Log LOG = LogFactory.getLog(S3ContentStore.class);
  private ApplicationContext applicationContext;

  private MinioClient minioClient;

  private String accessKey;
  private String secretKey;
  private String bucketName;
  private String endpoint;
  private String regionName; // Unused, but kept for compatibility
  private String rootDirectory;

  @Override
  public boolean isWriteSupported() {
    return true;
  }

  @Override
  public ContentReader getReader(String contentUrl) {
    LOG.info("GetReader ================> START");
    LOG.info("GetReader ================> " + contentUrl);
    String key = makeS3Key(contentUrl);
    return new S3ContentReader(key, contentUrl, minioClient, bucketName);
  }

  /**
   * Initialize the content store with MinIO client.
   */
  public void init() {
    try {
      minioClient = MinioClient.builder()
              .endpoint(endpoint)
              .credentials(accessKey, secretKey)
              .build();
      LOG.info("MinIO client successfully initialized with endpoint: " + endpoint);
      for (Bucket bucket : minioClient.listBuckets()) {
        LOG.info("MinIO client bucket: " + bucket.name());
      }
    } catch (Exception e) {
      LOG.error("Error initializing MinIO client", e);
    }
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setRootDirectory(String rootDirectory) {
    String dir = rootDirectory;
    if (dir.startsWith("/")) {
      dir = dir.substring(1);
    }
    this.rootDirectory = dir;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  protected ContentWriter getWriterInternal(ContentReader existingContentReader, String newContentUrl) {
    LOG.info("GetWriterInternal =================> START ");
    String contentUrl = newContentUrl;
    if (StringUtils.isBlank(contentUrl)) {
      contentUrl = createNewUrl();
    }
    String key = makeS3Key(contentUrl);
    LOG.info("S3ContentWriter object from MinIO with url: " + contentUrl + ", key: " + key + ", bucketName: " + bucketName);
    return new S3ContentWriter(bucketName, key, contentUrl, existingContentReader, minioClient);
  }

  public static String createNewUrl() {
    Calendar calendar = new GregorianCalendar();
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH) + 1;  // 0-based
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    int hour = calendar.get(Calendar.HOUR_OF_DAY);
    int minute = calendar.get(Calendar.MINUTE);

    StringBuilder sb = new StringBuilder(20);
    sb.append(FileContentStore.STORE_PROTOCOL)
            .append(ContentStore.PROTOCOL_DELIMITER)
            .append(year).append('/')
            .append(month).append('/')
            .append(day).append('/')
            .append(hour).append('/')
            .append(minute).append('/')
            .append(GUID.generate()).append(".bin");

    return sb.toString();
  }

  private String makeS3Key(String contentUrl) {
    Pair<String, String> urlParts = super.getContentUrlParts(contentUrl);
    String protocol = urlParts.getFirst();
    String relativePath = urlParts.getSecond();
    if (!protocol.equals(FileContentStore.STORE_PROTOCOL)) {
      throw new UnsupportedContentUrlException(this, protocol + PROTOCOL_DELIMITER + relativePath);
    }
    return rootDirectory + "/" + relativePath;
  }

  @Override
  public boolean delete(String contentUrl) {
    try {
      String key = makeS3Key(contentUrl);
      // Sử dụng RemoveObjectArgs để xóa đối tượng từ MinIO
      minioClient.removeObject(
              RemoveObjectArgs.builder()
                      .bucket(bucketName)
                      .object(key)
                      .build()
      );
      if (LOG.isTraceEnabled()) {
        LOG.trace("Deleted object from MinIO with url: " + contentUrl + ", key: " + key);
      }
      return true;
    } catch (Exception e) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Error deleting MinIO Object", e);
      }
    }
    return false;
  }

  private void publishEvent(ApplicationContext context, Map<String, Serializable> extendedEventParams) {
    context.publishEvent(new ContentStoreCreatedEvent(this, extendedEventParams));
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ContextRefreshedEvent && event.getSource() == this.applicationContext) {
      publishEvent(((ContextRefreshedEvent) event).getApplicationContext(), Collections.<String, Serializable>emptyMap());
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(accessKey, "Access key must not be null");
    Assert.notNull(secretKey, "Secret key must not be null");
    Assert.notNull(bucketName, "Bucket name must not be null");
    Assert.notNull(endpoint, "Endpoint must not be null");
    Assert.notNull(rootDirectory, "Root directory must not be null");
  }
}
