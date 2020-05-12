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
import org.opengroup.osdu.schema.impl.schemainfostore.GoogleEntityTypeStore;
import org.opengroup.osdu.schema.model.EntityType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

@RunWith(SpringJUnit4ClassRunner.class)
public class GoogleEntityTypeStoreTest {

    @InjectMocks
    GoogleEntityTypeStore googleEntityStore;

    @Mock
    KeyFactory keyFactory;

    @Mock
    Entity entity;

    @Mock
    Datastore dataStore;

    @Mock
    EntityType mockEntityType;

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
        System.out.println("testGet");
        String entityId = "testEntityId";
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.ENTITY_TYPE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey(entityId)).thenReturn(key);
        Mockito.when(dataStore.get(key)).thenReturn(entity);
        Mockito.when(entity.getKey()).thenReturn(key);
        assertNotNull(googleEntityStore.get(entityId));
    }

    @Test
    public void testGet_NotFoundException() {
        System.out.println("testGet_NotFoundException");
        String entityId = "";
        try {
            Mockito.when(headers.getPartitionId()).thenReturn("test");
            Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
            Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
            Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
            Mockito.when(keyFactory.setKind(SchemaConstants.ENTITY_TYPE)).thenReturn(keyFactory);
            Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
            Mockito.when(keyFactory.newKey(entityId)).thenReturn(key);
            Mockito.when(dataStore.get(key)).thenReturn(null);
            googleEntityStore.get(entityId);
            fail("Should not succeed");
        } catch (NotFoundException e) {
            assertEquals("bad input parameter", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreate() throws NotFoundException, ApplicationException, BadRequestException {
        System.out.println("testCreate");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.ENTITY_TYPE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("wellbore")).thenReturn(key);
        Entity entity = Entity.newBuilder(key).set(SchemaConstants.DATE_CREATED, Timestamp.now()).build();
        Mockito.when(dataStore.add(entity)).thenReturn(entity);
        Mockito.when(mockEntityType.getEntityTypeId()).thenReturn("wellbore");
        assertNotNull(googleEntityStore.create(mockEntityType));
    }

    @Test
    public void testCreate_BadRequestException() throws NotFoundException, ApplicationException, BadRequestException {
        System.out.println("testCreate_BadRequestException");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(mockEntityType.getEntityTypeId()).thenReturn("wks");
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.ENTITYTYPE_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("wks")).thenReturn(key);
        Mockito.when(dataStore.add(Mockito.any(Entity.class)))
                .thenThrow(new DatastoreException(400, SchemaConstants.ALREADY_EXISTS, SchemaConstants.ALREADY_EXISTS));

        try {
            googleEntityStore.create(mockEntityType);
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("EntityType already registered with Id: wks", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreate_ApplicationException() throws NotFoundException, ApplicationException, BadRequestException {
        System.out.println("testCreate_ApplicationException");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.ENTITYTYPE_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("wks")).thenReturn(key);
        Mockito.when(dataStore.add(Mockito.any(Entity.class))).thenThrow(DatastoreException.class);
        Mockito.when(mockEntityType.getEntityTypeId()).thenReturn("wks");
        try {
            googleEntityStore.create(mockEntityType);
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals("Invalid input, object invalid", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

}
