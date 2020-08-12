// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.schema.provider.azure.impl.schemastore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.azure.blobstorage.BlobStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.impl.schemastore.AzureSchemaStore;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AzureSchemaStoreTest {
    @InjectMocks
    AzureSchemaStore schemaStore;

    @Mock
    BlobStore blobStore;

    @Mock
    DpsHeaders headers;

    @Mock
    JaxRsDpsLog log;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final String dataPartitionId = "dataPartitionId";
    private static final String FILE_PATH = "/test-folder/test-file";
    private static final String CONTENT = "Hello World";
    private static final String filePath = dataPartitionId + ":" + FILE_PATH + SchemaConstants.JSON_EXTENSION;

    @Before
    public void init(){
        initMocks(this);
        doReturn(dataPartitionId).when(headers).getPartitionId();
    }

    @Test
    public void testGetSchema() throws ApplicationException, NotFoundException {
        doReturn(CONTENT).when(blobStore).readFromBlob(dataPartitionId, filePath);
        Assert.assertEquals(CONTENT, schemaStore.getSchema(dataPartitionId, FILE_PATH));
    }

    @Test
    public void testGetSchema_NotFound() throws ApplicationException, NotFoundException {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(SchemaConstants.SCHEMA_NOT_PRESENT);
        doReturn(null).when(blobStore).readFromBlob(dataPartitionId, filePath);
        schemaStore.getSchema(dataPartitionId, FILE_PATH);
    }

    @Test
    public void testGetSchema_Failure() throws ApplicationException, NotFoundException {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(SchemaConstants.SCHEMA_NOT_PRESENT);

        doThrow(AppException.class).when(blobStore).readFromBlob(dataPartitionId, filePath);
        schemaStore.getSchema(dataPartitionId, FILE_PATH);
    }

    @Test
    public void testDeleteSchema() throws ApplicationException {
        doReturn(true).when(blobStore).deleteFromBlob(dataPartitionId, filePath);

        Boolean result = schemaStore.cleanSchemaProject(FILE_PATH);
        Assert.assertEquals(true, result);
    }

    @Test
    public void testDeleteSchema_Failure() throws ApplicationException {
        expectedException.expect(ApplicationException.class);
        expectedException.expectMessage(SchemaConstants.INTERNAL_SERVER_ERROR);

        doThrow(AppException.class).when(blobStore).deleteFromBlob(dataPartitionId, filePath);
        schemaStore.cleanSchemaProject(FILE_PATH);
    }

    @Test
    public void testCreateSchema() throws ApplicationException {

        doNothing().when(blobStore).writeToBlob(dataPartitionId, filePath, CONTENT);
        Assert.assertEquals(filePath, schemaStore.createSchema(FILE_PATH, CONTENT));
    }

    @Test
    public void testCreateSchema_Failure() throws ApplicationException {
        expectedException.expect(ApplicationException.class);
        expectedException.expectMessage(SchemaConstants.INTERNAL_SERVER_ERROR);

        doThrow(AppException.class).when(blobStore).writeToBlob(dataPartitionId, filePath, CONTENT);
        schemaStore.createSchema(FILE_PATH, CONTENT);
    }
}
