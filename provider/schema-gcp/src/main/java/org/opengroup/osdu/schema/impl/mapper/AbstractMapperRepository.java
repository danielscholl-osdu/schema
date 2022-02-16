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

package org.opengroup.osdu.schema.impl.mapper;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.gcp.osm.model.query.GetQuery;
import org.opengroup.osdu.core.gcp.osm.model.where.Where;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository

public abstract class AbstractMapperRepository<EntityT, DestinationT>{

    protected static final String NAME_FIELD = "name";
    protected static String ENTITY_CREATED = "The info about created entity is unavailable.";

    protected DpsHeaders headers;
    protected DestinationProvider<DestinationT> destinationProvider;
    protected JaxRsDpsLog log;

    public AbstractMapperRepository(DpsHeaders headers, DestinationProvider<DestinationT> destinationProvider, JaxRsDpsLog log) {
        this.headers = headers;
        this.destinationProvider = destinationProvider;
        this.log = log;
    }

    @Value("${shared.tenant.name:common}")
    protected String sharedTenant;

    protected void updateDataPartitionId() {
        headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
    }

    protected abstract DestinationT getDestination();
    protected abstract GetQuery<EntityT> buildQueryFor(DestinationT destination, Where where);
}
