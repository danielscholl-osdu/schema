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

package org.opengroup.osdu.schema.provider.azure.impl.schemainfostore;

import com.azure.cosmos.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.impl.schemainfostore.AzureSourceStore;
import org.opengroup.osdu.schema.azure.definitions.SourceDoc;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AzureSourceStoreTest {
    @Mock
    private CosmosStore cosmosStore;

    @InjectMocks
    private AzureSourceStore store;

    @Mock
    Source mockSource;

    @Mock
    DpsHeaders headers;

    @Mock
    JaxRsDpsLog log;

    private static final String dataPartitionId = "testPartitionId";
    private static final String sourceId = "testSourceId";

    @Before
    public void init() {
        initMocks(this);
        Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
        Mockito.when(mockSource.getSourceId()).thenReturn(sourceId);
    }

    @Test
    public void testGetSource() throws NotFoundException, ApplicationException, IOException {
        SourceDoc sourceDoc = getSourceDoc(dataPartitionId, sourceId);
        Optional<SourceDoc> cosmosItem = Optional.of(sourceDoc);
        doReturn(cosmosItem)
                .when(cosmosStore)
                .findItem(
                        eq(dataPartitionId),
                        any(),
                        any(),
                        eq(dataPartitionId + ":" + sourceId),
                        eq(dataPartitionId),
                        any());
        assertNotNull(store.get(sourceId));
        assertEquals(sourceId, store.get(sourceId).getSourceId());
    }

    @Test
    public void testGetSource_NotFoundException() throws IOException {
        String sourceId = "";
        Optional<SourceDoc> cosmosItem = Optional.empty();
        doReturn(cosmosItem)
                .when(cosmosStore)
                .findItem(
                        eq(dataPartitionId),
                        any(),
                        any(),
                        eq(dataPartitionId + ":" + ""),
                        eq(dataPartitionId),
                        any());
        try {
            store.get(sourceId);
            fail("Should not succeed");
        } catch (NotFoundException e) {
            assertEquals("bad input parameter", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateSource() throws  ApplicationException, BadRequestException {
        doNothing().when(cosmosStore).createItem(anyString(), any(), any(), any(), any());
        assertNotNull(store.create(mockSource));
    }

    @Test
    public void testCreateSource_BadRequestException()
            throws NotFoundException, ApplicationException, BadRequestException, IOException {
        AppException exception = getMockAppException(409);
        doThrow(exception).when(cosmosStore).createItem(eq(dataPartitionId), any(), any(), any(), any());
        try {
            store.create(mockSource);
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Source already registered with Id: testSourceId", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateSource_ApplicationException()
            throws NotFoundException, ApplicationException, BadRequestException, CosmosException {
        AppException exception = getMockAppException(500);
        doThrow(exception).when(cosmosStore).createItem(eq(dataPartitionId), any(), any(), any(), any());

        try {
            store.create(mockSource);
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals(SchemaConstants.INVALID_INPUT, e.getMessage());
        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    private SourceDoc getSourceDoc(String partitionId, String sourceName)
    {
        String id = partitionId + ":" + sourceName;
        Source source = new Source();
        source.setSourceId(sourceName);
        return new SourceDoc(id, partitionId, source);
    }

    private AppException getMockAppException(int errorCode) {
        AppException mockException = mock(AppException.class);
        AppError mockError = mock(AppError.class);
        lenient().when(mockException.getError()).thenReturn(mockError);
        lenient().when(mockError.getCode()).thenReturn(errorCode);
        return mockException;
    }
}
