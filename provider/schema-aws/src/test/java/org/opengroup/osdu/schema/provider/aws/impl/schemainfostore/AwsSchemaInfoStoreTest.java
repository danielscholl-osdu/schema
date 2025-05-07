// Copyright © 2020 Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.GsiQueryRequest;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.aws.models.SchemaInfoDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;

import com.google.common.collect.Lists;

import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@ExtendWith(MockitoExtension.class)
class AwsSchemaInfoStoreTest {

    private static final String SCHEMA_INFO_TABLE_PATH = "/schema/info/table";
    private static final String COMMON_TENANT_ID = "common";
    private static final String TEST_PARTITION_ID = "test-partition";
    private static final String TEST_SCHEMA_CONTENT = "{\"type\":\"object\",\"properties\":{}}";

    private AwsSchemaInfoStore schemaInfoStore;

    @Mock
    private DpsHeaders headers;

    @Mock
    private IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;

    @Mock
    private DynamoDBQueryHelper<SchemaInfoDoc> queryHelper;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private ITenantFactory tenantFactory;
    
    @Mock
    private ISchemaStore schemaStore;
    
    @Captor
    private ArgumentCaptor<GsiQueryRequest<SchemaInfoDoc>> gsiRequestCaptor;

    @BeforeEach
    void setUp() {
        // Create the schemaInfoStore with constructor injection
        schemaInfoStore = new AwsSchemaInfoStore(
            headers,
            tenantFactory,
            logger,
            schemaStore,
            dynamoDBQueryHelperFactory,
            SCHEMA_INFO_TABLE_PATH,
            COMMON_TENANT_ID
        );        
    }

    @Test
    void isUnique_ifNotExists_returnTrue() {
        // Setup
        String partitionId = "partitionId";
        String schemaId = "schemaId";
        
        // Mock query helper - use exact argument matching
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                anyString(),
                eq(SCHEMA_INFO_TABLE_PATH),
                eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        when(queryHelper.getItem(partitionId + ":" + schemaId)).thenReturn(Optional.empty());
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + schemaId)).thenReturn(Optional.empty());
        
        // Execute
        Boolean actual = schemaInfoStore.isUnique(schemaId, partitionId);
        
        // Verify
        assertTrue(actual);
    }

    @Test
    void isUnique_ifNotExists_returnTrue_SystemSchema() {
        // Setup
        String schemaId = "schemaId";
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            anyString(),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        TenantInfo tenant1 = new TenantInfo();
        tenant1.setName(COMMON_TENANT_ID);
        tenant1.setDataPartitionId(COMMON_TENANT_ID);
        TenantInfo tenant2 = new TenantInfo();
        tenant2.setName("partitionId");
        tenant2.setDataPartitionId("partitionId");
        Collection<TenantInfo> tenants = Lists.newArrayList(tenant1, tenant2);
        
        // Mock tenant factory
        when(tenantFactory.listTenantInfo()).thenReturn(tenants);

        // Mock query helper - use exact argument matching
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + schemaId)).thenReturn(Optional.empty());
        when(queryHelper.getItem("partitionId" + ":" + schemaId)).thenReturn(Optional.empty());
        
        // Execute
        Boolean actual = schemaInfoStore.isUniqueSystemSchema(schemaId);
        
        // Verify
        assertTrue(actual);
    }

    @Test
    void isUnique_ifExists_returnFalse() {
        // Setup
        String partitionId = "partitionId";
        String schemaId = "schemaId";
        SchemaInfoDoc mockDoc = new SchemaInfoDoc();
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                anyString(),
                eq(SCHEMA_INFO_TABLE_PATH),
                eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // The isUnique method checks both the shared tenant and the provided tenant
        // We need to stub both potential calls to getItem
        doReturn(Optional.empty()).when(queryHelper).getItem(COMMON_TENANT_ID + ":" + schemaId);
        doReturn(Optional.of(mockDoc)).when(queryHelper).getItem(partitionId + ":" + schemaId);
        
        // Execute
        Boolean actual = schemaInfoStore.isUnique(schemaId, partitionId);
        
        // Verify
        assertFalse(actual);
    }

    @Test
    void isUnique_ifExists_returnFalse_SystemSchema() {
        // Setup
        String schemaId = "schemaId";
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                anyString(),
                eq(SCHEMA_INFO_TABLE_PATH),
                eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Create tenant info
        TenantInfo tenant1 = new TenantInfo();
        tenant1.setName(COMMON_TENANT_ID);
        tenant1.setDataPartitionId(COMMON_TENANT_ID);
        TenantInfo tenant2 = new TenantInfo();
        tenant2.setName("partitionId");
        tenant2.setDataPartitionId("partitionId");
        Collection<TenantInfo> tenants = Lists.newArrayList(tenant1, tenant2);
        
        // Mock tenant factory
        when(tenantFactory.listTenantInfo()).thenReturn(tenants);
        
        // Mock query helper - first check will find the item
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + schemaId)).thenReturn(Optional.of(new SchemaInfoDoc()));
        
        // Execute
        Boolean actual = schemaInfoStore.isUniqueSystemSchema(schemaId);
        
        // Verify
        assertFalse(actual);
    }

    @Test
    void getSchemaInfo_GetsSchemaInfo() throws NotFoundException {
        // Setup
        String schemaId = "schemaId";
        String partitionId = "test-partition";
        SchemaInfo schemaInfo = new SchemaInfo();
        SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.builder()
                .schemaInfo(schemaInfo)
                .build();
                when(dynamoDBQueryHelperFactory.createQueryHelper(
                    any(DpsHeaders.class),
                    eq(SCHEMA_INFO_TABLE_PATH),
                    eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper
        when(queryHelper.getItem(partitionId + ":" + schemaId)).thenReturn(Optional.of(schemaInfoDoc));
        
        // Execute
        SchemaInfo actual = schemaInfoStore.getSchemaInfo(schemaId);
        
        // Verify
        assertEquals(schemaInfo, actual);
    }

    @Test
    void getSchemaInfo_ThrowsException() {
        // Setup
        String schemaId = "schemaId";
        String partitionId = "test-partition";
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            any(DpsHeaders.class),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper
        when(queryHelper.getItem(partitionId + ":" + schemaId)).thenReturn(Optional.empty());

        // Execute and verify
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            schemaInfoStore.getSchemaInfo(schemaId);
        });

        assertEquals(SchemaConstants.SCHEMA_NOT_PRESENT, exception.getMessage());
    }

    @Test
    void getSystemSchemaInfo_GetsSystemSchemaInfo() throws NotFoundException {
        // Setup
        String schemaId = "schemaId";
        SchemaInfo schemaInfo = new SchemaInfo();
        SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.builder()
                .schemaInfo(schemaInfo)
                .build();
                when(dynamoDBQueryHelperFactory.createQueryHelper(
                    any(DpsHeaders.class),
                    eq(SCHEMA_INFO_TABLE_PATH),
                    eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Use doReturn...when syntax to avoid strict stubbing issues
        doReturn(Optional.of(schemaInfoDoc)).when(queryHelper).getItem(anyString());
        
        // Execute
        SchemaInfo actual = schemaInfoStore.getSystemSchemaInfo(schemaId);
        
        // Verify
        assertEquals(schemaInfo, actual);
    }

    @Test
    void updateSchemaInfo_ThrowsNotFoundException() {
        // Setup
        String schemaId = "schemaId";
        String partitionId = "test-partition";
        SchemaIdentity schemaIdentity = new SchemaIdentity();
        schemaIdentity.setId(schemaId);
        SchemaInfo schemaInfo = new SchemaInfo();
        schemaInfo.setSchemaIdentity(schemaIdentity);
        schemaInfo.setScope(SchemaScope.INTERNAL); // Set scope to avoid NPE
        SchemaRequest schemaRequest = new SchemaRequest(schemaInfo, null);
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            any(DpsHeaders.class),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper to return empty for getItem (schema not found)
        when(queryHelper.getItem(partitionId + ":" + schemaId)).thenReturn(Optional.empty());

        // Execute and verify
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            schemaInfoStore.updateSchemaInfo(schemaRequest);
        });

        assertEquals("Cannot update schema that doesn't exist: " + schemaId, exception.getMessage());
    }

    @Test
    void updateSchemaInfo_UpdatesSchemaInfo() throws NotFoundException, ApplicationException, BadRequestException {
        // Setup
        String schemaId = "schemaId";
        String partitionId = "test-partition";
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            any(DpsHeaders.class),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);        
        // Create existing schema
        SchemaIdentity existingIdentity = new SchemaIdentity();
        existingIdentity.setId(schemaId);
        SchemaInfo existingSchema = new SchemaInfo();
        existingSchema.setSchemaIdentity(existingIdentity);
        existingSchema.setCreatedBy("original-creator");
        existingSchema.setDateCreated(new Date());
        existingSchema.setScope(SchemaScope.INTERNAL); // Set scope to avoid NPE
        SchemaInfoDoc existingDoc = SchemaInfoDoc.builder()
                .schemaInfo(existingSchema)
                .build();
        
        // Create update request
        SchemaIdentity updateIdentity = new SchemaIdentity();
        updateIdentity.setId(schemaId);
        SchemaInfo updateSchema = new SchemaInfo();
        updateSchema.setSchemaIdentity(updateIdentity);
        updateSchema.setStatus(SchemaStatus.DEVELOPMENT);
        updateSchema.setScope(SchemaScope.INTERNAL); // Set scope to avoid NPE
        SchemaRequest updateRequest = new SchemaRequest(updateSchema, null);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper to return existing schema
        when(queryHelper.getItem(partitionId + ":" + schemaId)).thenReturn(Optional.of(existingDoc));
        
        // Execute
        schemaInfoStore.updateSchemaInfo(updateRequest);
        
        // Verify that the schema was updated with preserved creation metadata
        assertEquals("original-creator", updateSchema.getCreatedBy());
        assertEquals(existingSchema.getDateCreated(), updateSchema.getDateCreated());
    }

    @SuppressWarnings("unchecked")
    @Test
    void createSchemaInfo_throwsConditionalCheckFailedException() {
        // Setup
        String schemaId = "schemaId";
        String partitionId = "test-partition";
        String userEmail = "test-user@example.com";
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                any(DpsHeaders.class),
                eq(SCHEMA_INFO_TABLE_PATH),
                eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        SchemaIdentity schemaIdentity = new SchemaIdentity();
        schemaIdentity.setId(schemaId);
        SchemaInfo schemaInfo = new SchemaInfo();
        schemaInfo.setSchemaIdentity(schemaIdentity);
        schemaInfo.setScope(SchemaScope.INTERNAL); // Set scope to avoid NPE
        schemaInfo.setStatus(SchemaStatus.PUBLISHED); // Set status to avoid NPE in SchemaInfoDoc.mapFrom
        SchemaRequest schemaRequest = new SchemaRequest(schemaInfo, null);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        when(headers.getUserEmail()).thenReturn(userEmail);
        
        // Mock query helper to throw ConditionalCheckFailedException when putItem is called
        // This simulates the scenario where the schema already exists
        doThrow(ConditionalCheckFailedException.class)
                .when(queryHelper)
                .putItem(any(PutItemEnhancedRequest.class));
                
        // Execute and verify
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            schemaInfoStore.createSchemaInfo(schemaRequest)
        );

        // Verify the exact error message from the implementation
        assertEquals("Schema " + partitionId + ":" + schemaId + " already exist. Can't create again.", exception.getMessage());
    }

    @Test
    void getSchemaInfoList_ReturnsEmptyList() {
        // Setup
        String tenantId = "tenantId";
        QueryParams queryParams = new QueryParams("authority", "source", "entityType", 10L, 20L, 30L, 3, 3, "status", "scope", true);
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                anyString(),
                eq(SCHEMA_INFO_TABLE_PATH),
                eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Mock query helper
        when(queryHelper.scanTable(any(ScanEnhancedRequest.class))).thenReturn(new ArrayList<>());
        
        // Execute
        List<SchemaInfo> actual = schemaInfoStore.getSchemaInfoList(queryParams, tenantId);
        
        // Verify
        List<SchemaInfo> expected = new ArrayList<SchemaInfo>();
        assertEquals(expected, actual);
    }

    @Test
    void getSchemaInfoList_ReturnsList() {
        // Setup
        String tenantId = "tenantId";
        QueryParams queryParams = new QueryParams("authority", "source", "entityType", 10L, 20L, 30L, 3, 3, "status", "scope", true);
        
        // Create schema info
        SchemaIdentity schemaIdentity = new SchemaIdentity();
        schemaIdentity.setAuthority("authority");
        schemaIdentity.setSource("source");
        schemaIdentity.setEntityType("entityType");
        schemaIdentity.setSchemaVersionMajor(1L);
        schemaIdentity.setSchemaVersionMinor(2L);
        schemaIdentity.setSchemaVersionPatch(3L);
        SchemaInfo schemaInfo = new SchemaInfo();
        schemaInfo.setSchemaIdentity(schemaIdentity);
        schemaInfo.setScope(SchemaScope.INTERNAL); // Set scope to avoid NPE
        
        SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.builder()
                .schemaInfo(schemaInfo)
                .build();
                when(dynamoDBQueryHelperFactory.createQueryHelper(
                    anyString(),
                    eq(SCHEMA_INFO_TABLE_PATH),
                    eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        List<SchemaInfoDoc> schemaInfoDocList = new ArrayList<>();
        schemaInfoDocList.add(schemaInfoDoc);
        
        // Mock query helper
        when(queryHelper.scanTable(any(ScanEnhancedRequest.class))).thenReturn(schemaInfoDocList);
        
        // Execute
        List<SchemaInfo> actual = schemaInfoStore.getSchemaInfoList(queryParams, tenantId);
        
        // Verify
        List<SchemaInfo> expected = new LinkedList<SchemaInfo>();
        expected.add(schemaInfo);
        assertEquals(expected, actual);
    }
    
    @Test
    void getSystemSchemaInfoList_ReturnsEmptyList() {
        // Setup
        QueryParams queryParams = new QueryParams("authority", "source", "entityType", 10L, 20L, 30L, 3, 3, "status", "scope", false);
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                anyString(),
                eq(SCHEMA_INFO_TABLE_PATH),
                eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Mock query helper
        when(queryHelper.scanTable(any(ScanEnhancedRequest.class))).thenReturn(new ArrayList<>());
        
        // Execute
        List<SchemaInfo> actual = schemaInfoStore.getSystemSchemaInfoList(queryParams);
        
        // Verify
        List<SchemaInfo> expected = new ArrayList<SchemaInfo>();
        assertEquals(expected, actual);
    }

    @Test
    void cleanSchema_ReturnsTrue() {
        // Setup
        String schemaId = "schemaId";
        String partitionId = "test-partition";
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                any(DpsHeaders.class),
                eq(SCHEMA_INFO_TABLE_PATH),
                eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper
        doNothing().when(queryHelper).deleteItem(partitionId + ":" + schemaId);
        
        // Execute
        boolean actual = schemaInfoStore.cleanSchema(schemaId);
        
        // Verify
        assertTrue(actual);
    }

    @Test
    void cleanSchema_ReturnsFalseOnException() {
        // Setup
        String schemaId = "schemaId";
        String partitionId = "test-partition";
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                any(DpsHeaders.class),
                eq(SCHEMA_INFO_TABLE_PATH),
                eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper
        doThrow(new RuntimeException()).when(queryHelper).deleteItem(partitionId + ":" + schemaId);

        // Execute
        boolean actual = schemaInfoStore.cleanSchema(schemaId);
        
        // Verify
        assertFalse(actual);
    }
    
    @Test
    void getLatestMinorVerSchema_NoResults_ReturnsEmptyString() throws ApplicationException {
        // Setup
        SchemaInfo inputSchema = createSchemaInfo("test:schema:1.0.0", 1L, 0L, 0L);
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock DynamoDBQueryHelper
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            any(DpsHeaders.class),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        
        // Mock empty results from GSI query
        when(queryHelper.queryByGSI(any(GsiQueryRequest.class))).thenReturn(new ArrayList<>());
        
        // Execute
        String result = schemaInfoStore.getLatestMinorVerSchema(inputSchema);
        
        // Verify
        assertEquals("", result);
        
        // Verify the GSI query was called with correct parameters
        verify(queryHelper).queryByGSI(gsiRequestCaptor.capture());
        GsiQueryRequest<SchemaInfoDoc> capturedRequest = gsiRequestCaptor.getValue();
        assertEquals("major-version-index", capturedRequest.getIndexName());
    }
    
    @Test
    void getLatestMinorVerSchema_SingleResult_ReturnsSchema() throws ApplicationException, NotFoundException {
        // Setup
        SchemaInfo inputSchema = createSchemaInfo("test:schema:1.0.0", 1L, 0L, 0L);
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock DynamoDBQueryHelper
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            any(DpsHeaders.class),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        
        // Create result schema
        SchemaInfoDoc resultDoc = SchemaInfoDoc.builder()
            .schemaInfo(inputSchema)
            .build();
        List<SchemaInfoDoc> queryResults = List.of(resultDoc);
        
        // Mock GSI query to return the single result
        when(queryHelper.queryByGSI(any(GsiQueryRequest.class))).thenReturn(queryResults);
        
        // Mock schema store to return schema content
        when(schemaStore.getSchema(TEST_PARTITION_ID, "test:schema:1.0.0")).thenReturn(TEST_SCHEMA_CONTENT);
        
        // Execute
        String result = schemaInfoStore.getLatestMinorVerSchema(inputSchema);
        
        // Verify
        assertEquals(TEST_SCHEMA_CONTENT, result);
        verify(schemaStore).getSchema(TEST_PARTITION_ID, "test:schema:1.0.0");
    }
    
    @Test
    void getLatestMinorVerSchema_MultipleResults_ReturnsHighestMinorVersion() throws ApplicationException, NotFoundException {
        // Setup
        SchemaInfo inputSchema = createSchemaInfo("test:schema:1.0.0", 1L, 0L, 0L);
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock DynamoDBQueryHelper
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            any(DpsHeaders.class),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        
        // Create multiple schema versions with different minor versions
        SchemaInfo schema1 = createSchemaInfo("test:schema:1.0.0", 1L, 0L, 0L);
        SchemaInfo schema2 = createSchemaInfo("test:schema:1.1.0", 1L, 1L, 0L);
        SchemaInfo schema3 = createSchemaInfo("test:schema:1.2.0", 1L, 2L, 0L);
        
        SchemaInfoDoc doc1 = SchemaInfoDoc.builder().schemaInfo(schema1).build();
        SchemaInfoDoc doc2 = SchemaInfoDoc.builder().schemaInfo(schema2).build();
        SchemaInfoDoc doc3 = SchemaInfoDoc.builder().schemaInfo(schema3).build();
        
        List<SchemaInfoDoc> queryResults = List.of(doc1, doc2, doc3);
        
        // Mock GSI query to return multiple results
        when(queryHelper.queryByGSI(any(GsiQueryRequest.class))).thenReturn(queryResults);
        
        // Mock schema store to return schema content for the highest minor version
        when(schemaStore.getSchema(TEST_PARTITION_ID, "test:schema:1.2.0")).thenReturn(TEST_SCHEMA_CONTENT);
        
        // Execute
        String result = schemaInfoStore.getLatestMinorVerSchema(inputSchema);
        
        // Verify
        assertEquals(TEST_SCHEMA_CONTENT, result);
        verify(schemaStore).getSchema(TEST_PARTITION_ID, "test:schema:1.2.0");
    }
    
    @Test
    void getLatestMinorVerSchema_SchemaStoreThrowsException_ReturnsEmptyString() throws ApplicationException, NotFoundException {
        // Setup
        SchemaInfo inputSchema = createSchemaInfo("test:schema:1.0.0", 1L, 0L, 0L);
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock DynamoDBQueryHelper
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            any(DpsHeaders.class),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        
        // Create result schema
        SchemaInfoDoc resultDoc = SchemaInfoDoc.builder()
            .schemaInfo(inputSchema)
            .build();
        List<SchemaInfoDoc> queryResults = List.of(resultDoc);
        
        // Mock GSI query to return the single result
        when(queryHelper.queryByGSI(any(GsiQueryRequest.class))).thenReturn(queryResults);
        
        // Mock schema store to throw NotFoundException
        when(schemaStore.getSchema(anyString(), anyString())).thenThrow(new NotFoundException("Schema not found"));
        
        // Execute
        String result = schemaInfoStore.getLatestMinorVerSchema(inputSchema);
        
        // Verify
        assertEquals("", result);
        verify(logger).error(eq("Schema not found for ID: test:schema:1.0.0"), any(NotFoundException.class));
    }
    
    @Test
    void getLatestMinorVerSchema_UnsortedResults_FindsHighestMinorVersion() throws ApplicationException, NotFoundException {
        // Setup
        SchemaInfo inputSchema = createSchemaInfo("test:schema:1.0.0", 1L, 0L, 0L);
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock DynamoDBQueryHelper
        when(dynamoDBQueryHelperFactory.createQueryHelper(
            any(DpsHeaders.class),
            eq(SCHEMA_INFO_TABLE_PATH),
            eq(SchemaInfoDoc.class))).thenReturn(queryHelper);
        
        // Create multiple schema versions with different minor versions in unsorted order
        SchemaInfo schema1 = createSchemaInfo("test:schema:1.2.0", 1L, 2L, 0L);
        SchemaInfo schema2 = createSchemaInfo("test:schema:1.0.0", 1L, 0L, 0L);
        SchemaInfo schema3 = createSchemaInfo("test:schema:1.1.0", 1L, 1L, 0L);
        SchemaInfo schema4 = createSchemaInfo("test:schema:1.3.0", 1L, 3L, 0L);
        
        SchemaInfoDoc doc1 = SchemaInfoDoc.builder().schemaInfo(schema1).build();
        SchemaInfoDoc doc2 = SchemaInfoDoc.builder().schemaInfo(schema2).build();
        SchemaInfoDoc doc3 = SchemaInfoDoc.builder().schemaInfo(schema3).build();
        SchemaInfoDoc doc4 = SchemaInfoDoc.builder().schemaInfo(schema4).build();
        
        List<SchemaInfoDoc> queryResults = List.of(doc1, doc2, doc3, doc4);
        
        // Mock GSI query to return multiple results
        when(queryHelper.queryByGSI(any(GsiQueryRequest.class))).thenReturn(queryResults);
        
        // Mock schema store to return schema content for the highest minor version
        when(schemaStore.getSchema(TEST_PARTITION_ID, "test:schema:1.3.0")).thenReturn(TEST_SCHEMA_CONTENT);
        
        // Execute
        String result = schemaInfoStore.getLatestMinorVerSchema(inputSchema);
        
        // Verify
        assertEquals(TEST_SCHEMA_CONTENT, result);
        verify(schemaStore).getSchema(TEST_PARTITION_ID, "test:schema:1.3.0");
    }
    
    // Helper method to create a SchemaInfo object with specified version numbers
    private SchemaInfo createSchemaInfo(String id, Long majorVersion, Long minorVersion, Long patchVersion) {
        SchemaIdentity identity = new SchemaIdentity();
        identity.setId(id);
        identity.setSchemaVersionMajor(majorVersion);
        identity.setSchemaVersionMinor(minorVersion);
        identity.setSchemaVersionPatch(patchVersion);
        identity.setAuthority("test-authority");
        identity.setSource("test-source");
        identity.setEntityType("test-entity");
        
        SchemaInfo schemaInfo = new SchemaInfo();
        schemaInfo.setSchemaIdentity(identity);
        schemaInfo.setScope(SchemaScope.INTERNAL);
        schemaInfo.setStatus(SchemaStatus.PUBLISHED);
        
        return schemaInfo;
    }
}
