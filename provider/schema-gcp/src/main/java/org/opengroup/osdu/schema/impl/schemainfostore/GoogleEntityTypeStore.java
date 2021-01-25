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
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

/**
 * Repository class to register Entity type in Google store.
 *
 *
 */

@Repository
public class GoogleEntityTypeStore implements IEntityTypeStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private DatastoreFactory dataStoreFactory;

    @Autowired
    private ITenantFactory tenantFactory;

    @Autowired
    JaxRsDpsLog log;

    /**
     * Method to get entity type from google store
     *
     * @param entityTypeId
     * @return EntityType object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    @Override
    public EntityType get(String entityTypeId) throws NotFoundException, ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(tenantFactory.getTenantInfo(headers.getPartitionId()));
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.ENTITYTYPE_KIND).newKey(entityTypeId);
        Entity entity = datastore.get(key);
        if (entity == null) {
            throw new NotFoundException("bad input parameter");
        } else {
            return mapEntityToDto(entity);
        }

    }

    /**
     * Method to create entityType in google store of dataPartitionId GCP
     *
     * @param entityType
     * @return entityType object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public EntityType create(EntityType entityType) throws BadRequestException, ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(tenantFactory.getTenantInfo(headers.getPartitionId()));
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.ENTITYTYPE_KIND).newKey(entityType.getEntityTypeId());
        Entity entity = getEntityObject(key);
        try {
            datastore.add(entity);
        } catch (DatastoreException ex) {
            if ("ALREADY_EXISTS".equals(ex.getReason())) {
                log.warning(SchemaConstants.ENTITY_TYPE_EXISTS);
                throw new BadRequestException(MessageFormat.format(SchemaConstants.ENTITY_TYPE_EXISTS_EXCEPTION,
                        entityType.getEntityTypeId()));
            } else {
                log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
                throw new ApplicationException("Invalid input, object invalid");
            }
        }
        log.info(SchemaConstants.ENTITY_TYPE_CREATED);
        return mapEntityToDto(entity);
    }

    private EntityType mapEntityToDto(Entity entity) {

        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(entity.getKey().getName());
        return entityType;

    }

    private Entity getEntityObject(Key key) {
        Entity.Builder entityBuilder = Entity.newBuilder(key);
        entityBuilder.set(SchemaConstants.DATE_CREATED, Timestamp.now());
        return entityBuilder.build();
    }

}
