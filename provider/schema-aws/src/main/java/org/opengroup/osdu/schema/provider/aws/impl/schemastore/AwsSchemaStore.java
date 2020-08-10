package org.opengroup.osdu.schema.provider.aws.impl.schemastore;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.opengroup.osdu.core.aws.s3.S3Config;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Repository
public class AwsSchemaStore implements ISchemaStore {

  @Inject
  private DpsHeaders headers;

  @Inject
  private AwsServiceConfig serviceConfig;

  @Inject
  private JaxRsDpsLog logger;

  private AmazonS3 s3;

  @PostConstruct
  public void init() {
    s3 = new S3Config(serviceConfig.getS3Endpoint(),
            serviceConfig.getAmazonRegion()).amazonS3();
  }

  @Override
  public String createSchema(String filePath, String content) throws ApplicationException {
    // should this be headers.getPartitionIdWithFallbackToAccountId ??
    String path = resolvePath(headers.getPartitionId(), filePath);
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    int bytesSize = bytes.length;

    InputStream newStream = new ByteArrayInputStream(bytes);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(bytesSize);

    String bucket;
    try {
      bucket = serviceConfig.s3DataBucket;
      PutObjectRequest req = new PutObjectRequest(bucket, path, newStream, metadata);
      s3.putObject(req);

    } catch (Exception e) {
      throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
    }

    return s3.getUrl(bucket, path).toString();
  }

  @Override
  public String getSchema(String dataPartitionId, String filePath) throws NotFoundException, ApplicationException {
    // first this method is called with the callers partitionid, then if not found, its called with the
    // common project id which is "common".  Not sure why this isn't passed into the createSchema call.

    String content;
    String path = resolvePath(dataPartitionId, filePath);
    try {

      content = s3.getObjectAsString(serviceConfig.s3DataBucket, path);
    } catch (AmazonS3Exception ex) {
      if (ex.getErrorCode().equals("NoSuchKey")) {  // or could be ex.getStatusCode == 404 (depends)
        logger.error("AwsSchemaStore", String.format(SchemaConstants.SCHEMA_NOT_PRESENT, ex.getErrorMessage()));
        throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
      } else {
        logger.error("AwsSchemaStore", String.format("Get Schema for %s failed: ", ex.getErrorMessage()));
        throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception ex) {
      logger.error("AwsSchemaStore", String.format("Get Schema for %s failed: ", ex.toString()));
      throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
    }

    return content;

  }

  private String resolvePath(String dataPartitionId, String filePath) {
    return String.format("schema/%s/%s", dataPartitionId, filePath);
  }

  @Override
  public boolean cleanSchemaProject(String schemaId) throws ApplicationException {
    logger.info("Delete schema: " + schemaId);
    try {
      s3.deleteObject(serviceConfig.s3DataBucket, resolvePath(headers.getPartitionId(), schemaId));
      logger.info("Schema deleted: " + schemaId);
      return true;
    } catch (Exception  e) {
      logger.error("Failed to delete schema " +schemaId);
      return false;
    }
  }

}
