package org.opengroup.osdu.schema.azure.di;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventGridConfig {

    public boolean isPublishingToEventGridEnabled() {
        return publishToEventGridEnabled;
    }
    
    public String getCustomTopicName() {
        return eventGridCustomTopic;
    }

    @Value("#{new Boolean('${azure.publishToEventGrid:false}')}")
    private boolean publishToEventGridEnabled;
    
    @Value("#{new String('${azure.eventGridTopic:schema-change-alert}')}")
    private String eventGridCustomTopic;

}