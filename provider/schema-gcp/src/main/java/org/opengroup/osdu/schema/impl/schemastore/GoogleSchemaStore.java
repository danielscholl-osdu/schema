package org.opengroup.osdu.schema.impl.schemastore;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.gcp.multitenancy.TenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.credentials.StorageFactory;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

/**
 * Repository class to to register resolved Schema in Google storage.
 *
 *
 */
@Repository
public class GoogleSchemaStore implements ISchemaStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private StorageFactory storageFactory;

    @Autowired
    TenantFactory tenantFactory;

    /**
     * Method to get schema from google Storage given Tenant ProjectInfo
     *
     * @param dataPartitionId
     * @param filePath
     * @throws NotFoundException
     * @return schema object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    @Override
    public String getSchema(String dataPartitionId, String filePath) throws ApplicationException, NotFoundException {
        filePath = filePath + SchemaConstants.JSON_EXTENSION;
        String bucketname = getSchemaBucketName(dataPartitionId);
        Storage storage = storageFactory.getStorage(tenantFactory.getTenantInfo(dataPartitionId));
        Blob blob = storage.get(bucketname, filePath);
        if (blob != null)
            return new String(blob.getContent());
        throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
    }

    /**
     * Method to write schema to google Storage given Tenant ProjectInfo
     *
     * @param dataPartitionId
     * @param filePath
     * @param content
     * @return schema object
     * @throws ApplicationException
     */

    @Override
    public String createSchema(String filePath, String content) throws ApplicationException {
        String dataPartitionId = headers.getPartitionId();
        filePath = filePath + SchemaConstants.JSON_EXTENSION;
        String bucketname = getSchemaBucketName(dataPartitionId);
        BlobId blobId = BlobId.of(bucketname, filePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        Storage storage = storageFactory.getStorage(tenantFactory.getTenantInfo(dataPartitionId));
        try {
            Blob blob = storage.create(blobInfo, content.getBytes());
            return blob.getName();
        } catch (StorageException ex) {
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
    }

    private String getSchemaBucketName(String dataPartitionId) {
        return tenantFactory.getTenantInfo(dataPartitionId).getProjectId() + SchemaConstants.SCHEMA_BUCKET_EXTENSION;
    }

    @Override
    public boolean cleanSchemaProject(String schemaId) throws ApplicationException {
        String dataPartitionId = headers.getPartitionId();
        String fileName = schemaId + SchemaConstants.JSON_EXTENSION;
        String bucketname = getSchemaBucketName(dataPartitionId);
        BlobId blobId = BlobId.of(bucketname, fileName);
        return storageFactory.getStorage(tenantFactory.getTenantInfo(dataPartitionId)).delete(blobId);
    }

}
