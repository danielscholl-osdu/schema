package org.opengroup.osdu.schema.impl.schemastore;

import org.junit.Assert;
import org.junit.Before;
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
import org.opengroup.osdu.core.gcp.multitenancy.GcsMultiTenantAccess;
import org.opengroup.osdu.core.gcp.multitenancy.TenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

@RunWith(SpringJUnit4ClassRunner.class)
public class GoogleSchemaStoreTest {

    @InjectMocks
    GoogleSchemaStore schemaStore;

    @Mock
    private GcsMultiTenantAccess storageFactory;

    @Mock
    Storage storage;

    @Mock
    Blob blob;

    @Mock
    DpsHeaders headers;

    @Mock
    TenantFactory tenantFactory;

    @Mock
    TenantInfo TenantInfo;

    @Mock
    JaxRsDpsLog log;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final String BUCKET = "test-schema";
    private static final String dataPartitionId = "dataPartitionId";
    private static final String FILE_PATH = "/test-folder/test-file";
    private static final String CONTENT = "Hello World";

    @Before
    public void setUp() {
    	 ReflectionTestUtils.setField(schemaStore, "sharedTenant", "common");
    }
    
    @Test
    public void testCreateSchema() throws ApplicationException {

        Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
        String filepath = FILE_PATH + SchemaConstants.JSON_EXTENSION;
        BlobId blobId = Blob.newBuilder(BUCKET, filepath).build().getBlobId();
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        Mockito.when(tenantFactory.getTenantInfo(dataPartitionId)).thenReturn(TenantInfo);
        Mockito.when(TenantInfo.getProjectId()).thenReturn("test");
        Mockito.when(storageFactory.get(tenantFactory.getTenantInfo(dataPartitionId))).thenReturn(storage);
        Mockito.when(storage.create(blobInfo, CONTENT.getBytes())).thenReturn(blob);
        Mockito.when(blob.getName()).thenReturn(BUCKET + filepath);

        String blobName = schemaStore.createSchema(FILE_PATH, CONTENT);

        Assert.assertEquals((BUCKET + filepath), blobName);
    }

    @Test
    public void testCreateSchema_Failure() throws ApplicationException {
        expectedException.expect(ApplicationException.class);
        expectedException.expectMessage(SchemaConstants.INTERNAL_SERVER_ERROR);
        Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
        String filepath = FILE_PATH + SchemaConstants.JSON_EXTENSION;
        BlobId blobId = Blob.newBuilder(BUCKET, filepath).build().getBlobId();
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        Mockito.when(tenantFactory.getTenantInfo(dataPartitionId)).thenReturn(TenantInfo);
        Mockito.when(TenantInfo.getProjectId()).thenReturn("test");
        Mockito.when(storageFactory.get(tenantFactory.getTenantInfo(dataPartitionId))).thenReturn(storage);
        Mockito.when(storage.create(blobInfo, CONTENT.getBytes())).thenThrow(StorageException.class);
        schemaStore.createSchema(FILE_PATH, CONTENT);
    }

    @Test
    public void testGetSchema() throws ApplicationException, NotFoundException {
        String filePath = FILE_PATH + SchemaConstants.JSON_EXTENSION;
        Mockito.when(tenantFactory.getTenantInfo(dataPartitionId)).thenReturn(TenantInfo);
        Mockito.when(TenantInfo.getProjectId()).thenReturn("test");
        Mockito.when(storageFactory.get(tenantFactory.getTenantInfo(dataPartitionId))).thenReturn(storage);
        Mockito.when(storage.get(BUCKET, filePath)).thenReturn(blob);
        Mockito.when(blob.getContent()).thenReturn(CONTENT.getBytes());
        Assert.assertEquals(CONTENT, schemaStore.getSchema(dataPartitionId, FILE_PATH));
    }

    @Test
    public void testGetSchema_NotFound() throws ApplicationException, NotFoundException {

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage(SchemaConstants.SCHEMA_NOT_PRESENT);
        String filePath = FILE_PATH + SchemaConstants.JSON_EXTENSION;
        Mockito.when(tenantFactory.getTenantInfo(dataPartitionId)).thenReturn(TenantInfo);
        Mockito.when(TenantInfo.getProjectId()).thenReturn("test");
        Mockito.when(storageFactory.get(tenantFactory.getTenantInfo(dataPartitionId))).thenReturn(storage);
        Mockito.when(storage.get(BUCKET, filePath)).thenReturn(null);
        schemaStore.getSchema(dataPartitionId, FILE_PATH);
    }

    @Test
    public void testDeleteSchema() throws ApplicationException {

        Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
        String filepath = FILE_PATH + SchemaConstants.JSON_EXTENSION;
        BlobId blobId = Blob.newBuilder(BUCKET, filepath).build().getBlobId();
        Mockito.when(tenantFactory.getTenantInfo(dataPartitionId)).thenReturn(TenantInfo);
        Mockito.when(TenantInfo.getProjectId()).thenReturn("test");
        Mockito.when(storageFactory.get(tenantFactory.getTenantInfo(dataPartitionId))).thenReturn(storage);
        Mockito.when(storage.delete(blobId)).thenReturn(true);

        Boolean result = schemaStore.cleanSchemaProject(FILE_PATH);

        Assert.assertEquals(true, result);
    }

}
