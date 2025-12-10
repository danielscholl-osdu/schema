package org.opengroup.osdu.schema.validation.version.handler;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.validation.version.handler.common.AdditionalPropertiesHandler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaValidationChainOfHandlersTest {

	@Mock
	private List<SchemaValidationHandler> chainOfValidationHandlers;

	@InjectMocks
	SchemaValidationChainOfHandlers schemaValidationChainOfHandlers;

	@Test
	public void testGetFirstHandler() {
		Mockito.when(chainOfValidationHandlers.get(Mockito.anyInt())).thenReturn( new AdditionalPropertiesHandler() );
		assertNotNull(schemaValidationChainOfHandlers.getFirstHandler());
	}
}
