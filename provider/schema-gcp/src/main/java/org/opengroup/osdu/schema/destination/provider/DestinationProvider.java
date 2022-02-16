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

package org.opengroup.osdu.schema.destination.provider;

import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;

public interface DestinationProvider<DestinationT> {

    /**
     * The method used only for destinations without kind (i.e. {@link OqmDestination})
     * @param partitionId the id of partition
     * @return the destination for OSDU Mappers
     */
    DestinationT getDestination(String partitionId);

    /**
     * The method used only for destinations with kind (i.e. {@link org.opengroup.osdu.core.gcp.osm.model.Destination})
     * You can also pass empty string for the kind name if you want to use this method for building other destinations
     * @param partitionId the id of partition
     * @param kindName the name of the kind
     * @return the destination for OSDU Mappers
     */
    DestinationT getDestination(String partitionId, String kindName);

    /**
     * The method used if tenantInfo already acknowledged and there is no need to call TenantFactory to get the info
     *
     * @param tenantInfo tenant info got from somewhere
     * @param kindName the name of the kind
     * @return the destination for OSDU Mappers
     */
    DestinationT getDestination(TenantInfo tenantInfo, String kindName);

    /**
     * The method is used only for custom namespace and kind usage
     * 
     * @param partitionId partitionId for destination
     * @param namespace custom namespace
     * @param kindName the name of the kind
     * @return the destination for OSDU Mappers
     */
    DestinationT getDestination(String partitionId, String namespace, String kindName);
}
