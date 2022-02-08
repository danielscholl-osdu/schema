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

package org.opengroup.osdu.schema.impl.schemainfostore;

import org.apache.commons.lang3.ObjectUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.gcp.osm.model.Destination;
import org.opengroup.osdu.core.gcp.osm.model.query.GetQuery;
import org.opengroup.osdu.core.gcp.osm.model.where.Where;
import org.opengroup.osdu.core.gcp.osm.service.Context;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;

import static org.opengroup.osdu.core.gcp.osm.model.where.predicate.Eq.eq;

/**
 * Repository class to register Entity type in KV store using OSM.
 *
 */

@Repository
public class OsmEntityTypeStore extends AbstractOsmRepository<EntityType> implements IEntityTypeStore {
    static {
        ENTITY_CREATED = SchemaConstants.ENTITY_TYPE_CREATED;
    }

    @Autowired
    public OsmEntityTypeStore(DpsHeaders headers, DestinationProvider<Destination> destinationProvider, JaxRsDpsLog log, Context context) {
        super(headers, destinationProvider, log, context);
    }

    /**
     * Method to get System entity type from google store
     * @param entityTypeId
     * @return EntityType object
     * @throws NotFoundException
     * @throws ApplicationException
     */
    @Override
    public EntityType getSystemEntity(String entityTypeId) throws NotFoundException, ApplicationException {
        return super.getSystemEntity(entityTypeId);
    }

    /**
     * Method to create entityType in google store of dataPartitionId GCP
     * @param entityType
     * @return EntityType object
     * @throws BadRequestException
     * @throws ApplicationException
     */
    @Override
    public EntityType createSystemEntity(EntityType entityType) throws BadRequestException, ApplicationException {
        return super.createSystemEntity(entityType);
    }

    @Override
    protected Destination getDestination() {
        return destinationProvider.getDestination(
                headers.getPartitionId(),
                SchemaConstants.NAMESPACE,
                SchemaConstants.ENTITYTYPE_KIND
        );
    }

    @Override
    protected GetQuery<EntityType> buildQueryFor(Destination destination, Where where) {
        return new GetQuery<>(EntityType.class, destination, where);
    }

    @Override
    protected void checkEntityExistence(EntityType entityType) throws BadRequestException {
        EntityType entityFromDb = context.getOne(buildQueryFor(getDestination(), eq(NAME_FIELD, entityType.getEntityTypeId())));
        if (ObjectUtils.isNotEmpty(entityFromDb)) {
            log.warning(SchemaConstants.ENTITY_TYPE_EXISTS);
            throw new BadRequestException(MessageFormat.format(SchemaConstants.ENTITY_TYPE_EXISTS_EXCEPTION,
                    entityType.getEntityTypeId()));
        }
    }
}
