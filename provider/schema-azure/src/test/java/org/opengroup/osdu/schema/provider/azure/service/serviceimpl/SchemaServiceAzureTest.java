package org.opengroup.osdu.schema.provider.azure.service.serviceimpl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.di.SystemResourceConfig;
import org.opengroup.osdu.schema.azure.service.serviceimpl.SchemaServiceAzure;
import org.opengroup.osdu.schema.constants.SchemaConstants;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

public class SchemaServiceAzureTest {
    @InjectMocks
    SchemaServiceAzure schemaServiceAzure;

    @Mock
    ISchemaService schemaServiceCore;

    @Mock
    DpsHeaders headers;

    @Mock
    SchemaUpsertResponse upsertResponse;

    @Mock
    SystemResourceConfig systemResourceConfig;

    private Date currDate = new Date();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final String SHARED_TENANT = "common";
    private static final String PRIVATE_TENANT = "opendes";

    @Before
    public void setUp() {
        initMocks(this);
        Mockito.when(systemResourceConfig.getSharedTenant()).thenReturn(SHARED_TENANT);
    }

    @Test
    public void testCreateSharedSchema() throws BadRequestException, ApplicationException {
        SchemaRequest schemaRequest = getMockSchemaObject_published_SharedScope();
        SchemaInfo schemaInfo =  getMockSchemaInfo_Published_SharedScope();
        Mockito.when(schemaServiceCore.createSchema(schemaRequest)).thenReturn(schemaInfo);

        assertEquals(schemaInfo, schemaServiceAzure.createSystemSchema(schemaRequest));
        verify(this.headers, times(1)).put(SchemaConstants.DATA_PARTITION_ID, SHARED_TENANT);
    }
    
    @Test
    public void testCreateSharedSchema_SchemaAlreadyPresent() throws BadRequestException, ApplicationException {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(SchemaConstants.SCHEMA_ID_EXISTS);
        SchemaRequest schemaRequest = getMockSchemaObject_published_SharedScope();
        Mockito.when(schemaServiceCore.createSchema(schemaRequest)).thenThrow(new BadRequestException(SchemaConstants.SCHEMA_ID_EXISTS));
        schemaServiceAzure.createSystemSchema(schemaRequest);
    }

    @Test
    public void testUpsertSharedSchema_SuccessfulUpdate() throws BadRequestException, ApplicationException {
        SchemaRequest schemaRequest = getMockSchemaObject_published_SharedScope();
        SchemaInfo schemaInfo =  getMockSchemaInfo_Published_SharedScope();
        Mockito.when(upsertResponse.getHttpCode()).thenReturn(HttpStatus.OK);
        Mockito.when(schemaServiceCore.upsertSchema(schemaRequest)).thenReturn(upsertResponse);

        assertEquals(HttpStatus.OK, schemaServiceAzure.upsertSystemSchema(schemaRequest).getHttpCode());
        verify(this.headers, times(1)).put(SchemaConstants.DATA_PARTITION_ID, SHARED_TENANT);
        verify(this.schemaServiceCore, times(1)).upsertSchema(schemaRequest);
    }

    @Test
    public void testUpsertSharedSchema_SuccessfulCreate () throws BadRequestException, ApplicationException {
        SchemaRequest schemaRequest = getMockSchemaObject_published_SharedScope();
        SchemaInfo schemaInfo =  getMockSchemaInfo_Published_SharedScope();
        Mockito.when(upsertResponse.getHttpCode()).thenReturn(HttpStatus.CREATED);
        Mockito.when(schemaServiceCore.upsertSchema(schemaRequest)).thenReturn(upsertResponse);

        assertEquals(HttpStatus.CREATED, schemaServiceAzure.upsertSystemSchema(schemaRequest).getHttpCode());
        verify(this.headers, times(1)).put(SchemaConstants.DATA_PARTITION_ID, SHARED_TENANT);
        verify(this.schemaServiceCore, times(1)).upsertSchema(schemaRequest);
    }

    @Test(expected = BadRequestException.class)
    public void testUpsertSharedSchema_WhenSchemaExistInOtherTenant()
            throws ApplicationException, NotFoundException, BadRequestException {
        SchemaRequest schemaRequest = getMockSchemaObject_published_SharedScope();
        Mockito.when(schemaServiceCore.upsertSchema(schemaRequest)).thenThrow(new BadRequestException());
        schemaServiceAzure.upsertSystemSchema(schemaRequest).getHttpCode();
    }


    private SchemaRequest getMockSchemaObject_published_SharedScope() {
        return SchemaRequest.builder().schema("{}")
                .schemaInfo(SchemaInfo.builder()
                        .schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
                                .schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
                                .id("os:wks:well:1.1.1").build())
                        .dateCreated(currDate).scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).build())
                .build();

    }

    private SchemaInfo getMockSchemaInfo_Published_SharedScope() {
        return SchemaInfo.builder()
                .schemaIdentity(
                        SchemaIdentity.builder().authority("os").source("wks").entityType("well").schemaVersionMajor(1L)
                                .schemaVersionMinor(1L).schemaVersionPatch(1L).id("os:wks:well:1.1.1").build())
                .dateCreated(currDate).scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).build();
    }
}
