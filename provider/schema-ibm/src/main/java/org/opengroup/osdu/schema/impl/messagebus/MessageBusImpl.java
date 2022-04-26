package org.opengroup.osdu.schema.impl.messagebus;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.jms.TopicSession;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.ibm.messagebus.IMessageFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.impl.messagebus.model.SchemaPubSubInfo;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class MessageBusImpl implements IMessageBus{

	@Autowired
	private JaxRsDpsLog logger;
	
	@Inject
    private DpsHeaders headers;
	
	@Autowired
	public JmsTemplate jmsTemplate;
	
	@Value("${schema.update.event.topic:schema-event-default-topic}")
	private String SCHEMA_UPDATE_EVENT_TOPIC;

	@Value("${shared.tenant.name:common}")
	private String sharedTenant;
	
	Map<String, String> message = new HashMap<>();
	Gson gson = new Gson();
	
	private static final String FAILED_TO_PUBLISH_EVENT = "Failed to publish event. ";
	
	String correlationId = null;
	String dataPartitionId = null;
	
	@Override
	public void publishMessage(String schemaId, String eventType) {
	    SchemaPubSubInfo schemaPubSubMsg = new SchemaPubSubInfo(schemaId, eventType);
	    String json = new Gson().toJson(schemaPubSubMsg);
	    dataPartitionId = headers.getPartitionIdWithFallbackToAccountId();
	    
		message.put("data", json);
		message.put(DpsHeaders.DATA_PARTITION_ID, dataPartitionId);
		headers.addCorrelationIdIfMissing();
		message.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
		
		String topicNameWithPrefix = dataPartitionId + "-" + SCHEMA_UPDATE_EVENT_TOPIC;

		sendMessageToTopic(topicNameWithPrefix, gson.toJson(message), -1L);
	}

	@Override
	public void publishMessageForSystemSchema(String schemaId, String eventType) {
		this.updateDataPartitionId();
		this.publishMessage(schemaId, eventType);
	}
	
	public String createTopic(String topicNameWithPrefix) {
		try {
			ConnectionFactory connectionFactory = jmsTemplate.getConnectionFactory();
			Connection connection = connectionFactory.createConnection();
			TopicSession session = (TopicSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			session.createTopic(topicNameWithPrefix);
			session.close();
		} catch (Exception e) {
			logger.error("Failed to create topic", e);
			throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "could not create address" + topicNameWithPrefix,
					e.getMessage(), e);
		}
		return topicNameWithPrefix;
	}

	public void sendMessageToTopic(String topicName, String msg, Long delay) {
		try {
			jmsTemplate.setDeliveryDelay(delay);
			jmsTemplate.setPubSubDomain(true);
			
			if(null==dataPartitionId) {
				dataPartitionId=topicName.split("\\-")[0];
			}
			
			jmsTemplate.convertAndSend(topicName, msg, m -> {
				logger.info("setting custom JMS headers before sending");
				m.setStringProperty("data_partition_id", dataPartitionId); 
				return m;});
			logger.info("[x] Sent '" + msg + "' to topic [" + topicName + "] with delay " + delay);
		} catch (JmsException e) {
			logger.error(FAILED_TO_PUBLISH_EVENT +" topic name [" + topicName + "]");
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}
	}

	protected void updateDataPartitionId() {
		headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
	}

}