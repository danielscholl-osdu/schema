package org.opengroup.osdu.schema.provider.aws.impl.messagebus;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageBusImpl implements IMessageBus{

	@Autowired
	private JaxRsDpsLog logger;

	@Override
	public void publishMessage(String schemaId, String eventType) {
		// TODO Auto-generated method stub
		logger.warning("publish message not implemented yet");

	}

	@Override
	public void publishMessageForSystemSchema(String schemaId, String eventType) {
		// TODO Auto-generated method stub
		logger.warning("publish message not implemented yet");
	}

}
