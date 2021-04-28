package org.opengroup.osdu.schema.provider.interfaces.messagebus;

public interface IMessageBus {
	void publishMessage(String schemaId, String eventType);
}