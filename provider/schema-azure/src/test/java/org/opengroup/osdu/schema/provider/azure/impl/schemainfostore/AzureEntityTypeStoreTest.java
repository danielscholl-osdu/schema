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
import org.opengroup.osdu.azure.CosmosStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.impl.schemainfostore.AzureEntityTypeStore;
import org.opengroup.osdu.schema.azure.definitions.EntityTypeDoc;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AzureEntityTypeStoreTest {
    @Mock
    private CosmosStore cosmosStore;

    @InjectMocks
    private AzureEntityTypeStore store;

    @Mock
    EntityType mockEntityType;

    @Mock
    DpsHeaders headers;

    @Mock
    JaxRsDpsLog log;

    @Before
    public void init() {
        initMocks(this);
    }

    @Test
    public void testGetEntityType() throws NotFoundException, ApplicationException, IOException {
        String EntityTypeId = "testEntityId";
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        EntityTypeDoc entityTypeDoc = getEntityTypeDoc("test", EntityTypeId);
        Optional<EntityTypeDoc> cosmosItem = Optional.of(entityTypeDoc);
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), anyString(), anyString(), any());

        assertNotNull(store.get(EntityTypeId));
        assertEquals(EntityTypeId, store.get(EntityTypeId).getEntityTypeId());
    }

    @Test
    public void testGetEntityType_NotFoundException() throws IOException {
        String EntityTypeId = "";
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Optional<EntityTypeDoc> cosmosItem = Optional.empty();
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), anyString(), anyString(), any());

        try {
            store.get(EntityTypeId);
            fail("Should not succeed");
        } catch (NotFoundException e) {
            assertEquals("bad input parameter", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateEntityType() throws  ApplicationException, BadRequestException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(mockEntityType.getEntityTypeId()).thenReturn("testEntityId");
        doNothing().when(cosmosStore).upsertItem(anyString(), any(), any(), any());
        assertNotNull(store.create(mockEntityType));
    }

    @Test
    public void testCreateEntityType_BadRequestException()
            throws NotFoundException, ApplicationException, BadRequestException, IOException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(mockEntityType.getEntityTypeId()).thenReturn("testEntityId");
        EntityTypeDoc entityTypeDoc = getEntityTypeDoc("test", "testEntityId");
        Optional<EntityTypeDoc> cosmosItem = Optional.of(entityTypeDoc);
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), anyString(), anyString(), any());

        try {
            store.create(mockEntityType);
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("EntityType already registered with Id: testEntityId", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateEntityType_ApplicationException()
            throws NotFoundException, ApplicationException, BadRequestException, CosmosClientException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(mockEntityType.getEntityTypeId()).thenReturn("testEntityId");
        Optional<EntityTypeDoc> cosmosItem = Optional.empty();
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), anyString(), anyString(), any());
        doThrow(AppException.class).when(cosmosStore).upsertItem(anyString(), any(), any(), any());

        try {
            store.create(mockEntityType);
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals(SchemaConstants.INVALID_INPUT, e.getMessage());
        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    private EntityTypeDoc getEntityTypeDoc(String partitionId, String EntityTypeName)
    {
        String id = partitionId + ":" + EntityTypeName;
        EntityType EntityType = new EntityType();
        EntityType.setEntityTypeId(EntityTypeName);
        return new EntityTypeDoc(id, partitionId, EntityType);
    }
}
