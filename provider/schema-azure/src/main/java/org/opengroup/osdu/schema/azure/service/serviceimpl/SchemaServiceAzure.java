// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.schema.azure.service.serviceimpl;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.di.SystemResourceConfig;
import org.opengroup.osdu.schema.azure.interfaces.ISchemaServiceAzure;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.model.SchemaUpsertResponse;
import org.opengroup.osdu.schema.service.ISchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchemaServiceAzure implements ISchemaServiceAzure {

    private final ISchemaService schemaServiceCore;

    final DpsHeaders headers;

    private final SystemResourceConfig systemResourceConfig;

    public SchemaInfo createSystemSchema(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException {
        updateDataPartitionId();
        return schemaServiceCore.createSchema(schemaRequest);
    }

    public SchemaUpsertResponse upsertSystemSchema(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException {
        updateDataPartitionId();
        return schemaServiceCore.upsertSchema(schemaRequest);
    }

    private void updateDataPartitionId() {
        headers.put(SchemaConstants.DATA_PARTITION_ID, systemResourceConfig.getSharedTenant());
    }
}
