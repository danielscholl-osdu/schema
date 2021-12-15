package org.opengroup.osdu.schema.validation.version.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.util.JSONUtil;
import org.opengroup.osdu.schema.validation.version.SchemaValidationType;
import org.opengroup.osdu.schema.validation.version.handler.common.AdditionalPropertiesHandler;
import org.opengroup.osdu.schema.validation.version.model.SchemaBreakingChanges;
import org.opengroup.osdu.schema.validation.version.model.SchemaHandlerVO;
import org.opengroup.osdu.schema.validation.version.model.SchemaPatch;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaValidationManagerTest {

	@Mock
	private JaxRsDpsLog log;

	@Mock
	private SchemaValidationChainOfHandlers handlers;


	@Mock
	private JSONUtil jsonUtil;

	@InjectMocks
	SchemaValidationManager schemaValidationManager;

	@Test
	public void testInitiateValidationProcess_BreakingChanges() throws ApplicationException, IOException {
		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();

		SchemaValidationHandler handler = new AdditionalPropertiesHandler() ;
		SchemaValidationHandler spyHandler = Mockito.spy(handler);
		SchemaHandlerVO schHandlerVO = getEmptySchemaHandlerVO();
		List<SchemaPatch> patchList =  new ArrayList<>(getMockPatch());

		Mockito.when(handlers.getFirstHandler()).thenReturn(spyHandler);
		Mockito.when(jsonUtil.findJSONDiff(Mockito.any(JsonNode.class), Mockito.any(JsonNode.class))).thenReturn(patchList);
		Mockito.doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				schemaBreakingChanges.add(new SchemaBreakingChanges(patchList.get(0), "breaking changes found"));
				return null;
			}})
		.when(spyHandler).compare( Mockito.anyObject(),  Mockito.anyObject(),  Mockito.anyObject(), Mockito.anyObject());

		schemaValidationManager.initiateValidationProcess(schHandlerVO, schemaBreakingChanges);
		assertEquals(schemaBreakingChanges.size(), 1);
	}

	@Test
	public void testInitiateValidationProcess_NoBreakingChanges() throws ApplicationException, IOException {
		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();

		SchemaValidationHandler handler = new AdditionalPropertiesHandler() ;
		SchemaValidationHandler spyHandler = Mockito.spy(handler);
		SchemaHandlerVO schHandlerVO = getEmptySchemaHandlerVO();
		List<SchemaPatch> patchList =  new ArrayList<>(getMockPatch());

		Mockito.when(handlers.getFirstHandler()).thenReturn(spyHandler);
		Mockito.when(jsonUtil.findJSONDiff(Mockito.any(JsonNode.class), Mockito.any(JsonNode.class))).thenReturn(patchList);
		Mockito.doNothing().when(spyHandler).compare( Mockito.anyObject(),  Mockito.anyObject(),  Mockito.anyObject(), Mockito.anyObject());

		schemaValidationManager.initiateValidationProcess(schHandlerVO, schemaBreakingChanges);
		assertEquals(schemaBreakingChanges.size(), 0);
	}

	private List<SchemaPatch> getMockPatch(){

		List<SchemaPatch> patches = new ArrayList<SchemaPatch>();
		SchemaPatch patch = new SchemaPatch();
		patches.add(patch);

		return patches;
	}

	private SchemaHandlerVO getEmptySchemaHandlerVO() throws IOException{
		JsonNode cleanSource = new ObjectMapper().createObjectNode();
		JsonNode sourceDefinitions = new ObjectMapper().createObjectNode();
		JsonNode cleanTarget = new ObjectMapper().createObjectNode();
		JsonNode targetDefinitions =  new ObjectMapper().createObjectNode();

		SchemaHandlerVO newSchemaDiff = new SchemaHandlerVO(cleanSource, cleanTarget, sourceDefinitions, targetDefinitions, SchemaValidationType.MINOR);

		return newSchemaDiff;
	}

	@Test(expected = ApplicationException.class)
	public void testInitiateValidationProcess_Exception() throws ApplicationException, IOException {
		List<SchemaBreakingChanges> schemaBreakingChanges = new ArrayList<>();
		Mockito.when(handlers.getFirstHandler()).thenReturn( new AdditionalPropertiesHandler() );
		Mockito.when(jsonUtil.findJSONDiff(Mockito.any(JsonNode.class), Mockito.any(JsonNode.class))).thenThrow(JsonProcessingException.class);

		schemaValidationManager.initiateValidationProcess(getEmptySchemaHandlerVO(), schemaBreakingChanges);
	}

}
