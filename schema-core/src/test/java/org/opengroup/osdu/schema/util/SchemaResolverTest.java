package org.opengroup.osdu.schema.util;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.service.ISchemaService;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaResolverTest {
    @InjectMocks
    SchemaResolver schemaResolver;

    @Mock
    ISchemaService schemaService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testResolveSchema()
            throws JSONException, BadRequestException, ApplicationException, NotFoundException, IOException {
        String resolvedSchema = new FileUtils().read("/test_schema/resolvedSchema.json");
        String orginalSchema = new FileUtils().read("/test_schema/originalSchema.json");
        String referenceSchema = new FileUtils().read("/test_schema/referenceSchema.json");
        Mockito.when(schemaService.getSchema("os:wks:anyCrsFeatureCollection.1.0")).thenReturn(referenceSchema);
        JSONAssert.assertEquals(resolvedSchema, schemaResolver.resolveSchema(orginalSchema), JSONCompareMode.LENIENT);
    }

    @Test
    public void testResolveSchema_definitionblock()
            throws JSONException, BadRequestException, ApplicationException, NotFoundException, IOException {
        String resolvedSchema = new FileUtils().read("/test_schema/resolvedSchema.json");
        String orginalSchema = new FileUtils().read("/test_schema/originalSchemaWithNoDefinitionBlock.json");
        String referenceSchema = new FileUtils().read("/test_schema/referenceSchemaWithDefinitionBlock.json");
        Mockito.when(schemaService.getSchema("os:wks:anyCrsFeatureCollection.1.0")).thenReturn(referenceSchema);
        JSONAssert.assertEquals(resolvedSchema, schemaResolver.resolveSchema(orginalSchema), JSONCompareMode.LENIENT);
    }

    @Test
    public void testResolveSchema_InvalidExternalPath()
            throws JSONException, BadRequestException, ApplicationException, NotFoundException, IOException {
        String orginalSchema = new FileUtils().read("/test_schema/originalSchemaWithInvalidExternalPath.json");
        String referenceSchema = new FileUtils().read("/test_schema/referenceSchemaWithDefinitionBlock.json");
        Mockito.when(schemaService.getSchema("os:wks:anyCrsFeatureCollection.1.0")).thenReturn(referenceSchema);
        expectedException.expect(ApplicationException.class);
        expectedException.expectMessage("Internal server error");
        schemaResolver.resolveSchema(orginalSchema);
    }

    @Test
    public void testResolveSchema_BadRequestExternalPath()
            throws JSONException, BadRequestException, ApplicationException, NotFoundException, IOException {
        String orginalSchema = new FileUtils().read("/test_schema/originalSchemaWithInvalidExternalPath2.json");
        String referenceSchema = new FileUtils().read("/test_schema/referenceSchemaWithDefinitionBlock.json");
        Mockito.when(schemaService.getSchema("os:wks:anyCrsFeatureCollection.1.0")).thenReturn(referenceSchema);
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(
                "Invalid Request, https://schema-service.endpoints.evd-ddl-us-services.cloud.goog/de/schema-service/v1/schema not resolvable");
        schemaResolver.resolveSchema(orginalSchema);
    }

    @Test
    public void testResolveSchema_invalidRefSchema()
            throws JSONException, BadRequestException, ApplicationException, NotFoundException, IOException {
        Mockito.when(schemaService.getSchema("os:wks:anyCrsFeatureCollection.1.0"))
                .thenThrow(NotFoundException.class);
        String orginalSchema = new FileUtils().read("/test_schema/originalSchema.json");
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(
                "Invalid input, os:wks:anyCrsFeatureCollection.1.0 not registered but provided as reference");
        schemaResolver.resolveSchema(orginalSchema);
        fail("Should not succeed");

    }
}