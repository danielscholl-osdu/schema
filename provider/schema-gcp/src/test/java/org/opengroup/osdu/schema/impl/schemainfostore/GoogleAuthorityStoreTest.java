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
import org.opengroup.osdu.schema.impl.schemainfostore.GoogleAuthorityStore;
import org.opengroup.osdu.schema.model.Authority;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

@RunWith(SpringJUnit4ClassRunner.class)
public class GoogleAuthorityStoreTest {

    @InjectMocks
    GoogleAuthorityStore mockGoogleAuthorityStore;

    @Mock
    KeyFactory keyFactory;

    @Mock
    Entity mockEntity;

    @Mock
    Datastore dataStore;

    @Mock
    DatastoreFactory dataStoreFactory;

    @Mock
    Authority mockAuthority;

    @Mock
    Key key;

    @Mock
    DpsHeaders headers;

    @Mock
    TenantFactory tenantFactory;

    @Mock
    TenantInfo tenantInfo;

    @Test
    public void testGetAuthority() throws NotFoundException, ApplicationException {
        String authorityId = "testAuthorityId";
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.AUTHORITY_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey(authorityId)).thenReturn(key);
        Mockito.when(dataStore.get(key)).thenReturn(mockEntity);
        Mockito.when(mockEntity.getKey()).thenReturn(key);
        assertNotNull(mockGoogleAuthorityStore.get(authorityId));
    }

    @Test
    public void testGetAuthority_NotFoundException() {
        String authorityId = "";
        try {
            Mockito.when(headers.getPartitionId()).thenReturn("test");
            Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
            Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
            Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
            Mockito.when(keyFactory.setKind(SchemaConstants.AUTHORITY_KIND)).thenReturn(keyFactory);
            Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
            Mockito.when(keyFactory.newKey(authorityId)).thenReturn(key);
            Mockito.when(dataStore.get(key)).thenReturn(null);
            mockGoogleAuthorityStore.get(authorityId);
            fail("Should not succeed");
        } catch (NotFoundException e) {
            assertEquals("bad input parameter", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateAuthority() throws NotFoundException, ApplicationException, BadRequestException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.AUTHORITY_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os")).thenReturn(key);
        Mockito.when(dataStore.get(key)).thenReturn(mockEntity);
        Entity entity = Entity.newBuilder(key).set(SchemaConstants.DATE_CREATED, Timestamp.now()).build();
        Mockito.when(dataStore.add(entity)).thenReturn(entity);
        Mockito.when(mockAuthority.getAuthorityId()).thenReturn("os");
        assertNotNull(mockGoogleAuthorityStore.create(mockAuthority));
    }

    @Test
    public void testCreateAuthority_BadRequestException()
            throws NotFoundException, ApplicationException, BadRequestException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        mockGoogleAuthorityStore = Mockito.spy(mockGoogleAuthorityStore);
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.AUTHORITY_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os")).thenReturn(key);
        Mockito.when(dataStore.add(Mockito.any(Entity.class)))
                .thenThrow(new DatastoreException(400, SchemaConstants.ALREADY_EXISTS, SchemaConstants.ALREADY_EXISTS));
        Mockito.when(mockAuthority.getAuthorityId()).thenReturn("os");
        try {
            mockGoogleAuthorityStore.create(mockAuthority);
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Authority already registered with Id: os", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateAuthority_ApplicationException()
            throws NotFoundException, ApplicationException, BadRequestException {
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        mockGoogleAuthorityStore = Mockito.spy(mockGoogleAuthorityStore);
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);
        Mockito.when(dataStoreFactory.getDatastore(tenantInfo)).thenReturn(dataStore);
        Mockito.when(dataStore.newKeyFactory()).thenReturn(keyFactory);
        Mockito.when(keyFactory.setKind(SchemaConstants.AUTHORITY_KIND)).thenReturn(keyFactory);
        Mockito.when(keyFactory.setNamespace(SchemaConstants.NAMESPACE)).thenReturn(keyFactory);
        Mockito.when(keyFactory.newKey("os")).thenReturn(key);
        Mockito.when(dataStore.add(Mockito.any(Entity.class))).thenThrow(DatastoreException.class);
        Mockito.when(mockAuthority.getAuthorityId()).thenReturn("os");
        try {
            mockGoogleAuthorityStore.create(mockAuthority);
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals(SchemaConstants.INVALID_INPUT, e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

}
