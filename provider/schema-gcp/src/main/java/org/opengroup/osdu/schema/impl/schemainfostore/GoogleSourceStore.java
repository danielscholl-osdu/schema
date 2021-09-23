/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.opengroup.osdu.schema.impl.schemainfostore;

import java.text.MessageFormat;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.multitenancy.DatastoreFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

/**
 * Repository class to to register Source in Google store.
 *
 *
 */

@Repository
public class GoogleSourceStore implements ISourceStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private DatastoreFactory dataStoreFactory;

    @Autowired
    private ITenantFactory tenantFactory;

    @Autowired
    JaxRsDpsLog log;

    @Value("${shared.tenant.name:common}")
    private String sharedTenant;

    /**
     * Method to get Source in google store
     *
     * @param sourceId
     * @return Source object
     * @throws ApplicationException
     */
    @Override
    public Source get(String sourceId) throws NotFoundException, ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(tenantFactory.getTenantInfo(headers.getPartitionId()));
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE).setKind(SchemaConstants.SOURCE_KIND)
                .newKey(sourceId);
        Entity entity = datastore.get(key);
        if (entity == null) {
            throw new NotFoundException("bad input parameter");
        } else {
            return mapEntityToDto(entity);
        }

    }

    /**
     * Method to get System Source in google store
     * @param sourceId
     * @return Source object
     * @throws NotFoundException
     * @throws ApplicationException
     */
    @Override
    public Source getSystemSource(String sourceId) throws NotFoundException, ApplicationException {
        this.updateDataPartitionId();
        return this.get(sourceId);
    }

    /**
     * Method to create Source in google store of dataPartitionId GCP
     *
     * @return Source object
     */
    @Override
    public Source create(Source source) throws BadRequestException, ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(tenantFactory.getTenantInfo(headers.getPartitionId()));
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE).setKind(SchemaConstants.SOURCE_KIND)
                .newKey(source.getSourceId());
        Entity entity = getEntityObject(key);

        try {
            datastore.add(entity);
        } catch (DatastoreException ex) {
            if ("ALREADY_EXISTS".equals(ex.getReason())) {
                log.warning(SchemaConstants.SOURCE_EXISTS);
                throw new BadRequestException(
                        MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION, source.getSourceId()));
            } else {
                log.error(SchemaConstants.OBJECT_INVALID);
                throw new ApplicationException(SchemaConstants.INVALID_INPUT);
            }
        }
        log.info(SchemaConstants.SOURCE_CREATED);
        return mapEntityToDto(entity);
    }

    /**
     * Method to create System Source in google store
     * @param source
     * @return Source object
     * @throws BadRequestException
     * @throws ApplicationException
     */
    @Override
    public Source createSystemSource(Source source) throws BadRequestException, ApplicationException {
        updateDataPartitionId();
        return this.create(source);
    }

    private Source mapEntityToDto(Entity entity) {

        Source source = new Source();
        source.setSourceId(entity.getKey().getName());
        return source;

    }

    private Entity getEntityObject(Key key) {
        Entity.Builder entityBuilder = Entity.newBuilder(key);
        entityBuilder.set(SchemaConstants.DATE_CREATED, Timestamp.now());
        return entityBuilder.build();
    }

    private void updateDataPartitionId() {
        headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
    }

}
