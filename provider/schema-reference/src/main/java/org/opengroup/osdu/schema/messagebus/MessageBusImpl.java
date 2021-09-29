/*
  Copyright 2021 Google LLC
  Copyright 2021 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.opengroup.osdu.schema.messagebus;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.schema.config.RabbitMqConfigProperties;
import org.opengroup.osdu.schema.factory.RabbitMQFactoryImpl;
import org.opengroup.osdu.schema.logging.AuditLogger;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageBusImpl implements IMessageBus {

    private final RabbitMQFactoryImpl rabbitMQFactory;
    private final RabbitMqConfigProperties mqConfigProperties;
    private final AuditLogger auditLogger;

    @Override
    public void publishMessage(String schemaId, String eventType) {
        log.info(String.format("Generating event of type %s", eventType));
        SchemaPubSubInfo schemaPubSubMsg = new SchemaPubSubInfo(schemaId, eventType);
        String message = new Gson().toJson(schemaPubSubMsg);
        Channel client = rabbitMQFactory.getClient();
        String queueNameWithPrefix = mqConfigProperties.getStatusQueueName();
        try {
            client.basicPublish("", queueNameWithPrefix, null, message.getBytes());
            log.info(String.format("[x] Sent '%s' to queue [%s]", message, queueNameWithPrefix));
            this.auditLogger.schemaNotificationSuccess(Collections.singletonList(schemaId));
        } catch (IOException e) {
            log.error(String.format("Unable to publish message to [%s]", queueNameWithPrefix));
            log.error(e.getMessage(), e);
            this.auditLogger.schemaNotificationFailure(Collections.singletonList(schemaId));
        }
    }

}
