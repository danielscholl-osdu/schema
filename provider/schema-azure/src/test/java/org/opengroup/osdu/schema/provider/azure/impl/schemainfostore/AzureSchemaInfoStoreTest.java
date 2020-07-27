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
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.azure.CosmosStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.azure.definitions.FlattenedSchemaInfo;
import org.opengroup.osdu.schema.azure.definitions.SchemaInfoDoc;
import org.opengroup.osdu.schema.azure.impl.schemainfostore.AzureSchemaInfoStore;
import org.opengroup.osdu.schema.azure.impl.schemastore.AzureSchemaStore;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AzureSchemaInfoStoreTest {
    @Mock
    private CosmosStore cosmosStore;

    @Mock
    private ITenantFactory tenantFactory;

    @InjectMocks
    private AzureSchemaInfoStore schemaInfoStore;

    @Mock
    JaxRsDpsLog logger;

    @Mock
    SchemaInfoDoc schemaInfoDoc;

    @Mock
    SchemaInfo schemaInfo;

    @Mock
    SchemaRequest schemaRequest;

    @Mock
    DpsHeaders headers;

    @Mock
    AzureSchemaStore schemaStore;

    @Mock
    FlattenedSchemaInfo flattenedSchemaInfo;

    private static final String dataPartitionId = "testPartitionId";
    private static final String CONTENT = "Hello World";
    private static final String schemaId = "os:wks:well.1.1.1";
    private static final String supersedingSchemaId = "os:wks:well.1.2.1";
    private static final String commonTenantId = "common";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void init() {
        initMocks(this);
        doReturn(dataPartitionId).when(headers).getPartitionId();
    }

    @Test
    public void testGetLatestMinorVersion_ReturnNull() throws NotFoundException, ApplicationException, IOException {

        List<SchemaInfoDoc> cosmosItem = new ArrayList<>();
        doReturn(cosmosItem).when(cosmosStore).queryItems(anyString(), anyString(), anyString(), any(), any(), any());
        assertEquals("", schemaInfoStore.getLatestMinorVerSchema(getMockSchemaInfo()));
    }

    @Test
    public void testGetLatestMinorVersion_Entity() throws NotFoundException, ApplicationException {
        List<SchemaInfoDoc> schemaInfoDocsList = new LinkedList<>();
        schemaInfoDocsList.add(getMockSchemaInfoDoc());
        doReturn(schemaInfoDocsList).when(cosmosStore).queryItems(anyString(), any(), any(), any(), any(), any());

        Mockito.when(schemaStore.getSchema(dataPartitionId, schemaId)).thenReturn(CONTENT);
        assertEquals(CONTENT, schemaInfoStore.getLatestMinorVerSchema(getMockSchemaInfo()));
    }

    @Test
    public void testGetSchemaInfo_NotEmpty() throws NotFoundException, ApplicationException {
        Optional<SchemaInfoDoc> cosmosItem = Optional.of(schemaInfoDoc);
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), anyString(), anyString(), any());

        doReturn(getFlattenedSchemaInfo()).when(schemaInfoDoc).getFlattenedSchemaInfo();
        SchemaInfo schemaInfo = schemaInfoStore.getSchemaInfo(schemaId);
        assertNotNull(schemaInfo);
    }

    @Test
    public void testGetSchemaInfo_Empty() throws NotFoundException, ApplicationException {
        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(SchemaConstants.SCHEMA_NOT_PRESENT);

        Optional<SchemaInfoDoc> cosmosItem = Optional.empty();
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), anyString(), anyString(), any());
        schemaInfoStore.getSchemaInfo(schemaId);
    }

    @Test
    public void testCreateSchemaInfo_Positive() throws ApplicationException, BadRequestException {
        // the schema is not present in schemaInfoStore
        doReturn(Optional.empty()).when(cosmosStore).findItem(anyString(), any(), any(), eq(dataPartitionId + ":" + schemaId), anyString(), any());
        doReturn(getFlattenedSchemaInfo()).when(schemaInfoDoc).getFlattenedSchemaInfo();

        assertNotNull(schemaInfoStore.createSchemaInfo(getMockSchemaObject_Published()));
    }

    @Test
    public void testCreateSchemaInfo_WithSupersededBy()
            throws NotFoundException, ApplicationException, BadRequestException {
        doReturn(Optional.empty()).when(cosmosStore).findItem(anyString(), any(), any(), eq(dataPartitionId + ":" + schemaId), anyString(), any());
        Optional<SchemaInfoDoc> cosmosItem = Optional.of(schemaInfoDoc);

        doReturn(cosmosItem).when(cosmosStore).findItem(any(), any(), any(), eq(dataPartitionId + ":" + supersedingSchemaId), any(), any());
        doReturn(getFlattenedSchemaInfo_SupersededBy()).when(schemaInfoDoc).getFlattenedSchemaInfo();

        assertNotNull(schemaInfoStore.createSchemaInfo(getMockSchemaObject_SupersededBy()));
    }

    @Test
    public void testCreateSchemaInfo_BadRequestException()
            throws NotFoundException, ApplicationException, BadRequestException {

        Optional<SchemaInfoDoc> cosmosItem = Optional.of(schemaInfoDoc);
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), eq(dataPartitionId + ":" + schemaId), anyString(), any());

        try {
            schemaInfoStore.createSchemaInfo(getMockSchemaObject_Published());
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals(SchemaConstants.SCHEMA_ID_EXISTS, e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateSchemaInfo_ApplicationException()
            throws NotFoundException, ApplicationException, BadRequestException {

        doReturn(Optional.empty()).when(cosmosStore).findItem(anyString(), any(), any(), eq(dataPartitionId + ":" + schemaId), anyString(), any());

        doThrow(AppException.class).when(cosmosStore).upsertItem(anyString(), any(), any(), any());

        try {
            schemaInfoStore.createSchemaInfo(getMockSchemaObject_Published());
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT, e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testIsUnique_True() throws ApplicationException {

        doReturn(Optional.empty()).when(cosmosStore).findItem(anyString(), any(), any(), anyString(), anyString(), any());
        assertTrue(schemaInfoStore.isUnique(schemaId, dataPartitionId));
    }

    @Test
    public void testIsUnique_False() throws ApplicationException {
        Optional<SchemaInfoDoc> cosmosItem = Optional.of(schemaInfoDoc);
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), anyString(), anyString(), any());
        assertFalse(schemaInfoStore.isUnique(schemaId, dataPartitionId));
    }

    @Test
    public void testIsUnique_False_CommomTenant() throws ApplicationException {
        TenantInfo tenant1 = new TenantInfo();
        tenant1.setName(commonTenantId);
        tenant1.setDataPartitionId(commonTenantId);
        TenantInfo tenant2 = new TenantInfo();
        tenant2.setName(dataPartitionId);
        tenant2.setDataPartitionId(dataPartitionId);
        Collection<TenantInfo> tenants = Lists.newArrayList(tenant1, tenant2);
        when(this.tenantFactory.listTenantInfo()).thenReturn(tenants);
        Optional<SchemaInfoDoc> cosmosItem = Optional.of(schemaInfoDoc);
        doReturn(cosmosItem).when(cosmosStore).findItem(anyString(), any(), any(), eq(dataPartitionId + ":" + schemaId), anyString(), any());
        assertFalse(schemaInfoStore.isUnique(schemaId, commonTenantId));
    }

    @Test
    public void testUpdateSchemaInfo() throws NotFoundException, ApplicationException, BadRequestException {
        Optional<SchemaInfoDoc> cosmosItem = Optional.of(schemaInfoDoc);
        doReturn(cosmosItem).when(cosmosStore).findItem(any(), any(), any(), eq(dataPartitionId + ":" + supersedingSchemaId), any(), any());

        doReturn(getFlattenedSchemaInfo()).when(schemaInfoDoc).getFlattenedSchemaInfo();
        assertNotNull(schemaInfoStore.updateSchemaInfo(getMockSchemaObject_Published()));
    }

    @Test
    public void testUpdateSchemaInfo_SupersededBy()
            throws NotFoundException, ApplicationException, BadRequestException {
        Optional<SchemaInfoDoc> cosmosItem = Optional.of(getMockSchemaInfoDoc());
        doReturn(cosmosItem).when(cosmosStore).findItem(any(), any(), any(), eq(dataPartitionId + ":" + schemaId), any(), any());
        doReturn(Optional.of(schemaInfoDoc)).when(cosmosStore).findItem(any(), any(), any(), eq(dataPartitionId + ":" + supersedingSchemaId), any(), any());
        doReturn(getFlattenedSchemaInfo_SupersededBy()).when(schemaInfoDoc).getFlattenedSchemaInfo();
        assertNotNull(schemaInfoStore.updateSchemaInfo(getMockSchemaObject_Published()));
    }

    @Test
    public void testUpdateSchemaInfo_SupersededByException()
            throws NotFoundException, ApplicationException, BadRequestException {
        doReturn(Optional.empty()).when(cosmosStore).findItem(any(), any(), any(), eq(dataPartitionId + ":" + supersedingSchemaId), any(), any());
        doReturn(Optional.of(schemaInfoDoc)).when(cosmosStore).findItem(any(), any(), any(), eq(dataPartitionId + ":" + schemaId), any(), any());
        doReturn(getFlattenedSchemaInfo_SupersededBy()).when(schemaInfoDoc).getFlattenedSchemaInfo();
        try {
            schemaInfoStore.updateSchemaInfo(getMockSchemaObject_SupersededBy());
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Invalid SuperSededBy id", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testUpdateInfo_SupersededByWithoutIdException()
            throws NotFoundException, ApplicationException, BadRequestException {

        doReturn(Optional.of(schemaInfoDoc)).when(cosmosStore).findItem(any(), any(), any(), eq(dataPartitionId + ":" + schemaId), any(), any());
        doReturn(getFlattenedSchemaInfo_SupersededBy()).when(schemaInfoDoc).getFlattenedSchemaInfo();

        SchemaRequest schemaRequest = getMockSchemaObject_SuperSededByWithoutId();
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
        schemaInfoStore.updateSchemaInfo(schemaRequest);
    }

    @Test
    public void testUpdateSchemaInfo_ApplicationException()
            throws NotFoundException, ApplicationException, BadRequestException {
        doReturn(Optional.of(schemaInfoDoc)).when(cosmosStore).findItem(any(), any(), any(), anyString(), any(), any());
        doThrow(AppException.class).when(cosmosStore).upsertItem(anyString(), any(), any(), any());

        try {
            schemaInfoStore.updateSchemaInfo(getMockSchemaObject_Published());
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals("Schema creation failed due to invalid object", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testGetSchemaInfoList_withoutqueryparam()
            throws NotFoundException, ApplicationException, BadRequestException {
        List<SchemaInfoDoc> schemaInfoDocsList = new LinkedList<>();
        schemaInfoDocsList.add(getMockSchemaInfoDoc());
        doReturn(schemaInfoDocsList).when(cosmosStore).queryItems(anyString(), any(), any(), any(), any(), any());

        assertEquals(1,
                schemaInfoStore.getSchemaInfoList(QueryParams.builder().limit(100).offset(0).build(), dataPartitionId).size());
    }

    @Test
    public void testGetSchemaInfoList_withqueryparam()
            throws NotFoundException, ApplicationException, BadRequestException {
        List<SchemaInfoDoc> schemaInfoDocsList = new LinkedList<>();
        schemaInfoDocsList.add(getMockSchemaInfoDoc());
        doReturn(schemaInfoDocsList).when(cosmosStore).queryItems(anyString(), any(), any(), any(), any(), any());
        assertEquals(1,
                schemaInfoStore.getSchemaInfoList(QueryParams.builder().authority("test").source("test").entityType("test")
                        .schemaVersionMajor(1l).schemaVersionMinor(1l).scope("test").status("test").latestVersion(false)
                        .limit(100).offset(0).build(), "test").size());
    }

    @Test
    public void testGetSchemaInfoList_latestVersionTrue_NoSchemaMatchScenario()
            throws NotFoundException, ApplicationException, BadRequestException {
        List<SchemaInfoDoc> cosmosItem = new ArrayList<>();
        doReturn(cosmosItem).when(cosmosStore).queryItems(anyString(), any(), any(), any(), any(), any());
        assertEquals(0,
                schemaInfoStore
                        .getSchemaInfoList(QueryParams.builder().authority("test").source("test").entityType("test")
                                .scope("test").status("test").latestVersion(true).limit(100).offset(0).build(), "test")
                        .size());
    }

    @Test
    public void testCleanSchema_Success() throws ApplicationException {
        doReturn(Optional.of(schemaInfoDoc)).when(cosmosStore).findItem(any(), any(), any(), anyString(), any(), any());
        assertEquals(true, schemaInfoStore.cleanSchema(schemaId));
    }

    @Test
    public void testCleanSchema_Failure() throws ApplicationException {
        doReturn(Optional.empty()).when(cosmosStore).findItem(any(), any(), any(), anyString(), any(), any());
        assertEquals(false, schemaInfoStore.cleanSchema(schemaId));
    }

    private SchemaInfo getMockSchemaInfo() {
        return SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L).id(schemaId).build())
                .scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).createdBy("azure")
                .build();
    }

    private SchemaInfoDoc getMockSchemaInfoDoc() {
        String id = headers.getPartitionId() + ":" + schemaId;

        return new SchemaInfoDoc(id, headers.getPartitionId(), getFlattenedSchemaInfo());
    }

    private FlattenedSchemaInfo getFlattenedSchemaInfo() {
        return FlattenedSchemaInfo.builder()
                .id(schemaId)
                .supersededBy("")
                .dateCreated(new Date())
                .createdBy("azure")
                .authority("os")
                .source("wks")
                .entityType("well")
                .majorVersion(1L)
                .minorVersion(1L)
                .patchVersion(1L)
                .scope(SchemaScope.SHARED.toString())
                .status(SchemaStatus.PUBLISHED.toString())
                .build();
    }

    private FlattenedSchemaInfo getFlattenedSchemaInfo_SupersededBy() {
        return FlattenedSchemaInfo.builder()
                .id(schemaId)
                .supersededBy(supersedingSchemaId)
                .dateCreated(new Date())
                .createdBy("azure")
                .authority("os")
                .source("wks")
                .entityType("well")
                .majorVersion(1L)
                .minorVersion(1L)
                .patchVersion(1L)
                .scope(SchemaScope.SHARED.toString())
                .status(SchemaStatus.PUBLISHED.toString())
                .build();
    }

    private SchemaRequest getMockSchemaObject_Published() {
        return SchemaRequest.builder().schema("{}")
                .schemaInfo(getMockSchemaInfo())
                .build();
    }

    private SchemaRequest getMockSchemaObject_SupersededBy() {
        return SchemaRequest.builder().schema("{}")
                .schemaInfo(SchemaInfo.builder()
                        .schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
                                .id(schemaId).build())
                        .scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).createdBy("azure")
                        .supersededBy(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(2L)
                                .id(supersedingSchemaId).build())
                        .build())
                .build();

    }

    private SchemaRequest getMockSchemaObject_SuperSededByWithoutId() {
        return SchemaRequest.builder().schema("{}")
                .schemaInfo(SchemaInfo.builder()
                        .schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
                                .id(schemaId).build())
                        .scope(SchemaScope.SHARED).status(SchemaStatus.DEVELOPMENT).createdBy("ibm")
                        .supersededBy(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L).build())
                        .build())
                .build();

    }
}
