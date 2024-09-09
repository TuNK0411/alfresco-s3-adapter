//package org.redpill.alfresco.s3;
//
//import io.minio.MinioClient;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import java.util.List;
//
///**
// * S3 Connector
// *
// * @author TuNK
// */
//public class S3Connector {
//    private static final Log LOG = LogFactory.getLog(S3Connector.class);
//
//    private MinioClient minioClient;
//
//    public S3Connector(List<String> endpoints, String accessKey, String secretKey) {
//        try {
//            MinioClient.Builder builder = MinioClient.builder();
//
//            for (String endpoint : endpoints) {
//                builder.endpoint(endpoint);
//            }
//
//            builder.credentials(accessKey, secretKey);
//            minioClient = builder.build();
//        } catch (Exception ex) {
//            LOG.error("Connect S3 Store Error!", ex);
//        }
//    }
//
//    public MinioClient getMinioClient() {
//        return minioClient;
//    }
//}
