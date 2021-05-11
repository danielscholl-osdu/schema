/*
 * Copyright 2021 Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.schema.azure.impl.messagebus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.joda.time.DateTime;
import org.opengroup.osdu.azure.eventgrid.EventGridTopicStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.di.EventGridConfig;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.logging.AuditLogger;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.microsoft.azure.eventgrid.models.EventGridEvent;

@Component
public class MessageBusImpl implements IMessageBus {

	@Autowired
	private EventGridTopicStore eventGridTopicStore;

	@Autowired
	private JaxRsDpsLog logger;

	@Autowired
	private EventGridConfig eventGridConfig;

	@Autowired
	private AuditLogger auditLogger;
	
	@Autowired
	DpsHeaders headers;


	private final static String EVENT_DATA_VERSION = "1.0";

	@Override
	public void publishMessage(String schemaId, String eventType) {
		if (eventGridConfig.isEventGridEnabled()) {
			logger.info("Generating event of type {}",eventType);
			try {
				publishToEventGrid(schemaId, eventType);
				auditLogger.schemaNotificationSuccess(Collections.singletonList(schemaId));
			}catch (AppException ex) {
				
				//We do not want to fail schema creation if notification delivery has failed, hence just logging the exception
				auditLogger.schemaNotificationFailure(Collections.singletonList(schemaId));
				logger.warning(SchemaConstants.SCHEMA_NOTIFICATION_FAILED);
			}

		}else {
			logger.info(SchemaConstants.SCHEMA_NOTIFICATION_IS_DISABLED);
		}
	}

	private void publishToEventGrid(String schemaId, String eventType) {

		List<EventGridEvent> eventsList = new ArrayList<>();
		
		HashMap<String, Object> data = new HashMap<>();
		data.put(SchemaConstants.KIND, schemaId);
		data.put(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
		data.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionId());
		data.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
		
		HashMap<String, Object> pubsubMessage = new HashMap<>();
		pubsubMessage.put("data", data);
		
		String messageId = UUID.randomUUID().toString();
		//EventGridEvent supports array of messages to be triggered in a batch but at present we do not support 
		//schema creation in bulk so generating one event at a time.
		eventsList.add(new EventGridEvent(
				messageId,
				SchemaConstants.EVENT_SUBJECT,
				data,
				eventType,
				DateTime.now(),
				EVENT_DATA_VERSION
				));
		
		eventGridTopicStore.publishToEventGridTopic(headers.getPartitionId(), eventGridConfig.getCustomTopicName(), eventsList);
		logger.info("Event generated: " + messageId);
	}

}
