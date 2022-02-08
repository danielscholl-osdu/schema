/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.schema.impl.schemastore;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.obm.driver.Driver;
import org.opengroup.osdu.core.gcp.obm.driver.ObmDriverRuntimeException;
import org.opengroup.osdu.core.gcp.obm.model.Blob;
import org.opengroup.osdu.core.gcp.obm.persistence.ObmDestination;
import org.opengroup.osdu.core.gcp.osm.model.query.GetQuery;
import org.opengroup.osdu.core.gcp.osm.model.where.Where;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.impl.mapper.AbstractMapperRepository;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;

import static org.opengroup.osdu.core.gcp.obm.driver.S3CompatibleErrors.NO_SUCH_KEY;

/**
 * Repository class to register resolved Schema in Blob storage.
 *
 *
 */
@Repository
public class ObmSchemaStore extends AbstractMapperRepository<String, ObmDestination> implements ISchemaStore {

    private final ITenantFactory tenantFactory;

    private final Driver driver;

    @Autowired
    public ObmSchemaStore(DpsHeaders headers, DestinationProvider<ObmDestination> destinationProvider, JaxRsDpsLog log, ITenantFactory tenantFactory, Driver driver) {
        super(headers, destinationProvider, log);
        this.tenantFactory = tenantFactory;
        this.driver = driver;
    }

    /**
     * Method to get schema from Blob Storage given Tenant ProjectInfo
     *
     * @param dataPartitionId
     * @param filePath
     * @return schema object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    @Override
    public String getSchema(String dataPartitionId, String filePath) throws ApplicationException, NotFoundException {
        filePath = filePath + SchemaConstants.JSON_EXTENSION;
        String bucketName = getSchemaBucketName(dataPartitionId);

        byte[] blob = null;

        try {
            blob = driver.getBlobContent(bucketName, filePath, getDestination());
        } catch (ObmDriverRuntimeException | NullPointerException ex){
            if (ex instanceof NullPointerException
                    || NO_SUCH_KEY.equals(((ObmDriverRuntimeException)ex.getCause()).getError())){
                throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
            }
            else {
                throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
            }
        }

        if (blob != null) {
            return new String(blob, StandardCharsets.UTF_8);
        }
        throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
    }

    /**
     * Method to get System schema from Blob Storage
     * @param filePath
     * @return Schema object
     * @throws NotFoundException
     * @throws ApplicationException
     */
    @Override
    public String getSystemSchema(String filePath) throws NotFoundException, ApplicationException {
        return this.getSchema(sharedTenant, filePath);
    }

    /**
     * Method to write schema to Blob Storage given Tenant ProjectInfo
     *
     * @param filePath
     * @param content
     * @return schema object
     * @throws ApplicationException
     */

    @Override
    public String createSchema(String filePath, String content) throws ApplicationException {
        String dataPartitionId = headers.getPartitionId();
        filePath = filePath + SchemaConstants.JSON_EXTENSION;
        String bucketName = getSchemaBucketName(dataPartitionId);

        Blob blob = Blob.builder()
                .bucket(bucketName)
                .name(filePath)
                .build();

        try {
            Blob blobFromStorage = driver.createAndGetBlob(blob, content.getBytes(StandardCharsets.UTF_8), getDestination());
            log.info(SchemaConstants.SCHEMA_CREATED);
            return blobFromStorage.getName();
        } catch (ObmDriverRuntimeException ex) {
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Method to write System schema to Blob Storage
     * @param filePath
     * @param content
     * @return schema object
     * @throws ApplicationException
     */
    @Override
    public String createSystemSchema(String filePath, String content) throws ApplicationException {
        this.updateDataPartitionId();
        return this.createSchema(filePath, content);

    }

    @Override
    public boolean cleanSchemaProject(String schemaId) throws ApplicationException {
        String dataPartitionId = headers.getPartitionId();
        String fileName = schemaId + SchemaConstants.JSON_EXTENSION;
        String bucketName = getSchemaBucketName(dataPartitionId);
        return driver.deleteBlob(bucketName, fileName, getDestination());


    }

    /**
     * Method to clean System schema from Blob Storage
     * @param schemaId
     * @return
     * @throws ApplicationException
     */
    @Override
    public boolean cleanSystemSchemaProject(String schemaId) throws ApplicationException {
        this.updateDataPartitionId();
        return this.cleanSchemaProject(schemaId);
    }

    private String getSchemaBucketName(String dataPartitionId) {
        return tenantFactory.getTenantInfo(dataPartitionId).getProjectId() + SchemaConstants.SCHEMA_BUCKET_EXTENSION;
    }

    @Override
    protected ObmDestination getDestination() {
        return destinationProvider.getDestination(headers.getPartitionId());
    }

    @Override
    protected GetQuery<String> buildQueryFor(ObmDestination destination, Where where) {
        return null;
    }

}
