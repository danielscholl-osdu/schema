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

package org.opengroup.osdu.schema.di;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.gcp.googleidtoken.GcpServiceAccountJwtClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

@Configuration
public class PartitionProviderConfig {

    @Bean
    @Primary
    @Scope(value = SCOPE_PROTOTYPE, proxyMode = TARGET_CLASS)
    public IPartitionProvider partitionProvider(
            IPartitionFactory partitionFactory,
            GcpServiceAccountJwtClient jwtClient
    ) {
        DpsHeaders partitionHeaders = new DpsHeaders();
        String idToken = jwtClient.getDefaultOrInjectedServiceAccountIdToken();
        partitionHeaders.put("authorization", idToken);
        return partitionFactory.create(partitionHeaders);
    }
}
