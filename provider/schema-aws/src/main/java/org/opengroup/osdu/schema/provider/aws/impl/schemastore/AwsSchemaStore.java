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

import org.opengroup.osdu.core.aws.v2.s3.IS3ClientFactory;
import org.opengroup.osdu.core.aws.v2.s3.S3ClientWithBucket;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.nio.charset.StandardCharsets;

@Repository
public class AwsSchemaStore implements ISchemaStore {
  private static final String SERVICE_NAME = "AwsSchemaStore";

  private final DpsHeaders headers;
  private final JaxRsDpsLog logger;
  private final IS3ClientFactory s3ClientFactory;
  private final String s3SchemaBucketParameterRelativePath;
  private final String sharedTenant;

  public AwsSchemaStore(
      DpsHeaders headers,
      JaxRsDpsLog logger,
      IS3ClientFactory s3ClientFactory,
      @Value("${aws.s3.schemaBucket.ssm.relativePath}") String s3SchemaBucketParameterRelativePath,
      @Value("${shared.tenant.name:common}") String sharedTenant) {
    this.headers = headers;
    this.logger = logger;
    this.s3ClientFactory = s3ClientFactory;
    this.s3SchemaBucketParameterRelativePath = s3SchemaBucketParameterRelativePath;
    this.sharedTenant = sharedTenant;
  }

  private S3ClientWithBucket getS3ClientWithBucket() {
    String dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();
    return getS3ClientWithBucket(dataPartitionId);
  }

  private S3ClientWithBucket getS3ClientWithBucket(String dataPartitionId) {
    return s3ClientFactory.getS3ClientForPartition(dataPartitionId, s3SchemaBucketParameterRelativePath);
  }

  @Override
  public String createSchema(String filePath, String content) throws ApplicationException {
    S3ClientWithBucket s3ClientWithBucket = getS3ClientWithBucket();
    S3Client s3 = s3ClientWithBucket.getS3Client();

    String path = resolvePath(headers.getPartitionIdWithFallbackToAccountId(), filePath);
    String bucket = s3ClientWithBucket.getBucketName();

    try {
      // Store the content exactly as received without any processing
      // This ensures compatibility with the existing validation
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucket)
          .key(path)
          .build();
      
      s3.putObject(putObjectRequest, RequestBody.fromString(content, StandardCharsets.UTF_8));
      
      return String.format("https://%s.s3.amazonaws.com/%s", bucket, path);
    } catch (Exception e) {
      logger.error(SERVICE_NAME, "Failed to create schema: " + e.getMessage());
      throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public String createSystemSchema(String filePath, String content) throws ApplicationException {
    updateDataPartitionId();
    return this.createSchema(filePath, content);
  }

  @Override
  public String getSchema(String dataPartitionId, String filePath) throws NotFoundException, ApplicationException {
    S3ClientWithBucket s3ClientWithBucket = getS3ClientWithBucket(dataPartitionId);
    S3Client s3 = s3ClientWithBucket.getS3Client();
    String bucket = s3ClientWithBucket.getBucketName();
    String path = resolvePath(dataPartitionId, filePath);

    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(path)
          .build();
      
      ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(getObjectRequest);
      String content = objectBytes.asUtf8String();
      
      // Process the content to ensure it's compatible with schema validation
      // This is needed because AWS SDK v2 returns immutable collections that cause
      // UnsupportedOperationException in SchemaUtil.findClosestSchemas
      return processSchemaContent(content);
    } catch (AwsServiceException ex) {
      if (ex.statusCode() == 404) {
        logger.error(SERVICE_NAME, String.format(SchemaConstants.SCHEMA_NOT_PRESENT, ex.getMessage()));
        throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
      } else {
        logger.error(SERVICE_NAME, String.format("Get Schema failed: %s", ex.getMessage()));
        throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
      }
    } catch (Exception ex) {
      logger.error(SERVICE_NAME, String.format("Get Schema failed: %s", ex.toString()));
      throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public String getSystemSchema(String filePath) throws NotFoundException, ApplicationException {
    return processSchemaContent(this.getSchema(sharedTenant, filePath));
  }

  private String resolvePath(String dataPartitionId, String filePath) {
    return String.format("schema/%s/%s", dataPartitionId, filePath);
  }

  @Override
  public boolean cleanSchemaProject(String schemaId) throws ApplicationException {
    logger.info("Delete schema: " + schemaId);

    S3ClientWithBucket s3ClientWithBucket = getS3ClientWithBucket();
    S3Client s3 = s3ClientWithBucket.getS3Client();
    String bucket = s3ClientWithBucket.getBucketName();
    String path = resolvePath(headers.getPartitionIdWithFallbackToAccountId(), schemaId);

    try {
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(path)
          .build();
      
      s3.deleteObject(deleteObjectRequest);
      logger.info("Schema deleted: " + schemaId);
      return true;
    } catch (Exception e) {
      logger.error("Failed to delete schema " + schemaId + ": " + e.getMessage());
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

  /**
   * Process the schema content to ensure it's compatible with schema validation.
   * This is needed because AWS SDK v2 returns immutable collections that cause
   * UnsupportedOperationException in SchemaUtil.findClosestSchemas.
   * 
   * @param content The schema content as a string
   * @return The processed content that's compatible with schema validation
   */
  private String processSchemaContent(String content) {
    if (content == null || content.trim().isEmpty()) {
      return content;
    }
    
    try {
      // Parse the content to determine its structure
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode node = objectMapper.readTree(content);
      
      // If it's an array, ensure it's mutable by converting to a new array
      if (node.isArray()) {
        logger.info(SERVICE_NAME, "Converting array content to ensure mutability");
        
        // Create a new mutable array
        ArrayNode newArray = JsonNodeFactory.instance.arrayNode();
        ArrayNode arrayNode = (ArrayNode) node;
        
        // Copy all elements to the new array
        for (JsonNode element : arrayNode) {
          newArray.add(element);
        }
        
        // Convert back to JSON string
        return objectMapper.writeValueAsString(newArray);
      }
      
      // For objects or other types, return as is
      return content;
      
    } catch (Exception e) {
      // If there's an error processing the JSON, log it and return the original content
      logger.warning(SERVICE_NAME, "Error processing schema content: " + e.getMessage());
      return content;
    }
  }
}
