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
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.aws.models.AuthorityDoc;

@ExtendWith(MockitoExtension.class)
class AwsAuthorityStoreTest {

    private static final String AUTHORITY_TABLE_PATH = "/schema/authority/table";
    private static final String COMMON_TENANT_ID = "common";

    private AwsAuthorityStore authorityStore;

    @Mock
    private DpsHeaders headers;

    @Mock
    private IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;

    @Mock
    private DynamoDBQueryHelper<AuthorityDoc> queryHelper;

    @Mock
    private JaxRsDpsLog logger;

    @BeforeEach
    void setUp() {
        // Create the authorityStore with constructor injection
        authorityStore = new AwsAuthorityStore(
            headers,
            logger,
            dynamoDBQueryHelperFactory,
            AUTHORITY_TABLE_PATH,
            COMMON_TENANT_ID
        );
        
        // Set up the query helper factory to return our mock query helper
        when(dynamoDBQueryHelperFactory.createQueryHelper(
                any(DpsHeaders.class),
                eq(AUTHORITY_TABLE_PATH),
                eq(AuthorityDoc.class))).thenReturn(queryHelper);
    }

    @Test
    void get_ReturnsAuthority() throws NotFoundException, ApplicationException {
        // Setup
        String authorityId = "test-authority";
        String partitionId = "test-partition";
        Authority authority = new Authority();
        authority.setAuthorityId(authorityId);
        
        AuthorityDoc authorityDoc = new AuthorityDoc(
                partitionId + ":" + authorityId,
                partitionId,
                authority);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper
        when(queryHelper.getItem(partitionId + ":" + authorityId)).thenReturn(Optional.of(authorityDoc));
        
        // Execute
        Authority result = authorityStore.get(authorityId);
        
        // Verify
        assertEquals(authority, result);
    }

    @Test
    void get_ThrowsNotFoundException() {
        // Setup
        String authorityId = "non-existent-authority";
        String partitionId = "test-partition";

        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper to return empty (authority not found)
        when(queryHelper.getItem(partitionId + ":" + authorityId)).thenReturn(Optional.empty());

        // Execute and verify
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            authorityStore.get(authorityId);
        });

        assertEquals(SchemaConstants.INVALID_INPUT, exception.getMessage());
    }

    @Test
    void getSystemAuthority_ReturnsAuthority() throws NotFoundException, ApplicationException {
        // Setup
        String authorityId = "test-authority";
        Authority authority = new Authority();
        authority.setAuthorityId(authorityId);
        
        // Setup mocks to handle the data partition ID update
        doAnswer(invocation -> {
            // When headers.put is called, simulate the update by changing what getPartitionId returns
            when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
            return null;
        }).when(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        
        // Initially return a different partition ID
        when(headers.getPartitionId()).thenReturn("original-partition");
        
        // Mock query helper for the common tenant ID format
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + authorityId))
            .thenReturn(Optional.of(new AuthorityDoc(COMMON_TENANT_ID + ":" + authorityId, COMMON_TENANT_ID, authority)));
        
        // Execute
        Authority result = authorityStore.getSystemAuthority(authorityId);
        
        // Verify
        assertEquals(authority, result);
        
        // Verify that the data partition ID was updated
        verify(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
    }

    @Test
    void create_CreatesAuthority() throws ApplicationException, BadRequestException {
        // Setup
        String authorityId = "new-authority";
        String partitionId = "test-partition";
        Authority authority = new Authority();
        authority.setAuthorityId(authorityId);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper to return empty (authority doesn't exist yet)
        when(queryHelper.getItem(partitionId + ":" + authorityId)).thenReturn(Optional.empty());
        
        // Mock putItem to do nothing (successful save)
        doNothing().when(queryHelper).putItem(any(AuthorityDoc.class));
        
        // Execute
        Authority result = authorityStore.create(authority);
        
        // Verify
        assertEquals(authority, result);
        
        // Verify that putItem was called with the correct document
        verify(queryHelper).putItem(any(AuthorityDoc.class));
        
        // Verify that the logger was called
        verify(logger).info(SchemaConstants.AUTHORITY_CREATED);
    }

    @Test
    void create_ThrowsBadRequestException_WhenAuthorityExists() {
        // Setup
        String authorityId = "existing-authority";
        String partitionId = "test-partition";
        Authority authority = new Authority();
        authority.setAuthorityId(authorityId);
        
        AuthorityDoc existingDoc = new AuthorityDoc(
                partitionId + ":" + authorityId,
                partitionId,
                authority);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper to return existing authority
        when(queryHelper.getItem(partitionId + ":" + authorityId)).thenReturn(Optional.of(existingDoc));
        
        // Execute and verify
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authorityStore.create(authority);
        });
        
        // Verify that the logger was called
        verify(logger).warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
    }

    @Test
    void create_ThrowsApplicationException_OnGenericError() {
        // Setup
        String authorityId = "error-authority";
        String partitionId = "test-partition";
        Authority authority = new Authority();
        authority.setAuthorityId(authorityId);

        // Mock headers
        when(headers.getPartitionId()).thenReturn(partitionId);
        
        // Mock query helper to return empty (authority doesn't exist yet)
        when(queryHelper.getItem(partitionId + ":" + authorityId)).thenReturn(Optional.empty());
        
        // Mock putItem to throw exception
        doThrow(new RuntimeException("Test error")).when(queryHelper).putItem(any(AuthorityDoc.class));
        
        // Execute and verify
        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            authorityStore.create(authority);
        });
        
        assertEquals(SchemaConstants.INVALID_INPUT, exception.getMessage());
        
        // Verify that the logger was called
        verify(logger).error(anyString());
    }

    @Test
    void createSystemAuthority_CreatesAuthority() throws ApplicationException, BadRequestException {
        // Setup
        String authorityId = "new-system-authority";
        Authority authority = new Authority();
        authority.setAuthorityId(authorityId);
        
        // Setup mocks to handle the data partition ID update
        doAnswer(invocation -> {
            // When headers.put is called, simulate the update by changing what getPartitionId returns
            when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
            return null;
        }).when(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        
        // Initially return a different partition ID
        when(headers.getPartitionId()).thenReturn("original-partition");
        
        // Mock query helper for the common tenant ID format
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + authorityId)).thenReturn(Optional.empty());
        
        // Mock putItem to do nothing (successful save)
        doNothing().when(queryHelper).putItem(any(AuthorityDoc.class));
        
        // Execute
        Authority result = authorityStore.createSystemAuthority(authority);
        
        // Verify
        assertEquals(authority, result);
        
        // Verify that the data partition ID was updated
        verify(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        
        // Verify that putItem was called with the correct document
        verify(queryHelper).putItem(any(AuthorityDoc.class));
    }
}
