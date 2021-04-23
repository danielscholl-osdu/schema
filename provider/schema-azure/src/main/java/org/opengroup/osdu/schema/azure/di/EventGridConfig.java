package org.opengroup.osdu.schema.azure.di;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventGridConfig {


	public boolean isPublishToEventGridEnabled() {
		return publishToEventGridEnabled;
	}

	public String getCustomTopicName() {
		return eventGridCustomTopic;
	}

	private boolean publishToEventGridEnabled;

	private String eventGridCustomTopic;


	public EventGridConfig(@Value("#{new Boolean('${azure.publishToEventGrid:false}')}") boolean publish,
			@Value("#{new String('${azure.eventGridTopic:schema-change-alert}')}") String topicName) {
		if (publish) {
            if ((topicName.isEmpty())) {
                throw new RuntimeException("Missing EventGrid Configuration");
            }
        }
		
		this.publishToEventGridEnabled = publish;
		this.eventGridCustomTopic = topicName;
	}

}
