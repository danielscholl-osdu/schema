package org.opengroup.osdu.schema.impl.schemainfostore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.gcp.multitenancy.DatastoreFactory;
import org.opengroup.osdu.core.gcp.multitenancy.TenantFactory;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.ProjectionEntity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.TimestampValue;

@RunWith(SpringJUnit4ClassRunner.class)
public class GoogleSchemaInfoStoreTest {

    @InjectMocks
    GoogleSchemaInfoStore schemaInfoStore;

    @Mock
    KeyFactory keyFactory;

    @Mock
    ProjectionEntity projectionEntity;

    @Mock
    Entity entity;

    @Mock
    Datastore dataStore;

    @Mock
    SchemaRequest schemaRequest;

    @Mock
    Key key;

    @Mock
    Blob blob;

    @Mock
    Query<Entity> query;

    @Mock
    QueryResults<Object> queryResult;

    @Mock
    DatastoreFactory dataStoreFactory;

    @Mock
    DpsHeaders headers;

    @Mock
    Query<Key> keyQuery;

    @Mock
    QueryResults<Key> keys;

    @Mock
    TenantInfo tenantInfo;

    @Mock
    TenantInfo tenantInfoCommon;

    @Mock
    TenantFactory tenantFactory;

    @Mock
    JaxRsDpsLog log;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testGetLatestMinorVersion_ReturnNull() throws NotFoundException, ApplicationException {
        when(headers.getPartitionId()).thenReturn("tenant");
        Mockito.when(tenantFactory.getTenantInfo("tenant")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.run(Mockito.any())).thenReturn(queryResult);
        Mockito.when(entity.getBlob(SchemaConstants.SCHEMA)).thenReturn(blob);
        Mockito.when(blob.toByteArray()).thenReturn("{}".getBytes());
        assertEquals("", schemaInfoStore.getLatestMinorVerSchema(getMockSchemaInfo()));
    }

    @Test
    public void testGetSchemaInfo_NotEmptyEntity() throws NotFoundException, ApplicationException {
        String schemaId = "schemaId";
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey(schemaId)).thenReturn(key);
        Mockito.when(dataStore.get(key)).thenReturn(getMockEntityObject());
        SchemaInfo schemaInfo = schemaInfoStore.getSchemaInfo(schemaId);
        assertEquals(SchemaStatus.PUBLISHED, schemaInfo.getStatus());
    }

    @Test
    public void testGetSchemaInfo_EmptyEntity() throws NotFoundException, ApplicationException {

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(SchemaConstants.SCHEMA_NOT_PRESENT);
        String schemaId = "schemaId";
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey(schemaId)).thenReturn(key);
        Mockito.when(dataStore.get(key)).thenReturn(null);
        schemaInfoStore.getSchemaInfo(schemaId);
    }

    @Test
    public void testCreateSchemaInfo_Positive() throws ApplicationException, BadRequestException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os:wks:well.1.1.1")).thenReturn(key);
        Mockito.when(headers.getUserEmail()).thenReturn("hmadhani@delfi.com");
        Mockito.when(dataStore.add(entity)).thenReturn(entity);
        assertNotNull(schemaInfoStore.createSchemaInfo(getMockSchemaObject_Published()));
    }

    @Test
    public void testIsUnique_True() throws ApplicationException {
        Key storageKey = mock(Key.class);
        KeyFactory storageKeyFactory = mock(KeyFactory.class);
        String schemaId = "schemaId";
        String tenantId = "tenant";
        schemaInfoStore.setCommonAccountId(tenantId);
        Mockito.when(tenantFactory.getTenantInfo("tenant")).thenReturn(tenantInfo);
        Mockito.when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfoCommon);
        Mockito.when(tenantInfo.getName()).thenReturn("tenant");
        Mockito.when(tenantInfoCommon.getName()).thenReturn("common");
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo.getName(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfoCommon.getName(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        when(keyFactory.newKey(schemaId)).thenReturn(key);
        when(storageKeyFactory.newKey(schemaId)).thenReturn(storageKey);
        Query<Key> any = Mockito.any(Query.class);
        when(dataStore.run(any)).thenReturn(keys);
        when(keys.hasNext()).thenReturn(false);
        assertTrue(schemaInfoStore.isUnique(schemaId, tenantId));
    }

    @Test
    public void testIsUnique_False() throws ApplicationException {
        Key storageKey = mock(Key.class);
        KeyFactory storageKeyFactory = mock(KeyFactory.class);
        String schemaId = "schemaId";
        String tenantId = "tenant";
        Mockito.when(tenantFactory.getTenantInfo("tenant")).thenReturn(tenantInfo);
        Mockito.when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfoCommon);
        Mockito.when(tenantInfo.getName()).thenReturn("test");
        Mockito.when(tenantInfoCommon.getName()).thenReturn("common");
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfoCommon.getName(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        when(keyFactory.newKey(schemaId)).thenReturn(key);
        when(storageKeyFactory.newKey(schemaId)).thenReturn(storageKey);
        Query<Key> any = Mockito.any(Query.class);
        when(dataStore.run(any)).thenReturn(keys);
        when(keys.hasNext()).thenReturn(true);
        assertFalse(schemaInfoStore.isUnique(schemaId, tenantId));
    }

    @Test
    public void testIsUnique_False_CommomTenant() throws ApplicationException {
        Key storageKey = mock(Key.class);
        KeyFactory storageKeyFactory = mock(KeyFactory.class);
        String schemaId = "schemaId";
        String tenantId = "common";
        Mockito.when(tenantFactory.getTenantInfo("tenant")).thenReturn(tenantInfo);
        Mockito.when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfoCommon);
        Mockito.when(tenantInfo.getName()).thenReturn("test");
        Mockito.when(tenantInfoCommon.getName()).thenReturn("common");
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfoCommon.getName(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        when(keyFactory.newKey(schemaId)).thenReturn(key);
        when(storageKeyFactory.newKey(schemaId)).thenReturn(storageKey);
        Query<Key> any = Mockito.any(Query.class);
        when(dataStore.run(any)).thenReturn(keys);
        when(keys.hasNext()).thenReturn(true);
        assertFalse(schemaInfoStore.isUnique(schemaId, tenantId));
    }

    @Test
    public void testUpdateSchemaInfo() throws NotFoundException, ApplicationException, BadRequestException {
        String tenantId = "test";
        Mockito.when(headers.getPartitionId()).thenReturn(tenantId);
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os:wks:well.1.1.1")).thenReturn(key);
        Mockito.when(headers.getUserEmail()).thenReturn("hmadhani@delfi.com");
        Mockito.when(dataStore.put(entity)).thenReturn(entity);
        assertNotNull(schemaInfoStore.updateSchemaInfo(getMockSchemaObject_Published()));
    }

    @Test
    public void testCreateSchemaInfo_WithSupersededBy()
            throws NotFoundException, ApplicationException, BadRequestException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os:wks:well.1.1.1")).thenReturn(key);
        Key mockSupsededKey = Mockito.mock(Key.class);
        Mockito.when(keyFactory.newKey("os:wks:well.1.2.1")).thenReturn(mockSupsededKey);
        Mockito.when(headers.getUserEmail()).thenReturn("hmadhani@delfi.com");
        Mockito.when(dataStore.add(entity)).thenReturn(entity);
        Mockito.when(dataStore.get(mockSupsededKey)).thenReturn(entity);
        Mockito.when(entity.getKey()).thenReturn(mockSupsededKey);
        assertNotNull(schemaInfoStore.createSchemaInfo(getMockSchemaObject_SuperSededBy()));
    }

    @Test
    public void testUpdateSchemaInfo_SupersededByException()
            throws NotFoundException, ApplicationException, BadRequestException {
        try {
            Mockito.when(headers.getPartitionId()).thenReturn("test");
            Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
            Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                    .thenReturn(dataStore);
            Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
            Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
            Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
            Mockito.when(keyFactory.newKey("os:wks:well.1.1.1")).thenReturn(key);
            Key mockSupsededKey = Mockito.mock(Key.class);
            Mockito.when(keyFactory.newKey("os:wks:well.1.2.1")).thenReturn(mockSupsededKey);
            Mockito.when(dataStore.add(entity)).thenReturn(entity);
            Mockito.when(dataStore.get(mockSupsededKey)).thenReturn(null);
            Mockito.when(entity.getKey()).thenReturn(mockSupsededKey);
            schemaInfoStore.updateSchemaInfo(getMockSchemaObject_SuperSededBy());
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
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os:wks:well.1.1.1")).thenReturn(key);
        SchemaRequest schemaRequest = getMockSchemaObject_SuperSededByWithoutId();
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
        schemaInfoStore.updateSchemaInfo(schemaRequest);
    }

    @Test
    public void testCreateSchemaInfo_BadRequestException()
            throws NotFoundException, ApplicationException, BadRequestException {

        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(headers.getUserEmail()).thenReturn("dummy-user");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os:wks:well.1.1.1")).thenReturn(key);
        Mockito.when(dataStore.add(Mockito.any(Entity.class)))
                .thenThrow(new DatastoreException(400, SchemaConstants.ALREADY_EXISTS, SchemaConstants.ALREADY_EXISTS));
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

        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(headers.getUserEmail()).thenReturn("dummy-user");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os:wks:well.1.1.1")).thenReturn(key);
        Mockito.when(dataStore.add(Mockito.any(Entity.class))).thenThrow(DatastoreException.class);
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
    public void testUpdateSchemaInfo_ApplicationException()
            throws NotFoundException, ApplicationException, BadRequestException {

        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(headers.getUserEmail()).thenReturn("dummy-user");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os:wks:well.1.1.1")).thenReturn(key);
        Mockito.when(dataStore.put(Mockito.any(Entity.class))).thenThrow(DatastoreException.class);
        try {
            schemaInfoStore.updateSchemaInfo(getMockSchemaObject_Published());
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals("Invalid object, updation failed", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testGetLatestMinorVersion_Entity() throws NotFoundException, ApplicationException {
        when(headers.getPartitionId()).thenReturn("tenant");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(headers.getUserEmail()).thenReturn("hmadhani@delfi.com");
        entity = Entity.newBuilder(getMockEntityObject()).set(SchemaConstants.SCHEMA, blob).build();
        Mockito.when(dataStore.add(entity)).thenReturn(getMockEntityObject());
        Mockito.when(dataStore.run(Mockito.any())).thenReturn(queryResult);
        Mockito.when(queryResult.hasNext()).thenReturn(true, false);
        Mockito.when(queryResult.next()).thenReturn(entity);
        Mockito.when(blob.toByteArray()).thenReturn("{}".getBytes());
        assertEquals("{}", schemaInfoStore.getLatestMinorVerSchema(getMockSchemaInfo()));
    }

    @Test
    public void testGetSchemaInfoList_withoutqueryparam()
            throws NotFoundException, ApplicationException, BadRequestException {

        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(tenantInfo.getName()).thenReturn("test");
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(dataStore.run(Mockito.any())).thenReturn(queryResult);
        Mockito.when(queryResult.hasNext()).thenReturn(true, false);
        Mockito.when(queryResult.next()).thenReturn(getMockEntityObject());
        assertEquals(1,
                schemaInfoStore.getSchemaInfoList(QueryParams.builder().limit(100).offset(0).build(), "test").size());
    }

    @Test
    public void testGetSchemaInfoList_withqueryparam()
            throws NotFoundException, ApplicationException, BadRequestException {

        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(tenantInfo.getName()).thenReturn("test");
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(dataStore.run(Mockito.any())).thenReturn(queryResult);
        Mockito.when(queryResult.hasNext()).thenReturn(true, false);
        Mockito.when(queryResult.next()).thenReturn(getMockEntityObject());
        assertEquals(1,
                schemaInfoStore.getSchemaInfoList(QueryParams.builder().authority("test").source("test")
                        .entityType("test").schemaVersionMajor(1l).schemaVersionMinor(1l).scope("test").status("test")
                        .latestVersion(false).limit(100).offset(0).build(), "test").size());
    }

    @Test
    public void testCleanSchema_Success() throws ApplicationException {
        String schemaId = "schemaId";
        String dataPartitionId = "tenant1";
        when(headers.getPartitionId()).thenReturn(dataPartitionId);
        Mockito.when(tenantFactory.getTenantInfo(dataPartitionId)).thenReturn(tenantInfo);
        Mockito.when(tenantInfo.getName()).thenReturn("test");
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Key key = mock(Key.class);
        Mockito.when(keyFactory.newKey(schemaId)).thenReturn(key);
        assertEquals(true, schemaInfoStore.cleanSchema(schemaId));
    }

    @Test
    public void testCleanSchema_Failure() throws ApplicationException {
        String schemaId = "schemaId";
        String dataPartitionId = "tenant1";
        when(headers.getPartitionId()).thenReturn(dataPartitionId);
        Mockito.when(tenantFactory.getTenantInfo(dataPartitionId)).thenReturn(tenantInfo);
        Mockito.when(tenantInfo.getName()).thenReturn("tenant1");
        Mockito.when(dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE))
                .thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SCHEMA_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Key key = mock(Key.class);
        Mockito.when(keyFactory.newKey(schemaId)).thenReturn(key);
        doThrow(DatastoreException.class).when(dataStore).delete(key);
        assertEquals(false, schemaInfoStore.cleanSchema(schemaId));
    }

    private SchemaRequest getMockSchemaObject_Published() {
        return SchemaRequest.builder().schema("{}")
                .schemaInfo(SchemaInfo.builder()
                        .schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
                                .id("os:wks:well.1.1.1").build())
                        .scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).createdBy("subham").build())
                .build();

    }

    private SchemaInfo getMockSchemaInfo() {
        return SchemaInfo.builder()
                .schemaIdentity(
                        SchemaIdentity.builder().authority("os").source("wks").entityType("well").schemaVersionMajor(1L)
                                .schemaVersionMinor(1L).schemaVersionPatch(1L).id("os:wks:well.1.1.1").build())
                .scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).createdBy("subham")

                .build();

    }

    private SchemaRequest getMockSchemaObject_SuperSededBy() {
        return SchemaRequest.builder().schema("{}")
                .schemaInfo(SchemaInfo.builder()
                        .schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
                                .id("os:wks:well.1.1.1").build())
                        .scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).createdBy("subham")
                        .supersededBy(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(2L)
                                .id("os:wks:well.1.2.1").build())
                        .build())
                .build();

    }

    private SchemaRequest getMockSchemaObject_SuperSededByWithoutId() {
        return SchemaRequest.builder().schema("{}")
                .schemaInfo(SchemaInfo.builder()
                        .schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
                                .id("os:wks:well.1.1.1").build())
                        .scope(SchemaScope.SHARED).status(SchemaStatus.DEVELOPMENT).createdBy("subham")
                        .supersededBy(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L).build())
                        .build())
                .build();

    }

    private Entity getMockEntityObject() {
        return Entity.newBuilder(key).set("createdBy", "subham")
                .set("dateCreated", TimestampValue.newBuilder(Timestamp.MAX_VALUE).build()).set("scope", "INTERNAL")
                .set("status", "PUBLISHED").setKey(key).set("authority", "os1").set("source", "techlog1")
                .set("entityType", "wellbore1").set("majorVersion", 2).set("minorVersion", 1024).set("patchVersion", 2)
                .build();
    }

}
