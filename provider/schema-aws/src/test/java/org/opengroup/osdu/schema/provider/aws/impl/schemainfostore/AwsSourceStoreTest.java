/*
 * Copyright © Amazon Web Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.aws.models.SourceDoc;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsSourceStoreTest {

    @InjectMocks
    private AwsSourceStore sourceStore;

    @Mock
    private DpsHeaders headers;

    @Mock
    private DynamoDBQueryHelper<SourceDoc> queryHelper;

    @Mock
    private IDynamoDBQueryHelperFactory queryHelperFactory;

    @Mock
    private JaxRsDpsLog logger;

    private static final String COMMON_TENANT_ID = "common";
    private static final String PARTITION_ID = "partitionId";
    private static final String SOURCE_ID = "source";
    private static final String SOURCE_TABLE_PARAM = "sourceTableParam";

    @BeforeEach
    void setUp() {
        // Set the sourceTableParameterRelativePath field
        ReflectionTestUtils.setField(sourceStore, "sourceTableParameterRelativePath", SOURCE_TABLE_PARAM);
        ReflectionTestUtils.setField(sourceStore, "sharedTenant", COMMON_TENANT_ID);
        
        // Mock the factory to return our mock queryHelper
        when(queryHelperFactory.createQueryHelper(any(DpsHeaders.class), eq(SOURCE_TABLE_PARAM), eq(SourceDoc.class)))
                .thenReturn(queryHelper);
                
        // Default behavior for queryHelper.getItem() to avoid strict stubbing issues
        when(queryHelper.getItem(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void get_shouldReturnSource_whenSourceExists() throws NotFoundException, ApplicationException {
        // Arrange
        Source expected = new Source();
        SourceDoc sourceDoc = new SourceDoc(PARTITION_ID + ":" + SOURCE_ID, PARTITION_ID, expected);
        when(headers.getPartitionId()).thenReturn(PARTITION_ID);
        when(queryHelper.getItem(PARTITION_ID + ":" + SOURCE_ID)).thenReturn(Optional.of(sourceDoc));

        // Act
        Source actual = sourceStore.get(SOURCE_ID);

        // Assert
        assertEquals(expected, actual);
        verify(queryHelper).getItem(PARTITION_ID + ":" + SOURCE_ID);
    }

    @Test
    void getSystemSource_shouldReturnSource_whenSourceExists() throws NotFoundException, ApplicationException {
        // Arrange
        Source expected = new Source();
        SourceDoc sourceDoc = new SourceDoc(COMMON_TENANT_ID + ":" + SOURCE_ID, COMMON_TENANT_ID, expected);
        
        // Use doAnswer to ensure headers.getPartitionId() returns COMMON_TENANT_ID after headers.put() is called
        doAnswer(invocation -> COMMON_TENANT_ID).when(headers).getPartitionId();
        
        // Mock the queryHelper.getItem() to return the existing document
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + SOURCE_ID)).thenReturn(Optional.of(sourceDoc));

        // Act
        Source actual = sourceStore.getSystemSource(SOURCE_ID);

        // Assert
        assertEquals(expected, actual);
        verify(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        verify(queryHelper).getItem(COMMON_TENANT_ID + ":" + SOURCE_ID);
    }

    @Test
    void get_shouldThrowNotFoundException_whenSourceDoesNotExist() {
        // Arrange
        when(headers.getPartitionId()).thenReturn(PARTITION_ID);
        // Default behavior already set in setUp()

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> sourceStore.get(SOURCE_ID));
        assertEquals(AwsSourceStore.SOURCE_NOT_FOUND, exception.getMessage());
    }

    @Test
    void getSystemSource_shouldThrowNotFoundException_whenSourceDoesNotExist() {
        // Arrange
        when(headers.getPartitionId())
            .thenReturn(PARTITION_ID)
            .thenReturn(COMMON_TENANT_ID);
        // Default behavior already set in setUp()

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> sourceStore.getSystemSource(SOURCE_ID));
        assertEquals(AwsSourceStore.SOURCE_NOT_FOUND, exception.getMessage());
    }

    @Test
    void create_shouldCreateSource_whenSourceDoesNotExist() throws BadRequestException, ApplicationException {
        // Arrange
        Source source = new Source();
        source.setSourceId(SOURCE_ID);
        when(headers.getPartitionId()).thenReturn(PARTITION_ID);
        // Default behavior already set in setUp()
        doNothing().when(queryHelper).putItem(any(SourceDoc.class));

        // Act
        Source result = sourceStore.create(source);

        // Assert
        assertEquals(source, result);
        verify(queryHelper).putItem(any(SourceDoc.class));
    }

    @Test
    void createSystemSource_shouldCreateSource_whenSourceDoesNotExist() throws BadRequestException, ApplicationException {
        // Arrange
        Source source = new Source();
        source.setSourceId(SOURCE_ID);
        
        when(headers.getPartitionId())
            .thenReturn(PARTITION_ID)
            .thenReturn(COMMON_TENANT_ID);
        // Default behavior already set in setUp()
        doNothing().when(queryHelper).putItem(any(SourceDoc.class));

        // Act
        Source result = sourceStore.createSystemSource(source);

        // Assert
        assertEquals(source, result);
        verify(headers).put(SchemaConstants.DATA_PARTITION_ID, COMMON_TENANT_ID);
        verify(queryHelper).putItem(any(SourceDoc.class));
    }

    @Test
    void create_shouldThrowBadRequestException_whenSourceAlreadyExists() {
        // Arrange
        Source source = new Source();
        source.setSourceId(SOURCE_ID);
        SourceDoc existingDoc = new SourceDoc(PARTITION_ID + ":" + SOURCE_ID, PARTITION_ID, source);
        when(headers.getPartitionId()).thenReturn(PARTITION_ID);
        when(queryHelper.getItem(PARTITION_ID + ":" + SOURCE_ID)).thenReturn(Optional.of(existingDoc));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> sourceStore.create(source));
        assertTrue(exception.getMessage().contains("Source already registered with Id: " + SOURCE_ID));
    }

    @Test
    void createSystemSource_shouldThrowBadRequestException_whenSourceAlreadyExists() {
        // Arrange
        Source source = new Source();
        source.setSourceId(SOURCE_ID);
        SourceDoc existingDoc = new SourceDoc(COMMON_TENANT_ID + ":" + SOURCE_ID, COMMON_TENANT_ID, source);
        
        // Use doAnswer to ensure headers.getPartitionId() returns COMMON_TENANT_ID after headers.put() is called
        doAnswer(invocation -> COMMON_TENANT_ID).when(headers).getPartitionId();
        
        // Mock the queryHelper.getItem() to return the existing document
        when(queryHelper.getItem(COMMON_TENANT_ID + ":" + SOURCE_ID)).thenReturn(Optional.of(existingDoc));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> sourceStore.createSystemSource(source));
        assertTrue(exception.getMessage().contains("Source already registered with Id: " + SOURCE_ID));
    }

    @Test
    void create_shouldThrowApplicationException_whenSaveFails() {
        // Arrange
        Source source = new Source();
        source.setSourceId(SOURCE_ID);
        when(headers.getPartitionId()).thenReturn(PARTITION_ID);
        // Default behavior already set in setUp()
        doThrow(new RuntimeException("Test exception")).when(queryHelper).putItem(any(SourceDoc.class));

        // Act & Assert
        ApplicationException exception = assertThrows(ApplicationException.class, () -> sourceStore.create(source));
        assertEquals(SchemaConstants.INVALID_INPUT, exception.getMessage());
    }

    @Test
    void createSystemSource_shouldThrowApplicationException_whenSaveFails() {
        // Arrange
        Source source = new Source();
        source.setSourceId(SOURCE_ID);
        
        when(headers.getPartitionId())
            .thenReturn(PARTITION_ID)
            .thenReturn(COMMON_TENANT_ID);
        // Default behavior already set in setUp()
        doThrow(new RuntimeException("Test exception")).when(queryHelper).putItem(any(SourceDoc.class));

        // Act & Assert
        ApplicationException exception = assertThrows(ApplicationException.class, () -> sourceStore.createSystemSource(source));
        assertEquals(SchemaConstants.INVALID_INPUT, exception.getMessage());
    }
}
