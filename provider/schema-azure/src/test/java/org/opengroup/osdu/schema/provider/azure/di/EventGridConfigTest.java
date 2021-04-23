package org.opengroup.osdu.schema.provider.azure.di;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.schema.azure.di.EventGridConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class EventGridConfigTest {

    private static String VALID_TOPIC_NAME = "topicname";
    private static String INVALID_TOPIC_NAME = "";

    @Test
    public void configurationValidationTests() {

        // Positive Case
        EventGridConfig eventGridConfig = new EventGridConfig(true, VALID_TOPIC_NAME);
        assertEquals(VALID_TOPIC_NAME, eventGridConfig.getCustomTopicName());

        // Negative Cases
        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class,
                () -> new EventGridConfig(true, INVALID_TOPIC_NAME));
        assertEquals("Missing EventGrid Configuration", runtimeException.getMessage());

    }
}