// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo;

import org.opengroup.osdu.core.aws.mongodb.MongoDBMultiClusterFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.EntityTypeDto;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.SchemaDBIndexFields;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import jakarta.inject.Inject;
import java.text.MessageFormat;


/**
 * The type Aws entity type store mongo db.
 */
@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "mongodb")
@Repository
public class MongoDBEntityTypeStore implements IEntityTypeStore {

    /**
     * The constant ENTITY_TYPE_PREFIX.
     */
    public static final String ENTITY_TYPE_PREFIX = "EntityType-";

    @Inject
    private DpsHeaders headers;

    @Inject
    private JaxRsDpsLog log;

    @Inject
    private MongoDBMultiClusterFactory mongoDBMultiClusterFactory;

    @Value("${shared.tenant.name:common}")
    private String sharedTenant;

    private String getDataPartitionId() {
        this.log.warning("TenantInfo found to be null, defaulting to partition id from headers");
        return this.headers.getPartitionId();
    }

    /**
     * Get EntityType.
     *
     * @param entityTypeId the entityType id
     * @return the EntityType
     */
    @Override
    public EntityType get(String entityTypeId) throws NotFoundException, ApplicationException {
        String dataPartitionId = getDataPartitionId();
        EntityTypeDto entityTypeDto = mongoDBMultiClusterFactory.getHelper(dataPartitionId).get(SchemaDBIndexFields.ID, entityTypeId, EntityTypeDto.class, getCollection(dataPartitionId));
        if (entityTypeDto == null) {
            throw new NotFoundException(SchemaConstants.INVALID_INPUT);
        }
        return entityTypeDto.getData();
    }

    /**
     * Get system EntityType.
     *
     * @param entityTypeId the entityType id
     * @return the EntityType
     */
    @Override
    public EntityType getSystemEntity(String entityTypeId) throws NotFoundException, ApplicationException {
        updateDataPartitionId();
        return get(entityTypeId);
    }

    /**
     * Create EntityType.
     *
     * @param entityType the EntityType
     * @return the EntityType
     */
    @Override
    public EntityType create(EntityType entityType) throws BadRequestException, ApplicationException {

        EntityTypeDto entityTypeDto = new EntityTypeDto();
        entityTypeDto.setId(entityType.getEntityTypeId());
        entityTypeDto.setData(entityType);
        try {
            String dataPartitionId = getDataPartitionId();
            mongoDBMultiClusterFactory.getHelper(dataPartitionId).insert(entityTypeDto, getCollection(dataPartitionId));
        } catch (DuplicateKeyException exception) {
            log.warning(SchemaConstants.ENTITY_TYPE_EXISTS);
            throw new BadRequestException(
                    MessageFormat.format(SchemaConstants.ENTITY_TYPE_EXISTS_EXCEPTION, entityType.getEntityTypeId()));
        } catch (Exception ex) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
            throw new ApplicationException(SchemaConstants.INVALID_INPUT);
        }
        log.info(SchemaConstants.ENTITY_TYPE_CREATED);
        return entityType;
    }

    /**
     * Create system EntityType.
     *
     * @param entityType the EntityType
     * @return the EntityType
     */
    @Override
    public EntityType createSystemEntity(EntityType entityType) throws BadRequestException, ApplicationException {
        updateDataPartitionId();
        return create(entityType);
    }

    private void updateDataPartitionId() {
        headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
    }

    private String getCollection(String dataPartitionId) {
        return ENTITY_TYPE_PREFIX + dataPartitionId;
    }
}
