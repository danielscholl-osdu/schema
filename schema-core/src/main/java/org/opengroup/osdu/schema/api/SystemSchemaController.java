package org.opengroup.osdu.schema.api;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.model.SchemaUpsertResponse;
import org.opengroup.osdu.schema.service.ISchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/schemas/system")
public class SystemSchemaController {

    @Autowired
    ISchemaService schemaService;

    @PutMapping()
    @PreAuthorize("@authorizationFilterSA.hasPermissions()")
    public ResponseEntity<SchemaInfo> upsertSystemSchema(@Valid @RequestBody SchemaRequest schemaRequest)
            throws ApplicationException, BadRequestException {
        SchemaUpsertResponse upsertResp = schemaService.upsertSystemSchema(schemaRequest);
        ResponseEntity<SchemaInfo> response = new ResponseEntity<>(upsertResp.getSchemaInfo(), upsertResp.getHttpCode());
        return response;
    }
}
