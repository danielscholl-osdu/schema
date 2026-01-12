package org.opengroup.osdu.schema.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.model.SchemaUpsertResponse;
import org.opengroup.osdu.schema.service.ISchemaService;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class SystemSchemaControllerTest {

    @Mock
    ISchemaService schemaService;

    @InjectMocks
    SystemSchemaController systemSchemaController;

    private SchemaRequest schemaRequest;
    
    @Test
    public void testUpsertSchema_update() throws ApplicationException, BadRequestException {
        schemaRequest = getSchemaRequestObject();

        when(schemaService.upsertSystemSchema(schemaRequest)).thenReturn(getSchemaUpsertResponse_Updated());
        assertNotNull(systemSchemaController.upsertSystemSchema(schemaRequest));

    }

    @Test
    public void testUpsertSchema_create() throws ApplicationException, BadRequestException {
        schemaRequest = getSchemaRequestObject();

        when(schemaService.upsertSystemSchema(schemaRequest)).thenReturn(getSchemaUpsertResponse_Created());
        assertNotNull(systemSchemaController.upsertSystemSchema(schemaRequest));

    }

    @Test(expected = BadRequestException.class)
    public void testUpsertSchema_Failed() throws ApplicationException, BadRequestException {
        schemaRequest = getSchemaRequestObject();

        when(schemaService.upsertSystemSchema(schemaRequest)).thenThrow(BadRequestException.class);
        systemSchemaController.upsertSystemSchema(schemaRequest);
    }

    private SchemaRequest getSchemaRequestObject() {
        return SchemaRequest.builder().schema(null).schemaInfo(SchemaInfo.builder().createdBy("creator")
                .dateCreated(new Date(System.currentTimeMillis()))
                .schemaIdentity(SchemaIdentity.builder().authority("os").entityType("well").id("os..wks.well.1.1")
                        .schemaVersionMajor(1L).schemaVersionMinor(1L).source("wks").build())
                .scope(SchemaScope.INTERNAL).status(SchemaStatus.DEVELOPMENT)
                .supersededBy(SchemaIdentity.builder().authority("os").entityType("well").id("os..wks.well.1.4")
                        .schemaVersionMajor(1L).schemaVersionMinor(1L).source("wks").build())
                .build()).build();
    }

    private SchemaInfo getSchemaInfoObject() {
        return SchemaInfo.builder().createdBy("creator").dateCreated(new Date(System.currentTimeMillis()))
                .schemaIdentity(SchemaIdentity.builder().authority("os").entityType("well").id("os..wks.well.1.1")
                        .schemaVersionMajor(1L).schemaVersionMinor(1L).source("wks").build())
                .scope(SchemaScope.INTERNAL).status(SchemaStatus.DEVELOPMENT)
                .supersededBy(SchemaIdentity.builder().authority("os").entityType("well").id("os..wks.well.1.4")
                        .schemaVersionMajor(1L).schemaVersionMinor(1L).source("wks").build())
                .build();
    }

    private SchemaUpsertResponse getSchemaUpsertResponse_Created() {
        return SchemaUpsertResponse.builder().schemaInfo(getSchemaInfoObject()).httpCode(HttpStatus.CREATED).build();
    }

    private SchemaUpsertResponse getSchemaUpsertResponse_Updated() {
        return SchemaUpsertResponse.builder().schemaInfo(getSchemaInfoObject()).httpCode(HttpStatus.OK).build();
    }
}
