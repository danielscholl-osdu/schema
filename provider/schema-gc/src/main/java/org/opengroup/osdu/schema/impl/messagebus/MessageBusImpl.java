/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.schema.impl.messagebus;

import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriverRuntimeException;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.opengroup.osdu.schema.configuration.EventMessagingPropertiesConfig;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.impl.messagebus.model.SchemaPubSubInfo;
import org.opengroup.osdu.schema.logging.AuditLogger;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageBusImpl implements IMessageBus {

  private final OqmDriver driver;
  private final DestinationProvider<OqmDestination> destinationProvider;
  private final TenantInfo tenantInfo;

  private final DpsHeaders headers;
  private final EventMessagingPropertiesConfig eventMessagingPropertiesConfig;
  private final JaxRsDpsLog logger;
  private final AuditLogger auditLogger;

  private OqmTopic oqmTopic = null;

  @PostConstruct
  void postConstruct() {
    oqmTopic = OqmTopic.builder().name(eventMessagingPropertiesConfig.getTopicName()).build();
  }

  @Override
  public void publishMessage(String schemaId, String eventType) {
    if (this.eventMessagingPropertiesConfig.isMessagingEnabled()) {
      this.logger.info(String.format("Generating event of type %s", eventType));

      OqmDestination destination = destinationProvider.getDestination(headers.getPartitionId());
      healthCheckTopic(schemaId, false);

      OqmMessage message = createMessage(schemaId, eventType);
      this.driver.publish(message, oqmTopic, destination);
      this.auditLogger.schemaNotificationSuccess(Collections.singletonList(schemaId));
    } else {
      this.logger.info(SchemaConstants.SCHEMA_NOTIFICATION_IS_DISABLED);
    }
  }

  @Override
  public void publishMessageForSystemSchema(String schemaId, String eventType) {
    if (this.eventMessagingPropertiesConfig.isMessagingEnabled()) {
      OqmDestination destination = destinationProvider.getDestination(headers.getPartitionId());

      healthCheckTopic(schemaId, true);

      OqmMessage message = createMessage(schemaId, eventType);
      this.driver.publish(message, oqmTopic, destination);
      this.auditLogger.schemaNotificationSuccess(Collections.singletonList(schemaId));
    } else {
      this.logger.info(SchemaConstants.SCHEMA_NOTIFICATION_IS_DISABLED);
    }
  }

  private void healthCheckTopic(String schemaId, boolean isSystemSchema) {
    if (Objects.isNull(this.oqmTopic)) {
      try {
        this.oqmTopic = OqmTopic.builder().name(eventMessagingPropertiesConfig.getTopicName()).build();
      } catch (OqmDriverRuntimeException e) {
        String errorMessage = isSystemSchema ? SchemaConstants.SYSTEM_SCHEMA_NOTIFICATION_FAILED :
                SchemaConstants.SCHEMA_NOTIFICATION_FAILED;
        this.logger.info(errorMessage);
        this.auditLogger.schemaNotificationFailure(Collections.singletonList(schemaId));
        throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal error", errorMessage, e);
      }
    }
  }

  private OqmMessage createMessage(String schemaId, String eventType) {
    SchemaPubSubInfo schemaPubSubMsg = new SchemaPubSubInfo(schemaId, eventType);

    String data = new Gson().toJson(Collections.singletonList(schemaPubSubMsg));

    this.headers.addCorrelationIdIfMissing();
    Map<String, String> headersMap = headers.getHeaders();
    headersMap.put(DpsHeaders.ACCOUNT_ID, this.tenantInfo.getName());
    headersMap.remove(DpsHeaders.AUTHORIZATION);

    Map<String, String> attributes = new HashMap<>(headersMap);
    return OqmMessage.builder()
        .data(data)
        .attributes(attributes)
        .build();
  }

}
