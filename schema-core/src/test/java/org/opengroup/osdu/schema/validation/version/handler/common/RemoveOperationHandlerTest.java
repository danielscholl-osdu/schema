package org.opengroup.osdu.schema.validation.version.handler.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.util.SchemaUtil;
import org.opengroup.osdu.schema.util.TestUtility;
import org.opengroup.osdu.schema.validation.version.SchemaValidationType;
import org.opengroup.osdu.schema.validation.version.handler.SchemaValidationHandler;
import org.opengroup.osdu.schema.validation.version.model.SchemaBreakingChanges;
import org.opengroup.osdu.schema.validation.version.model.SchemaHandlerVO;
import org.opengroup.osdu.schema.validation.version.model.SchemaPatch;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
public class RemoveOperationHandlerTest {

	@InjectMocks
	RemoveOperationHandler removeOperationHandler;
	
	@Mock
	SchemaUtil schemaUtil;

	@Test
	public void testCompare_ValidChange_RemovePropWithOldTrue() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/remove_operation/allowed/base-schema.json"
				,"/schema_compare/remove_operation/allowed/new-schema.json");
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		schemaHandlerVO.getChangedRefIds().put("osdu:wks:AbstractCommonResources:1.0.0", "osdu:wks:AbstractCommonResources:1.1.0");
		for(SchemaPatch patch : schemaPatchList) {
			removeOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() > 0)
				Assert.fail();
		}

	}
	
	@Test
	public void testCompare_ValidChange_RemovePropInDefinition() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/remove_operation/allowed/base-schema.json"
				,"/schema_compare/remove_operation/allowed/new-schema.json");
		schemaHandlerVO.setValidationType(SchemaValidationType.MINOR);
		Mockito.when(schemaUtil.isValidSchemaVersionChange("osdu:wks:AbstractCommonResources:1.0.0", "osdu:wks:AbstractCommonResources:1.1.0", SchemaValidationType.MINOR)).thenReturn(true);
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		for(SchemaPatch patch : schemaPatchList) {
			removeOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() > 0)
				Assert.fail();
		}

	}
	
	@Test
	public void testCompare_ValidChange_RemovePropInDefinition_WithPatchUpgrage() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/remove_operation/allowed-patch/base-schema.json"
				,"/schema_compare/remove_operation/allowed-patch/new-schema.json");
		schemaHandlerVO.setValidationType(SchemaValidationType.MINOR);
		Mockito.when(schemaUtil.isValidSchemaVersionChange("osdu:wks:AbstractCommonResources:1.1.0", "osdu:wks:AbstractCommonResources:1.1.1", SchemaValidationType.MINOR)).thenReturn(true);
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		for(SchemaPatch patch : schemaPatchList) {
			removeOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() > 0)
				Assert.fail();
		}

	}
	
	@Test
	public void testCompare_ValidChange_RemovePropInDefinition_Patch() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/remove_operation/allowed-patch/base-schema.json"
				,"/schema_compare/remove_operation/allowed-patch/new-schema.json");
		schemaHandlerVO.setValidationType(SchemaValidationType.PATCH);
		Mockito.when(schemaUtil.isValidSchemaVersionChange("osdu:wks:AbstractCommonResources:1.1.0", "osdu:wks:AbstractCommonResources:1.1.1", SchemaValidationType.PATCH)).thenReturn(true);
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		for(SchemaPatch patch : schemaPatchList) {
			removeOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() > 0)
				Assert.fail();
		}

	}
	
	@Test
	public void testCompare_BreakingChange_RemovePropInDefinition() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/remove_operation/allowed/new-schema.json",
						"/schema_compare/remove_operation/allowed/base-schema.json"
				);
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		schemaHandlerVO.setValidationType(SchemaValidationType.MINOR);
		Mockito.when(schemaUtil.isValidSchemaVersionChange("osdu:wks:AbstractCommonResources:1.1.0", "osdu:wks:AbstractCommonResources:1.0.0", SchemaValidationType.MINOR)).thenReturn(false);
		for(SchemaPatch patch : schemaPatchList) {
			removeOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() > 0)
				Assert.assertTrue(true);
			else
				Assert.fail();
		}

	}
	
	@Test
	public void testCompare_BreakingChange_RemovePropInDefinition_Patch() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/remove_operation/allowed-patch/new-schema.json",
						"/schema_compare/remove_operation/allowed-patch/base-schema.json"
				);
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		schemaHandlerVO.setValidationType(SchemaValidationType.PATCH);
		Mockito.when(schemaUtil.isValidSchemaVersionChange("osdu:wks:AbstractCommonResources:1.1.1", "osdu:wks:AbstractCommonResources:1.1.0", SchemaValidationType.PATCH)).thenReturn(false);
		for(SchemaPatch patch : schemaPatchList) {
			removeOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() > 0)
				Assert.assertTrue(true);
			else
				Assert.fail();
		}

	}

	@Test
	public void testCompare_BreakingChange_ChangeFromTrueToFalse() throws IOException, ApplicationException {
		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/remove_operation/base-schema.json"
				,"/schema_compare/remove_operation/breaking/attribute-removed.json");
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());

		for(SchemaPatch patch : schemaPatchList) {
			removeOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() == 0)
				Assert.fail();
		}
	}

	@Test
	public void testCompare_PassdownToNextHandler() throws IOException, ApplicationException {
		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/remove_operation/base-schema.json"
				,"/schema_compare/remove_operation/other/added-new-attr.json");
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());

		SchemaValidationHandler dummyHandler = Mockito.spy(new DummyHandler());
		removeOperationHandler.setNextHandler(dummyHandler);

		for(SchemaPatch patch : schemaPatchList) {
			removeOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
		}

		assertThat(schemaBreakingChanges.size() == 0);
		Mockito.verify(dummyHandler, Mockito.atLeastOnce()).compare(schemaHandlerVO, schemaPatchList.get(0), schemaBreakingChanges, processedArrayPath);
	}

	private SchemaHandlerVO getMockSchemaHandlerVO(String baseSchemaPath, String newSchemaPath) throws IOException {
		JsonNode baseSchema = TestUtility.getJsonNodeFromFile(baseSchemaPath);
		JsonNode newSchema = TestUtility.getJsonNodeFromFile(newSchemaPath);
		JsonNode baseDef = new ObjectMapper().createObjectNode();
		JsonNode newDef =  new ObjectMapper().createObjectNode();

		SchemaHandlerVO newSchemaDiff = new SchemaHandlerVO(baseSchema, newSchema, baseDef, newDef, SchemaValidationType.MINOR);

		return newSchemaDiff;
	}
}
