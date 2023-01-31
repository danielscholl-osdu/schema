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

package org.opengroup.osdu.schema.destination.provider.model;

import lombok.Builder;
import lombok.Data;
import org.opengroup.osdu.core.gcp.obm.persistence.ObmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.osm.model.Destination;
import org.opengroup.osdu.core.gcp.osm.model.Kind;
import org.opengroup.osdu.core.gcp.osm.model.Namespace;

@Data
@Builder
public class DestinationInstructions {

  private String dataPartition;
  private Namespace namespace;
  private Kind kind;

  public static Destination toOsmDestination(DestinationInstructions instructions) {
    return Destination.builder()
        .partitionId(instructions.dataPartition)
        .namespace(instructions.namespace)
        .kind(instructions.kind)
        .build();
  }

  public static OqmDestination toOqmDestination(DestinationInstructions instructions) {
    return OqmDestination.builder()
        .partitionId(instructions.dataPartition)
        .build();
  }

  public static ObmDestination toObmDestination(DestinationInstructions instructions) {
    return ObmDestination.builder()
        .partitionId(instructions.dataPartition)
        .build();
  }
}
