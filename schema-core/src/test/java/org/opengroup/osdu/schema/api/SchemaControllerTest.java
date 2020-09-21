package org.opengroup.osdu.schema.api;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaInfoResponse;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.model.SchemaUpsertResponse;
import org.opengroup.osdu.schema.service.ISchemaService;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.fasterxml.jackson.core.JsonProcessingException;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaControllerTest {

    @Mock
    ISchemaService schemaService;

    @InjectMocks
    SchemaController schemaController;

    private SchemaRequest schemaRequest;

    @Test
    public void testGetSchema_SuccessScenario() throws ApplicationException, NotFoundException, BadRequestException {
        String schemaId = "testschema";
        when(schemaService.getSchema(schemaId)).thenReturn("{}");
        assertNotNull(schemaController.getSchema(schemaId));
    }

    @Test
    public void testCreateSchema()
            throws ApplicationException, NotFoundException, BadRequestException, JsonProcessingException {
        schemaRequest = getSchemaRequestObject();

        when(schemaService.createSchema(schemaRequest)).thenReturn(getSchemaInfoObject());
        assertNotNull(schemaController.createSchema(schemaRequest));

    }

    @Test
    public void testUpsertSchema_update() throws ApplicationException, BadRequestException {
        schemaRequest = getSchemaRequestObject();

        when(schemaService.upsertSchema(schemaRequest)).thenReturn(getSchemaUpsertResponse_Updated());
        assertNotNull(schemaController.upsertSchema(schemaRequest));

    }

    @Test
    public void testUpsertSchema_create() throws ApplicationException, BadRequestException {
        schemaRequest = getSchemaRequestObject();

        when(schemaService.upsertSchema(schemaRequest)).thenReturn(getSchemaUpsertResponse_Created());
        assertNotNull(schemaController.upsertSchema(schemaRequest));

    }
    
    @Test(expected = BadRequestException.class)
    public void testUpsertSchema_Failed() throws ApplicationException, BadRequestException {
        schemaRequest = getSchemaRequestObject();

        when(schemaService.upsertSchema(schemaRequest)).thenThrow(BadRequestException.class);
        schemaController.upsertSchema(schemaRequest);
    }

    @Test
    public void testGetSchemaInfoList() throws ApplicationException, NotFoundException, BadRequestException {
        schemaRequest = getSchemaRequestObject();

        when(schemaService.getSchemaInfoList(QueryParams.builder().authority("test").build()))
                .thenReturn(SchemaInfoResponse.builder().schemaInfos(new LinkedList<SchemaInfo>()).build());

        assertNotNull(
                schemaController.getSchemaInfoList("test", null, null, null, null, null, null, null, null, 100, 0));

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
    
    private SchemaUpsertResponse getSchemaUpsertResponse_Created() {
        return SchemaUpsertResponse.builder().schemaInfo(getSchemaInfoObject()).httpCode(HttpStatus.CREATED).build();
    }
    
    private SchemaUpsertResponse getSchemaUpsertResponse_Updated() {
        return SchemaUpsertResponse.builder().schemaInfo(getSchemaInfoObject()).httpCode(HttpStatus.OK).build();
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
}