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
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;

import static org.opengroup.osdu.core.gcp.osm.model.where.predicate.Eq.eq;

/**
 * Repository class to to register authority in KV Store using OSM
 *
 */

@Repository
public class OsmAuthorityStore extends AbstractOsmRepository<Authority> implements IAuthorityStore {
    static {
        ENTITY_CREATED = SchemaConstants.AUTHORITY_CREATED;
    }

    @Autowired
    public OsmAuthorityStore(DpsHeaders headers, DestinationProvider<Destination> destinationProvider, JaxRsDpsLog log, Context context) {
        super(headers, destinationProvider, log, context);
    }

    @Override
    protected Destination getDestination() {
        return destinationProvider.getDestination(
                headers.getPartitionId(),
                SchemaConstants.NAMESPACE,
                SchemaConstants.AUTHORITY_KIND
        );
    }

    @Override
    protected GetQuery<Authority> buildQueryFor(Destination destination, Where where) {
        return new GetQuery<>(Authority.class, destination, where);
    }

    @Override
    protected void checkEntityExistence(Authority authority) throws BadRequestException{
        Authority entityFromDb = context.getOne(buildQueryFor(getDestination(), eq(NAME_FIELD, authority.getAuthorityId())));

        if (ObjectUtils.isNotEmpty(entityFromDb)) {
            log.warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
            throw new BadRequestException(
                    MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_EXCEPTION, authority.getAuthorityId()));
        }
    }

    @Override
    public Authority getSystemAuthority(String authorityId) throws NotFoundException, ApplicationException {
        return getSystemEntity(authorityId);
    }

    @Override
    public Authority createSystemAuthority(Authority authority) throws ApplicationException, BadRequestException {
        return createSystemEntity(authority);
    }
}
