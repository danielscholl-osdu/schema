package org.opengroup.osdu.schema.impl.schemainfostore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.gcp.multitenancy.TenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.credentials.DatastoreFactory;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.impl.schemainfostore.GoogleSourceStore;
import org.opengroup.osdu.schema.model.Source;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

@RunWith(SpringJUnit4ClassRunner.class)
public class GoogleSourceStoreTest {

    @InjectMocks
    GoogleSourceStore sourceStore;

    @Mock
    KeyFactory keyFactory;

    @Mock
    Entity entity;

    @Mock
    Datastore dataStore;

    @Mock
    Source mockSource;

    @Mock
    Key key;

    @Mock
    DpsHeaders headers;

    @Mock
    DatastoreFactory dataStoreFactory;

    @Mock
    TenantInfo tenantInfo;

    @Mock
    TenantFactory tenantFactory;

    @Test
    public void testGet() throws NotFoundException, ApplicationException {
        String sourceId = "sourceId";
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SOURCE_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey(sourceId)).thenReturn(key);
        Mockito.when(dataStore.get(key)).thenReturn(entity);
        Mockito.when(entity.getKey()).thenReturn(key);
        assertNotNull(sourceStore.get(sourceId));
    }

    @Test
    public void testGet_NotFoundException() {
        String sourceId = "";
        try {
            Mockito.when(headers.getPartitionId()).thenReturn("test");
            Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
            Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
            Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
            Mockito.when(keyFactory.setKind(SchemaConstants.SOURCE_KIND)).thenReturn(keyFactory);
            Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
            Mockito.when(keyFactory.newKey(sourceId)).thenReturn(key);
            Mockito.when(dataStore.get(key)).thenReturn(null);
            sourceStore.get(sourceId);
            fail("Should not succeed");
        } catch (NotFoundException e) {
            assertEquals("bad input parameter", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreate() throws NotFoundException, ApplicationException, BadRequestException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SOURCE_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("wks")).thenReturn(key);
        Entity entity = Entity.newBuilder(key).set(SchemaConstants.DATE_CREATED, Timestamp.now()).build();
        Mockito.when(dataStore.add(entity)).thenReturn(entity);
        Mockito.when(mockSource.getSourceId()).thenReturn("wks");
        assertNotNull(sourceStore.create(mockSource));
    }

    @Test
    public void testCreate_BadRequestException() throws NotFoundException, ApplicationException, BadRequestException {
        sourceStore = Mockito.spy(sourceStore);
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SOURCE_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("wks")).thenReturn(key);
        Mockito.when(dataStore.add(Mockito.any(Entity.class)))
                .thenThrow(new DatastoreException(400, SchemaConstants.ALREADY_EXISTS, SchemaConstants.ALREADY_EXISTS));
        Mockito.when(mockSource.getSourceId()).thenReturn("wks");
        try {
            sourceStore.create(mockSource);
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Source already registered with Id: wks", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreate_ApplicationException() throws NotFoundException, ApplicationException, BadRequestException {
        sourceStore = Mockito.spy(sourceStore);
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.SOURCE_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("wks")).thenReturn(key);
        Mockito.when(dataStore.add(Mockito.any(Entity.class))).thenThrow(DatastoreException.class);
        Mockito.when(mockSource.getSourceId()).thenReturn("wks");
        try {
            sourceStore.create(mockSource);
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals(SchemaConstants.INVALID_INPUT, e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

}
