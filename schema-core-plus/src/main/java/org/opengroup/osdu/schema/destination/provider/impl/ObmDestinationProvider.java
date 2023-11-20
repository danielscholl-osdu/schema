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

package org.opengroup.osdu.schema.destination.provider.impl;

import static org.opengroup.osdu.schema.destination.provider.model.DestinationInstructions.toObmDestination;

import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.obm.core.persistence.ObmDestination;
import org.opengroup.osdu.schema.destination.provider.DestinationProviderImpl;
import org.opengroup.osdu.schema.destination.provider.model.DestinationInstructions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ObmDestinationProvider extends DestinationProviderImpl<ObmDestination> {

  @Autowired
  public ObmDestinationProvider(ITenantFactory tenantFactory) {
    super(tenantFactory);
  }

  @Override
  protected ObmDestination buildDestination(DestinationInstructions instructions) {
    return toObmDestination(instructions);
  }
}
