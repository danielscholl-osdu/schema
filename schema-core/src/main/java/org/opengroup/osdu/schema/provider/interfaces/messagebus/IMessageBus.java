package org.opengroup.osdu.schema.provider.interfaces.messagebus;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;

public interface IMessageBus {
	void publishMessage(DpsHeaders headers, String schemaId, String eventType);
}