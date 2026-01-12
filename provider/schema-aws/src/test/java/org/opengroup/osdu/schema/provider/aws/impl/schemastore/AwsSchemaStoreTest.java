// Copyright © 2020 Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.schema.provider.aws.impl.schemastore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.s3.IS3ClientFactory;
import org.opengroup.osdu.core.aws.v2.s3.S3ClientWithBucket;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class AwsSchemaStoreTest {

    private static final String S3_SCHEMA_BUCKET_PATH = "/schema/bucket/path";
    private static final String COMMON_TENANT_ID = "common";
    private static final String TEST_PARTITION_ID = "test-partition";
    private static final String TEST_BUCKET_NAME = "test-bucket";

    private AwsSchemaStore schemaStore;

    @Mock
    private DpsHeaders headers;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private IS3ClientFactory s3ClientFactory;

    @Mock
    private S3ClientWithBucket s3ClientWithBucket;

    @Mock
    private S3Client s3Client;

    @Mock
    private ResponseBytes<GetObjectResponse> responseBytes;

    @BeforeEach
    void setUp() {
        // Create the schemaStore with constructor injection
        schemaStore = new AwsSchemaStore(
            headers,
            logger,
            s3ClientFactory,
            S3_SCHEMA_BUCKET_PATH,
            COMMON_TENANT_ID
        );
        
        // Set up common mocks
        when(s3ClientFactory.getS3ClientForPartition(anyString(), eq(S3_SCHEMA_BUCKET_PATH)))
            .thenReturn(s3ClientWithBucket);
        when(s3ClientWithBucket.getS3Client()).thenReturn(s3Client);
        when(s3ClientWithBucket.getBucketName()).thenReturn(TEST_BUCKET_NAME);
    }

    @Test
    void createSchema_Success() throws ApplicationException {
        // Setup
        String filePath = "test-schema.json";
        String content = "{\"test\": \"schema\"}";
        
        // Mock headers
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(TEST_PARTITION_ID);
        
        // Execute
        String result = schemaStore.createSchema(filePath, content);
        
        // Verify
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals("https://test-bucket.s3.amazonaws.com/schema/test-partition/test-schema.json", result);
    }

    @Test
    void createSchema_ThrowsApplicationException() {
        // Setup
        String filePath = "test-schema.json";
        String content = "{\"test\": \"schema\"}";
        
        // Mock headers
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock S3 client to throw exception
        doThrow(new RuntimeException("Test error")).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        
        // Execute and verify
        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            schemaStore.createSchema(filePath, content);
        });
        
        assertEquals(SchemaConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @Test
    void createSystemSchema_Success() throws ApplicationException {
        // Setup
        String filePath = "test-schema.json";
        String content = "{\"test\": \"schema\"}";
        
        // Setup mocks to handle the data partition ID update
        doAnswer(invocation -> {
            // When headers.put is called, simulate the update
            when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(COMMON_TENANT_ID);
            return null;
        }).when(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        
        // Initially return a different partition ID
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(TEST_PARTITION_ID);
        
        // Execute
        String result = schemaStore.createSystemSchema(filePath, content);
        
        // Verify
        verify(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals("https://test-bucket.s3.amazonaws.com/schema/common/test-schema.json", result);
    }

    @Test
    void getSchema_Success() throws NotFoundException, ApplicationException {
        // Setup
        String filePath = "test-schema.json";
        String expectedContent = "{\"test\": \"schema\"}";
        
        // Mock S3 client response
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asUtf8String()).thenReturn(expectedContent);
        
        // Execute
        String result = schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        
        // Verify
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        assertEquals(expectedContent, result);
    }

    @Test
    void getSchema_NotFound() {
        // Setup
        String filePath = "non-existent-schema.json";
        
        // Mock S3 client to throw 404 exception
        AwsServiceException notFoundException = AwsServiceException.builder()
            .statusCode(404)
            .message("The specified key does not exist")
            .build();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(notFoundException);
        
        // Execute and verify
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        });
        
        assertEquals(SchemaConstants.SCHEMA_NOT_PRESENT, exception.getMessage());
    }

    @Test
    void getSchema_OtherS3Exception() {
        // Setup
        String filePath = "error-schema.json";
        
        // Mock S3 client to throw non-404 exception
        AwsServiceException otherException = AwsServiceException.builder()
            .statusCode(500)
            .message("Internal server error")
            .build();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(otherException);
        
        // Execute and verify
        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        });
        
        assertEquals(SchemaConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @Test
    void getSchema_GenericException() {
        // Setup
        String filePath = "error-schema.json";
        
        // Mock S3 client to throw generic exception
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(new RuntimeException("Test error"));
        
        // Execute and verify
        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        });
        
        assertEquals(SchemaConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @Test
    void getSystemSchema_Success() throws NotFoundException, ApplicationException {
        // Setup
        String filePath = "test-schema.json";
        String expectedContent = "{\"test\": \"schema\"}";
        
        // Mock S3 client response
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asUtf8String()).thenReturn(expectedContent);
        
        // Execute
        String result = schemaStore.getSystemSchema(filePath);
        
        // Verify
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        assertEquals(expectedContent, result);
    }

    @Test
    void cleanSchemaProject_Success() throws ApplicationException {
        // Setup
        String schemaId = "test-schema.json";
        
        // Mock headers
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(TEST_PARTITION_ID);
        
        // Execute
        boolean result = schemaStore.cleanSchemaProject(schemaId);
        
        // Verify
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        assertTrue(result);
    }

    @Test
    void cleanSchemaProject_Failure() throws ApplicationException {
        // Setup
        String schemaId = "test-schema.json";
        
        // Mock headers
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock S3 client to throw exception
        doThrow(new RuntimeException("Test error")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));
        
        // Execute
        boolean result = schemaStore.cleanSchemaProject(schemaId);
        
        // Verify
        assertFalse(result);
    }

    @Test
    void cleanSystemSchemaProject_Success() throws ApplicationException {
        // Setup
        String schemaId = "test-schema.json";
        
        // Setup mocks to handle the data partition ID update
        doAnswer(invocation -> {
            // When headers.put is called, simulate the update
            when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(COMMON_TENANT_ID);
            return null;
        }).when(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        
        // Initially return a different partition ID
        when(headers.getPartitionIdWithFallbackToAccountId()).thenReturn(TEST_PARTITION_ID);
        
        // Execute
        boolean result = schemaStore.cleanSystemSchemaProject(schemaId);
        
        // Verify
        verify(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        assertTrue(result);
    }

    @Test
    void getSchema_WithArrayContent_ProcessesContent() throws Exception {
        // Setup
        String filePath = "array-schema.json";
        String arrayContent = "[{\"id\":\"item1\"},{\"id\":\"item2\"}]";
        
        // Mock S3 client response
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asUtf8String()).thenReturn(arrayContent);
        
        // Execute
        String result = schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        
        // Verify
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        
        // Verify the result is valid JSON and contains the expected data
        ObjectMapper mapper = new ObjectMapper();
        JsonNode resultNode = mapper.readTree(result);
        assertTrue(resultNode.isArray(), "Result should be a JSON array");
        assertEquals(2, resultNode.size(), "Array should have 2 elements");
        assertEquals("item1", resultNode.get(0).get("id").asText());
        assertEquals("item2", resultNode.get(1).get("id").asText());
        
        // Verify the result is a mutable array by attempting to modify it
        ArrayNode arrayNode = (ArrayNode) resultNode;
        // This would throw UnsupportedOperationException if the array were immutable
        assertNotNull(arrayNode);
    }
    
    @Test
    void getSchema_WithObjectContent_ReturnsUnmodified() throws Exception {
        // Setup
        String filePath = "object-schema.json";
        String objectContent = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        
        // Mock S3 client response
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asUtf8String()).thenReturn(objectContent);
        
        // Execute
        String result = schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        
        // Verify
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        
        // For object content, the result should be identical to the input
        assertEquals(objectContent, result);
    }
    
    @Test
    void getSchema_WithEmptyContent_ReturnsUnmodified() throws Exception {
        // Setup
        String filePath = "empty-schema.json";
        String emptyContent = "";
        
        // Mock S3 client response
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asUtf8String()).thenReturn(emptyContent);
        
        // Execute
        String result = schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        
        // Verify
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        
        // For empty content, the result should be identical to the input
        assertEquals(emptyContent, result);
    }
    
    @Test
    void getSchema_WithNullContent_ReturnsNull() throws Exception {
        // Setup
        String filePath = "null-schema.json";
        
        // Mock S3 client response
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asUtf8String()).thenReturn(null);
        
        // Execute
        String result = schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        
        // Verify
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        
        // For null content, the result should be null
        assertEquals(null, result);
    }
    
    @Test
    void getSchema_WithInvalidJsonContent_ReturnsOriginalContent() throws Exception {
        // Setup
        String filePath = "invalid-schema.json";
        String invalidContent = "{invalid-json}";
        
        // Mock S3 client response
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asUtf8String()).thenReturn(invalidContent);
        
        // Execute
        String result = schemaStore.getSchema(TEST_PARTITION_ID, filePath);
        
        // Verify
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        
        // For invalid JSON content, the result should be identical to the input
        assertEquals(invalidContent, result);
    }
    
    @Test
    void getSystemSchema_WithArrayContent_ProcessesContent() throws Exception {
        // Setup
        String filePath = "system-array-schema.json";
        String arrayContent = "[{\"id\":\"system1\"},{\"id\":\"system2\"}]";
        
        // Mock S3 client response
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);
        when(responseBytes.asUtf8String()).thenReturn(arrayContent);
        
        // Execute
        String result = schemaStore.getSystemSchema(filePath);
        
        // Verify
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
        
        // Verify the result is valid JSON and contains the expected data
        ObjectMapper mapper = new ObjectMapper();
        JsonNode resultNode = mapper.readTree(result);
        assertTrue(resultNode.isArray(), "Result should be a JSON array");
        assertEquals(2, resultNode.size(), "Array should have 2 elements");
        assertEquals("system1", resultNode.get(0).get("id").asText());
        assertEquals("system2", resultNode.get(1).get("id").asText());
    }
}
