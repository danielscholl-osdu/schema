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

package org.opengroup.osdu.schema.azure.api;

import org.opengroup.osdu.schema.azure.interfaces.ISchemaServiceAzure;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.model.SchemaUpsertResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@RestController
@RequestMapping("/schemas/system")
public class AzureSchemaApi {

    @Autowired
    ISchemaServiceAzure schemaService;

    @PutMapping()
    @PreAuthorize("@authorizationFilterSP.hasPermissions()")
    public ResponseEntity<SchemaInfo> upsertSystemSchema(@Valid @RequestBody SchemaRequest schemaRequest)
            throws ApplicationException, BadRequestException {
        SchemaUpsertResponse upsertResp = schemaService.upsertSystemSchema(schemaRequest);
        ResponseEntity<SchemaInfo> response = new ResponseEntity<>(upsertResp.getSchemaInfo(), upsertResp.getHttpCode());
        return response;
    }

}
