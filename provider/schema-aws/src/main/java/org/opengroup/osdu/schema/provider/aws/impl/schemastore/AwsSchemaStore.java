/* Copyright © 2020 Amazon Web Services

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package org.opengroup.osdu.schema.provider.aws.impl.schemastore;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.opengroup.osdu.core.aws.s3.IS3ClientFactory;
import org.opengroup.osdu.core.aws.s3.S3ClientWithBucket;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Repository
public class AwsSchemaStore implements ISchemaStore {
  private  static String serviceName = "AwsSchemaStore";
  @Inject
  private DpsHeaders headers;

  @Inject
  private JaxRsDpsLog logger;

  @Inject
  private IS3ClientFactory s3ClientFactory;

  @Value("${aws.s3.schemaBucket.ssm.relativePath}")
  private String s3SchemaBucketParameterRelativePath;

  @Value("${shared.tenant.name:common}")
  private String sharedTenant;

  private S3ClientWithBucket getS3ClientWithBucket() {
    String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();
    return getS3ClientWithBucket(dataPartitionId);
  } 

  private S3ClientWithBucket getS3ClientWithBucket( String dataPartitionId) {    
    return s3ClientFactory.getS3ClientForPartition(dataPartitionId, s3SchemaBucketParameterRelativePath);
  } 


  @Override
  public String createSchema(String filePath, String content) throws ApplicationException {

    S3ClientWithBucket s3ClientWithBucket = getS3ClientWithBucket();
    AmazonS3 s3 = s3ClientWithBucket.getS3Client();
    
    String path = resolvePath(headers.getPartitionIdWithFallbackToAccountId(), filePath);
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    int bytesSize = bytes.length;

    InputStream newStream = new ByteArrayInputStream(bytes);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(bytesSize);

    String bucket;
    try {
      bucket = s3ClientWithBucket.getBucketName();
      PutObjectRequest req = new PutObjectRequest(bucket, path, newStream, metadata);
      s3.putObject(req);

    } catch (Exception e) {
      throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
    }

    return s3.getUrl(bucket, path).toString();
  }

  @Override
  public String createSystemSchema(String filePath, String content) throws ApplicationException {
    updateDataPartitionId();
    return this.createSchema(filePath, content);
  }

  @Override
  public String getSchema(String dataPartitionId, String filePath) throws NotFoundException, ApplicationException {
    // first this method is called with the callers partitionid, then if not found, its called with the
    // common project id which is "common".  Not sure why this isn't passed into the createSchema call.

    S3ClientWithBucket s3ClientWithBucket = getS3ClientWithBucket(dataPartitionId);
    AmazonS3 s3 = s3ClientWithBucket.getS3Client();

    String content;
    String path = resolvePath(dataPartitionId, filePath);
    try {

      content = s3.getObjectAsString(s3ClientWithBucket.getBucketName(), path);
    } catch (AmazonS3Exception ex) {
      if (ex.getErrorCode().equals("NoSuchKey")) {  // or could be ex.getStatusCode == 404 (depends)
        logger.error(serviceName, String.format(SchemaConstants.SCHEMA_NOT_PRESENT, ex.getErrorMessage()));
        throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
      } else {
        logger.error(serviceName, String.format("Get Schema for %s failed: ", ex.getErrorMessage()));
        throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception ex) {
      logger.error(serviceName, String.format("Get Schema for %s failed: ", ex.toString()));
      throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
    }

    return content;

  }

  @Override
  public String getSystemSchema(String filePath) throws NotFoundException, ApplicationException {
    return this.getSchema(sharedTenant, filePath);
  }

  private String resolvePath(String dataPartitionId, String filePath) {
    return String.format("schema/%s/%s", dataPartitionId, filePath);
  }

  @Override
  public boolean cleanSchemaProject(String schemaId) throws ApplicationException {
    logger.info("Delete schema: " + schemaId);

    S3ClientWithBucket s3ClientWithBucket = getS3ClientWithBucket();
    AmazonS3 s3 = s3ClientWithBucket.getS3Client();

    try {
      s3.deleteObject(s3ClientWithBucket.getBucketName(), resolvePath(headers.getPartitionIdWithFallbackToAccountId(), schemaId));
      logger.info("Schema deleted: " + schemaId);
      return true;
    } catch (Exception  e) {
      logger.error("Failed to delete schema " +schemaId);
      return false;
    }
  }

  @Override
  public boolean cleanSystemSchemaProject(String schemaId) throws ApplicationException {
    this.updateDataPartitionId();
    return this.cleanSchemaProject(schemaId);
  }

  private void updateDataPartitionId() {
    headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
  }
}
