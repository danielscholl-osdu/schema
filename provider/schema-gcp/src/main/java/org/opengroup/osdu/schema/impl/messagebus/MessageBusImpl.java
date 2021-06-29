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

package org.opengroup.osdu.schema.impl.messagebus;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PubsubMessage.Builder;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.schema.configuration.EventMessagingPropertiesConfig;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.impl.messagebus.model.SchemaPubSubInfo;
import org.opengroup.osdu.schema.logging.AuditLogger;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.threeten.bp.Duration;

@Component
public class MessageBusImpl implements IMessageBus {

  private static final RetrySettings RETRY_SETTINGS = RetrySettings.newBuilder()
      .setTotalTimeout(Duration.ofSeconds(10))
      .setInitialRetryDelay(Duration.ofMillis(5))
      .setRetryDelayMultiplier(2)
      .setMaxRetryDelay(Duration.ofSeconds(3))
      .setInitialRpcTimeout(Duration.ofSeconds(10))
      .setRpcTimeoutMultiplier(2)
      .setMaxRpcTimeout(Duration.ofSeconds(10))
      .build();


  private Publisher publisher;

  @Autowired
  private TenantInfo tenantInfo;

  @Autowired
  private DpsHeaders headers;

  @Autowired
  private EventMessagingPropertiesConfig eventMessagingPropertiesConfig;

  @Autowired
  private JaxRsDpsLog logger;

  @Autowired
  private AuditLogger auditLogger;

  @Override
  public void publishMessage(String schemaId, String eventType) {
    if (this.eventMessagingPropertiesConfig.isMessagingEnabled()) {
      this.logger.info(String.format("Generating event of type %s", eventType));

      if (Objects.isNull(this.publisher)) {
        try {
          this.publisher = Publisher.newBuilder(
              ProjectTopicName.newBuilder()
                  .setProject(this.tenantInfo.getProjectId())
                  .setTopic(this.eventMessagingPropertiesConfig.getTopicName()).build())
              .setRetrySettings(RETRY_SETTINGS).build();
        } catch (IOException e) {
          this.logger.info(SchemaConstants.SCHEMA_NOTIFICATION_FAILED);
          this.auditLogger.schemaNotificationFailure(Collections.singletonList(schemaId));
          throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal error",
              "A fatal internal error has occurred.", e);
        }
      }

      PubsubMessage message = createMessage(schemaId, eventType);
      this.publisher.publish(message);
      this.auditLogger.schemaNotificationSuccess(Collections.singletonList(schemaId));
    } else {
      this.logger.info(SchemaConstants.SCHEMA_NOTIFICATION_IS_DISABLED);
    }
  }

  private PubsubMessage createMessage(String schemaId, String eventType) {
    SchemaPubSubInfo schemaPubSubMsg = new SchemaPubSubInfo(schemaId, eventType);

    String json = new Gson().toJson(schemaPubSubMsg);
    ByteString data = ByteString.copyFromUtf8(json);

    Builder messageBuilder = PubsubMessage.newBuilder();
    messageBuilder.putAttributes(DpsHeaders.ACCOUNT_ID, this.tenantInfo.getName());
    messageBuilder.putAttributes(DpsHeaders.DATA_PARTITION_ID,
        this.headers.getPartitionIdWithFallbackToAccountId());
    this.headers.addCorrelationIdIfMissing();
    messageBuilder.putAttributes(DpsHeaders.CORRELATION_ID, this.headers.getCorrelationId());
    messageBuilder.setData(data);

    return messageBuilder.build();
  }

}
