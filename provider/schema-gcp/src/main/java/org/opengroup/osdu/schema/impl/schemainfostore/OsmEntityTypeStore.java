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

import static org.opengroup.osdu.core.gcp.osm.model.where.predicate.Eq.eq;

import java.text.MessageFormat;
import org.apache.commons.lang3.ObjectUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.gcp.osm.model.Destination;
import org.opengroup.osdu.core.gcp.osm.model.query.GetQuery;
import org.opengroup.osdu.core.gcp.osm.model.where.Where;
import org.opengroup.osdu.core.gcp.osm.service.Context;
import org.opengroup.osdu.core.gcp.osm.translate.TranslatorRuntimeException;
import org.opengroup.osdu.schema.configuration.PropertiesConfiguration;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository class to register Entity type in KV store using OSM.
 */

@Repository
public class OsmEntityTypeStore implements IEntityTypeStore {

    private static final String NAME_FIELD = "name";
    private static final String SYSTEM_ENTITY_KIND = "system_entity_type";
    private final DpsHeaders headers;
    private final DestinationProvider<Destination> destinationProvider;
    private final JaxRsDpsLog log;
    private final PropertiesConfiguration configuration;
    private final Context context;

    @Autowired
    public OsmEntityTypeStore(DpsHeaders headers, DestinationProvider<Destination> destinationProvider, JaxRsDpsLog log, Context context,
        PropertiesConfiguration configuration) {
        this.headers = headers;
        this.destinationProvider = destinationProvider;
        this.log = log;
        this.configuration = configuration;
        this.context = context;
    }

    @Override
    public EntityType get(String entityTypeId) throws NotFoundException, ApplicationException {
        Destination tenantDestination = getPrivateTenantDestination(this.headers.getPartitionId());

        return context.findOne(buildQueryFor(tenantDestination, eq(NAME_FIELD, entityTypeId)))
            .orElseThrow(() ->
                new NotFoundException("bad input parameter"));
    }

    /**
     * Method to get System entity type from google store
     *
     * @param entityTypeId
     * @return EntityType object
     * @throws NotFoundException
     * @throws ApplicationException
     */
    @Override
    public EntityType getSystemEntity(String entityTypeId) throws NotFoundException, ApplicationException {
        Destination systemDestination = getSystemDestination();

        return context.findOne(buildQueryFor(systemDestination, eq(NAME_FIELD, entityTypeId)))
            .orElseThrow(() ->
                new NotFoundException("bad input parameter"));
    }

    @Override
    public EntityType create(EntityType entityType) throws BadRequestException, ApplicationException {
        Destination tenantDestination = getPrivateTenantDestination(this.headers.getPartitionId());
        checkEntityExistence(entityType, tenantDestination);

        EntityType entityFromDb;
        try {
            entityFromDb = context.createAndGet(entityType, getPrivateTenantDestination(this.headers.getPartitionId()));
        } catch (TranslatorRuntimeException ex) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
            throw new ApplicationException(SchemaConstants.INVALID_INPUT);
        }
        log.info(SchemaConstants.ENTITY_TYPE_CREATED);
        return entityFromDb;
    }

    /**
     * Method to create entityType in google store of dataPartitionId GCP
     *
     * @param entityType
     * @return EntityType object
     * @throws BadRequestException
     * @throws ApplicationException
     */
    @Override
    public EntityType createSystemEntity(EntityType entityType) throws BadRequestException, ApplicationException {
        Destination systemDestination = getSystemDestination();
        checkEntityExistence(entityType, systemDestination);

        EntityType entityFromDb;
        try {
            entityFromDb = context.createAndGet(entityType, systemDestination);
        } catch (TranslatorRuntimeException ex) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
            throw new ApplicationException(SchemaConstants.INVALID_INPUT);
        }
        log.info(SchemaConstants.ENTITY_TYPE_CREATED);
        return entityFromDb;
    }

    private GetQuery<EntityType> buildQueryFor(Destination destination, Where where) {
        return new GetQuery<>(EntityType.class, destination, where);
    }

    private void checkEntityExistence(EntityType entityType, Destination destination) throws BadRequestException {
        EntityType entityFromDb = context.getOne(buildQueryFor(destination, eq(NAME_FIELD, entityType.getEntityTypeId())));
        if (ObjectUtils.isNotEmpty(entityFromDb)) {
            log.warning(SchemaConstants.ENTITY_TYPE_EXISTS);
            throw new BadRequestException(MessageFormat.format(SchemaConstants.ENTITY_TYPE_EXISTS_EXCEPTION,
                entityType.getEntityTypeId()));
        }
    }

    private Destination getPrivateTenantDestination(String partitionId) {
        return destinationProvider.getDestination(
            partitionId,
            SchemaConstants.NAMESPACE,
            SchemaConstants.ENTITYTYPE_KIND
        );
    }

    private Destination getSystemDestination() {
        return destinationProvider.getDestination(
            configuration.getSharedTenantName(),
            SchemaConstants.NAMESPACE,
            SYSTEM_ENTITY_KIND
        );
    }
}
