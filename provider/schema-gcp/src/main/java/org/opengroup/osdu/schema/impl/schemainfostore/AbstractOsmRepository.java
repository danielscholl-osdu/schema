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

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.gcp.osm.model.Destination;
import org.opengroup.osdu.core.gcp.osm.service.Context;
import org.opengroup.osdu.core.gcp.osm.translate.TranslatorRuntimeException;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.impl.mapper.AbstractMapperRepository;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;

import static org.opengroup.osdu.core.gcp.osm.model.where.predicate.Eq.eq;

@Repository
public abstract class AbstractOsmRepository<EntityT> extends AbstractMapperRepository<EntityT, Destination> {

    protected Context context;

    public AbstractOsmRepository(DpsHeaders headers, DestinationProvider<Destination> destinationProvider, JaxRsDpsLog log, Context context) {
        super(headers, destinationProvider, log);
        this.context = context;
    }

    /**
     * Method to get entity from KV store
     *
     * @param entityId
     * @return Entity object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    public EntityT get(String entityId) throws NotFoundException, ApplicationException {
        Destination destination = getDestination();

        return context.findOne(buildQueryFor(destination, eq(NAME_FIELD, entityId)))
                .orElseThrow(() ->
                        new NotFoundException("bad input parameter"));
    }

    /**
     * Method to get System entity from KV store
     * @param entityId
     * @return Entity object
     * @throws NotFoundException
     * @throws ApplicationException
     */
    public EntityT getSystemEntity(String entityId) throws NotFoundException, ApplicationException {
        this.updateDataPartitionId();
        return this.get(entityId);
    }

    /**
     * Method to create entity in KV store of dataPartitionId project
     *
     * @param entity
     * @return Entity object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    public EntityT create(EntityT entity) throws ApplicationException, BadRequestException {
        checkEntityExistence(entity);

        EntityT entityFromDb;
        try {
            entityFromDb = context.createAndGet(entity, getDestination());
        } catch (TranslatorRuntimeException ex) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
            throw new ApplicationException(SchemaConstants.INVALID_INPUT);
        }
        log.info(ENTITY_CREATED);
        return entityFromDb;
    }

    /**
     * Method to create System entity in KV store of dataPartitionId project
     * @param entity
     * @return Entity Object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    public EntityT createSystemEntity(EntityT entity) throws ApplicationException, BadRequestException {
        this.updateDataPartitionId();
        return this.create(entity);
    }

    protected abstract void checkEntityExistence(EntityT entity) throws BadRequestException;
}
