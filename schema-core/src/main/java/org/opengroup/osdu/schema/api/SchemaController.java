package org.opengroup.osdu.schema.api;

import javax.validation.Valid;

import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaInfoResponse;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.service.ISchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("schema")
public class SchemaController {

    @Autowired
    ISchemaService schemaService;

    @PostMapping()
    @PreAuthorize("@authorizationFilter.hasRole('" + SchemaConstants.ENTITLEMENT_SERVICE_GROUP_EDITORS + "')")
    public ResponseEntity<SchemaInfo> createSchema(@Valid @RequestBody SchemaRequest schemaRequest)
            throws ApplicationException, BadRequestException, JsonProcessingException {
        return new ResponseEntity<>(schemaService.createSchema(schemaRequest), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationFilter.hasRole('" + SchemaConstants.ENTITLEMENT_SERVICE_GROUP_VIEWERS + "')")
    public ResponseEntity<Object> getSchema(@PathVariable("id") String id)
            throws ApplicationException, NotFoundException, BadRequestException {
        return new ResponseEntity<>(schemaService.getSchema(id), HttpStatus.OK);
    }

    @GetMapping()
    @PreAuthorize("@authorizationFilter.hasRole('" + SchemaConstants.ENTITLEMENT_SERVICE_GROUP_VIEWERS + "')")
    public ResponseEntity<SchemaInfoResponse> getSchemaInfoList(
            @RequestParam(required = false, name = "authority") String authority,
            @RequestParam(required = false, name = "source") String source,
            @RequestParam(required = false, name = "entityType") String entityType,
            @RequestParam(required = false, name = "schemaVersionMajor") Long schemaVersionMajor,
            @RequestParam(required = false, name = "schemaVersionMinor") Long schemaVersionMinor,
            @RequestParam(required = false, name = "status") String status,
            @RequestParam(required = false, name = "scope") String scope,
            @RequestParam(required = false, name = "latestVersion") Boolean latestVersion,
            @RequestParam(required = false, name = "limit", defaultValue = "100") int limit,
            @RequestParam(required = false, name = "offset", defaultValue = "0") int offset)
            throws ApplicationException, NotFoundException, BadRequestException {
        QueryParams queryParams = QueryParams.builder().authority(authority).source(source).entityType(entityType)
                .schemaVersionMajor(schemaVersionMajor).schemaVersionMinor(schemaVersionMinor).limit(limit)
                .offset(offset).scope(scope).status(status).latestVersion(latestVersion).build();
        return new ResponseEntity<SchemaInfoResponse>(schemaService.getSchemaInfoList(queryParams), HttpStatus.OK);
    }

    @PutMapping()
    @PreAuthorize("@authorizationFilter.hasRole('" + SchemaConstants.ENTITLEMENT_SERVICE_GROUP_EDITORS + "')")
    public ResponseEntity<SchemaInfo> updateSchema(@Valid @RequestBody SchemaRequest schemaRequest)
            throws ApplicationException, BadRequestException, JsonProcessingException {
        return new ResponseEntity<>(schemaService.updateSchema(schemaRequest), HttpStatus.NO_CONTENT);
    }

}
