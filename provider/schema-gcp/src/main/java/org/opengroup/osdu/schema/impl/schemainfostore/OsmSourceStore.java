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

import java.text.MessageFormat;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.multitenancy.DatastoreFactory;
import org.opengroup.osdu.core.gcp.osm.model.Destination;
import org.opengroup.osdu.core.gcp.osm.model.query.GetQuery;
import org.opengroup.osdu.core.gcp.osm.model.where.Where;
import org.opengroup.osdu.core.gcp.osm.service.Context;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
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

import static org.opengroup.osdu.core.gcp.osm.model.where.predicate.Eq.eq;
/**
 * Repository class to register Source in KV store.
 *
 *
 */

@Repository
public class OsmSourceStore extends AbstractOsmRepository<Source> implements ISourceStore {

    static {
        ENTITY_CREATED = SchemaConstants.SOURCE_CREATED;
    }

    @Autowired
    public OsmSourceStore(DpsHeaders headers, DestinationProvider<Destination> destinationProvider, JaxRsDpsLog log, Context context) {
        super(headers, destinationProvider, log, context);
    }

    /**
     * Method to get System Source in KV store
     * @param sourceId
     * @return Source object
     * @throws NotFoundException
     * @throws ApplicationException
     */
    @Override
    public Source getSystemSource(String sourceId) throws NotFoundException, ApplicationException {
        return this.getSystemEntity(sourceId);
    }

    /**
     * Method to create System Source in KV store
     * @param source
     * @return Source object
     * @throws BadRequestException
     * @throws ApplicationException
     */
    @Override
    public Source createSystemSource(Source source) throws BadRequestException, ApplicationException {
        return this.createSystemEntity(source);
    }
    protected Destination getDestination() {
        return destinationProvider.getDestination(
                headers.getPartitionId(),
                SchemaConstants.NAMESPACE,
                SchemaConstants.SOURCE_KIND
        );
    }

    @Override
    protected GetQuery<Source> buildQueryFor(Destination destination, Where where) {
        return new GetQuery<>(Source.class, destination, where);
    }

    @Override
    protected void checkEntityExistence(Source source) throws BadRequestException {
        Source entityFromDb = context.getOne(buildQueryFor(getDestination(), eq(NAME_FIELD, source.getSourceId())));

        if (ObjectUtils.isNotEmpty(entityFromDb)) {
            log.warning(SchemaConstants.SOURCE_EXISTS);
            throw new BadRequestException(
                    MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION, source.getSourceId()));
        }
    }
}
