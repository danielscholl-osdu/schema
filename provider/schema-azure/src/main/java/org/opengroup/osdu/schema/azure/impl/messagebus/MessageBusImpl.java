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
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.opengroup.osdu.azure.eventgrid.EventGridTopicStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.azure.di.EventGridConfig;
import org.opengroup.osdu.schema.azure.di.SystemResourceConfig;
import org.opengroup.osdu.schema.azure.impl.messagebus.model.SchemaPubSubInfo;
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
	@Autowired
	private ITenantFactory tenantFactory;

	@Autowired
	SystemResourceConfig systemResourceConfig;

	private final static String EVENT_DATA_VERSION = "1.0";

	@Override
	public void publishMessage(String schemaId, String eventType) {
		// This if block will be removed once schema-core starts consuming *System* methods.
		if (systemResourceConfig.getSharedTenant().equalsIgnoreCase(headers.getPartitionId())) {
			this.publishMessageForSystemSchema(schemaId, eventType);
			return;
		}

		if (eventGridConfig.isEventGridEnabled()) {
			logger.info("Generating event of type {}",eventType);
			try {
				publishToEventGrid(schemaId, eventType, headers.getPartitionId());
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

	/**
	 * Method to publish schema create notification for system schemas.
	 * @param schemaId
	 * @param eventType
	 */
	@Override
	public void publishMessageForSystemSchema(String schemaId, String eventType) {
		if (eventGridConfig.isEventGridEnabled()) {
			logger.info("Generating event of type {}",eventType);
			try {
				// Publish the event for all the tenants.
				List<String> privateTenantList = tenantFactory.listTenantInfo().stream().map(TenantInfo::getName)
						.collect(Collectors.toList());

				for (String tenant : privateTenantList) {
					publishToEventGrid(schemaId, eventType, tenant);
				}

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

	private void publishToEventGrid(String schemaId, String eventType, String dataPartitionId) {

		String messageId = UUID.randomUUID().toString();
		SchemaPubSubInfo[] schemaPubSubMsgs = new SchemaPubSubInfo [1];
		schemaPubSubMsgs[0]=new SchemaPubSubInfo(schemaId,eventType);
		List<EventGridEvent> eventsList = new ArrayList<>();
		HashMap<String, Object> message = new HashMap<>();
		message.put("data", schemaPubSubMsgs);
		message.put(DpsHeaders.ACCOUNT_ID, dataPartitionId);
		message.put(DpsHeaders.DATA_PARTITION_ID, dataPartitionId);
		message.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
		
		//EventGridEvent supports array of messages to be triggered in a batch but at present we do not support 
		//schema creation in bulk so generating one event at a time.
		EventGridEvent eventGridEvent = new EventGridEvent(
				messageId,
				SchemaConstants.EVENT_SUBJECT,
				message,
				eventType,
				DateTime.now(),
				EVENT_DATA_VERSION
				);
		eventsList.add(eventGridEvent);
		logger.info("Schema event created: " + messageId);
		eventGridTopicStore.publishToEventGridTopic(dataPartitionId, eventGridConfig.getCustomTopicName(), eventsList);
		logger.info("Schema event generated successfully");
	}

}
