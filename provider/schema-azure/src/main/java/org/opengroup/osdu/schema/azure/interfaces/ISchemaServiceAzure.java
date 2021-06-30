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

package org.opengroup.osdu.schema.azure.interfaces;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.model.SchemaUpsertResponse;

public interface ISchemaServiceAzure {

    /**
     * This method creates a shared schema
     * @param schemaRequest             schema request
     * @return                          Schema info of created schema
     * @throws ApplicationException
     * @throws BadRequestException
     */
    SchemaInfo createSystemSchema(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException;

    /**
     * This method first tries to update the schema with the given schema-id. If there is no schema found,
     * 	it tries to create the new shared schema.
     * @param schemaRequest             schema request
     * @return                          schema upsert response
     * @throws ApplicationException
     * @throws BadRequestException
     */
    SchemaUpsertResponse upsertSystemSchema(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException;
}
