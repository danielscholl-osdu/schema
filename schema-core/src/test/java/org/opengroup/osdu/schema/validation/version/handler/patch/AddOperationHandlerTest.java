package org.opengroup.osdu.schema.validation.version.handler.patch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.util.TestUtility;
import org.opengroup.osdu.schema.validation.version.SchemaValidationType;
import org.opengroup.osdu.schema.validation.version.model.SchemaBreakingChanges;
import org.opengroup.osdu.schema.validation.version.model.SchemaHandlerVO;
import org.opengroup.osdu.schema.validation.version.model.SchemaPatch;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
public class AddOperationHandlerTest {
	@InjectMocks
	AddOperationHandler addOperationHandler;
	
	@Test
	public void testCompare_AddNewAttr_NotAllowed() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/add_operation/base-schema.json"
				,"/schema_compare/add_operation/addattr-breaking-patch.json");
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		for(SchemaPatch patch : schemaPatchList) {
			addOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() == 0)
				Assert.fail();
		}

	}
	
	@Test
	public void testCompare_NoChange() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/add_operation/base-schema.json"
				,"/schema_compare/add_operation/base-schema.json");
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		for(SchemaPatch patch : schemaPatchList) {
			addOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() > 0)
				Assert.fail();
		}

	}
	
	@Test
	public void testCompare_AddAttr_Minor() throws IOException, ApplicationException {

		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Set<String> processedArrayPath = new HashSet<>();
		SchemaHandlerVO schemaHandlerVO = getMockSchemaHandlerVO("/schema_compare/add_operation/base-schema.json"
				,"/schema_compare/add_operation/base-schema.json");
		List<SchemaPatch> schemaPatchList = TestUtility.findSchemaPatch(schemaHandlerVO.getSourceSchema(), schemaHandlerVO.getTargetSchema());
		
		schemaHandlerVO.setValidationType(SchemaValidationType.MINOR);
		for(SchemaPatch patch : schemaPatchList) {
			addOperationHandler.compare(schemaHandlerVO, patch, schemaBreakingChanges, processedArrayPath);
			if(schemaBreakingChanges.size() > 0)
				Assert.fail();
		}

	}
	
	private SchemaHandlerVO getMockSchemaHandlerVO(String baseSchemaPath, String newSchemaPath) throws IOException {
		JsonNode baseSchema = TestUtility.getJsonNodeFromFile(baseSchemaPath);
		JsonNode newSchema = TestUtility.getJsonNodeFromFile(newSchemaPath);
		JsonNode baseDef = new ObjectMapper().createObjectNode();
		JsonNode newDef =  new ObjectMapper().createObjectNode();

		SchemaHandlerVO newSchemaDiff = new SchemaHandlerVO(baseSchema, newSchema, baseDef, newDef, SchemaValidationType.PATCH);

		return newSchemaDiff;
	}
}
