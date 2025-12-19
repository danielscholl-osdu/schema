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
package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.aws.models.EntityTypeDoc;

@ExtendWith(MockitoExtension.class)
class AwsEntityTypeStoreTest {

    private static final String ENTITY_TYPE_TABLE_PATH = "/schema/entitytype/table";
    private static final String COMMON_TENANT_ID = "common";
    private static final String TEST_PARTITION_ID = "test-partition";

    private AwsEntityTypeStore entityTypeStore;

    @Mock
    private DpsHeaders headers;

    @Mock
    private JaxRsDpsLog log;

    @Mock
    private IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;

    @Mock
    private DynamoDBQueryHelper<EntityTypeDoc> queryHelper;

    @BeforeEach
    void setUp() {
        // Create the entityTypeStore with constructor injection
        entityTypeStore = new AwsEntityTypeStore(
            headers,
            log,
            dynamoDBQueryHelperFactory,
            ENTITY_TYPE_TABLE_PATH,
            COMMON_TENANT_ID
        );
        
        // Set up the query helper factory to return our mock query helper
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                any(DpsHeaders.class),
                eq(ENTITY_TYPE_TABLE_PATH),
                eq(EntityTypeDoc.class))).thenReturn(queryHelper);
    }

    @Test
    void get_ReturnsEntityType() throws NotFoundException, ApplicationException {
        // Setup
        String entityTypeId = "test-entity";
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(entityTypeId);
        
        EntityTypeDoc entityTypeDoc = new EntityTypeDoc(
                TEST_PARTITION_ID + ":" + entityTypeId,
                TEST_PARTITION_ID,
                entityType);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock query helper
        when(queryHelper.getItem(TEST_PARTITION_ID + ":" + entityTypeId)).thenReturn(Optional.of(entityTypeDoc));
        
        // Execute
        EntityType result = entityTypeStore.get(entityTypeId);
        
        // Verify
        assertEquals(entityType, result);
    }

    @Test
    void get_ThrowsNotFoundException() {
        // Setup
        String entityTypeId = "non-existent-entity";
        
        // Mock headers
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock query helper to return empty (entity type not found)
        when(queryHelper.getItem(TEST_PARTITION_ID + ":" + entityTypeId)).thenReturn(Optional.empty());

        // Execute and verify
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            entityTypeStore.get(entityTypeId);
        });

        assertEquals(SchemaConstants.INVALID_INPUT, exception.getMessage());
    }

    @Test
    void getSystemEntity_ReturnsEntityType() throws NotFoundException, ApplicationException {
        // Setup
        String entityTypeId = "test-entity";
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(entityTypeId);
        
        // Setup mocks to handle the data partition ID update
        doAnswer(invocation -> {
            // When headers.put is called, simulate the update by changing what getPartitionId returns
            when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
            return null;
        }).when(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        
        // Initially return a different partition ID
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock query helper for the specific ID format
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + entityTypeId))
            .thenReturn(Optional.of(new EntityTypeDoc(COMMON_TENANT_ID + ":" + entityTypeId, COMMON_TENANT_ID, entityType)));
        
        // Execute
        EntityType result = entityTypeStore.getSystemEntity(entityTypeId);
        
        // Verify
        assertEquals(entityType, result);
        
        // Verify that the data partition ID was updated
        verify(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
    }

    @Test
    void create_CreatesEntityType() throws ApplicationException, BadRequestException {
        // Setup
        String entityTypeId = "new-entity";
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(entityTypeId);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock query helper to return empty (entity type doesn't exist yet)
        when(queryHelper.getItem(TEST_PARTITION_ID + ":" + entityTypeId)).thenReturn(Optional.empty());
        
        // Mock putItem to do nothing (successful save)
        doNothing().when(queryHelper).putItem(any(EntityTypeDoc.class));
        
        // Execute
        EntityType result = entityTypeStore.create(entityType);
        
        // Verify
        assertEquals(entityType, result);
        
        // Verify that putItem was called with the correct document
        verify(queryHelper).putItem(any(EntityTypeDoc.class));
        
        // Verify that the logger was called
        verify(log).info(SchemaConstants.ENTITY_TYPE_CREATED);
    }

    @Test
    void create_ThrowsBadRequestException_WhenEntityTypeExists() {
        // Setup
        String entityTypeId = "existing-entity";
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(entityTypeId);
        
        EntityTypeDoc existingDoc = new EntityTypeDoc(
                TEST_PARTITION_ID + ":" + entityTypeId,
                TEST_PARTITION_ID,
                entityType);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock query helper to return existing entity type
        when(queryHelper.getItem(TEST_PARTITION_ID + ":" + entityTypeId)).thenReturn(Optional.of(existingDoc));
        
        // Execute and verify
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            entityTypeStore.create(entityType);
        });
        
        // Verify that the logger was called
        verify(log).warning(SchemaConstants.ENTITY_TYPE_EXISTS);
    }

    @Test
    void create_ThrowsApplicationException_OnGenericError() {
        // Setup
        String entityTypeId = "error-entity";
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(entityTypeId);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock query helper to return empty (entity type doesn't exist yet)
        when(queryHelper.getItem(TEST_PARTITION_ID + ":" + entityTypeId)).thenReturn(Optional.empty());
        
        // Mock putItem to throw exception
        doThrow(new RuntimeException("Test error")).when(queryHelper).putItem(any(EntityTypeDoc.class));
        
        // Execute and verify
        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            entityTypeStore.create(entityType);
        });
        
        assertEquals(SchemaConstants.INVALID_INPUT, exception.getMessage());
        
        // Verify that the logger was called
        verify(log).error(anyString());
    }

    @Test
    void createSystemEntity_CreatesEntityType() throws ApplicationException, BadRequestException {
        // Setup
        String entityTypeId = "new-system-entity";
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(entityTypeId);
        
        // Setup mocks to handle the data partition ID update
        doAnswer(invocation -> {
            // When headers.put is called, simulate the update by changing what getPartitionId returns
            when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
            return null;
        }).when(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        
        // Initially return a different partition ID
        when(headers.getPartitionId()).thenReturn(TEST_PARTITION_ID);
        
        // Mock query helper for the specific ID format
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + entityTypeId)).thenReturn(Optional.empty());
        
        // Mock putItem to do nothing (successful save)
        doNothing().when(queryHelper).putItem(any(EntityTypeDoc.class));
        
        // Execute
        EntityType result = entityTypeStore.createSystemEntity(entityType);
        
        // Verify
        assertEquals(entityType, result);
        
        // Verify that the data partition ID was updated
        verify(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        
        // Verify that putItem was called with the correct document
        verify(queryHelper).putItem(any(EntityTypeDoc.class));
    }
}
