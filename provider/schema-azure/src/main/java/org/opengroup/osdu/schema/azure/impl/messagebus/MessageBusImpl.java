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
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.opengroup.osdu.schema.service.IAuthorityService;
import org.opengroup.osdu.schema.service.IEntityTypeService;
import org.opengroup.osdu.schema.service.ISourceService;
import org.opengroup.osdu.schema.util.SchemaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.microsoft.azure.eventgrid.models.EventGridEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MessageBusImpl implements IMessageBus {

	@Autowired
	private EventGridTopicStore eventGridTopicStore;

	@Autowired
	private JaxRsDpsLog logger;

	@Autowired
	private EventGridConfig eventGridConfig;

	private final AuditLogger auditLogger;

	final JaxRsDpsLog log;


	private final static String EVENT_DATA_VERSION = "1.0";

	@Override
	public void publishMessage(DpsHeaders headers, String schemaId, String eventType) {
		if (eventGridConfig.isPublishingToEventGridEnabled()) {
			logger.info("Generating event of type {}",eventType);
			try {
				publishToEventGrid(headers, schemaId, eventType);
				auditLogger.schemaNotificationSuccess(Collections.singletonList(schemaId));
			}catch (AppException ex) {
				
				//We do not want to fail schema creation if notification delivery has failed, hence just logging the exception
				auditLogger.schemaNotificationFailure(Collections.singletonList(schemaId));
				log.warning(SchemaConstants.SCHEMA_NOTIFICATION_FAILED);
			}

		}else {
			logger.info("Schema event notification is turned off.");
		}
	}

	private void publishToEventGrid(DpsHeaders headers, String schemaId, String eventType) {

		List<EventGridEvent> eventsList = new ArrayList<>();

		HashMap<String, Object> data = new HashMap<>();
		data.put("id", schemaId);
		data.put(DpsHeaders.ACCOUNT_ID, headers.getPartitionIdWithFallbackToAccountId());
		data.put(DpsHeaders.DATA_PARTITION_ID, headers.getPartitionIdWithFallbackToAccountId());
		data.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
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
