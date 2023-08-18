package org.opengroup.osdu.schema.provider.aws.impl.messagebus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;

@RunWith(MockitoJUnitRunner.class)
public class MessageBusImplTest {

	@InjectMocks
	private MessageBusImpl messageBusImpl;

	@Mock
	private JaxRsDpsLog logger;

	@Test
	public void publishMessage_Success() {
		String schemaId = "schemaId";
		String eventType = "eventType";

		assertDoesNotThrow(() -> {
			messageBusImpl.publishMessage(schemaId, eventType);
		});
	}

	@Test
	public void publishMessageForSystemSchema_Success() {
		String schemaId = "schemaId";
		String eventType = "eventType";

		assertDoesNotThrow(() -> {
			messageBusImpl.publishMessageForSystemSchema(schemaId, eventType);
		});
	}
}
