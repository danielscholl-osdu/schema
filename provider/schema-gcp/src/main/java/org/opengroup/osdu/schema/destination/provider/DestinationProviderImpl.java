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

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.osm.model.Kind;
import org.opengroup.osdu.core.gcp.osm.model.Namespace;
import org.opengroup.osdu.schema.destination.provider.model.DestinationInstructions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public abstract class DestinationProviderImpl<DestinationT> implements DestinationProvider<DestinationT> {

    private final ITenantFactory tenantFactory;

    @Autowired
    public DestinationProviderImpl(ITenantFactory tenantFactory) {
        this.tenantFactory = tenantFactory;
    }

    @Override
    public DestinationT getDestination(String partitionId) {
        TenantInfo tenantInfo = tenantFactory.getTenantInfo(partitionId);
        return getDestination(tenantInfo, "");
    }

    @Override
    public DestinationT getDestination(String partitionId, String kindName) {
        TenantInfo tenantInfo = tenantFactory.getTenantInfo(partitionId);
        return getDestination(tenantInfo, kindName);
    }

    @Override
    public DestinationT getDestination(TenantInfo tenantInfo, String kindName) {
        log.debug("Providing destination for the tenant: " + tenantInfo.getName());
        String partitionId = tenantInfo.getDataPartitionId();
        String namespace = tenantInfo.getName();
        return getDestination(partitionId, namespace, kindName);
    }

    @Override
    public DestinationT getDestination(String partitionId, String namespace, String kindName) {

        DestinationInstructions instructions = DestinationInstructions.builder()
                .dataPartition(partitionId)
                .namespace(new Namespace(namespace))
                .kind(new Kind(kindName))
                .build();

        return buildDestination(instructions);
    }

    protected abstract DestinationT buildDestination(DestinationInstructions instructions);
}
