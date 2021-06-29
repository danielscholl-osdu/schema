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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.schema.configuration.EventMessagingPropertiesConfig;
import org.opengroup.osdu.schema.logging.AuditLogger;

@RunWith(MockitoJUnitRunner.class)
public class MessageBusImplTest {

  private static final String SCHEMA_ID = "schemaId";
  private static final String EVENT_TYPE = "eventType";
  private static final String TENANT_NAME = "tenantName";
  private static final String DATA_PARTITION_ID = "partitionId";
  private static final String CORRELATION_ID = "correlationId";

  @Mock
  private Publisher publisher;

  @Mock
  private TenantInfo tenantInfo;

  @Mock
  private DpsHeaders headers;

  @Mock
  private EventMessagingPropertiesConfig eventMessagingPropertiesConfig;

  @Mock
  private JaxRsDpsLog logger;

  @Mock
  private AuditLogger auditLogger;

  @InjectMocks
  private MessageBusImpl messageBusImpl;

  @Test
  public void shouldNot_publishEventMessage_WhenFlagIsFalse() {
    when(this.eventMessagingPropertiesConfig.isMessagingEnabled()).thenReturn(false);
    this.messageBusImpl.publishMessage(SCHEMA_ID, EVENT_TYPE);
    verify(this.publisher, times(0)).publish(any());
  }

  @Test
  public void should_publishEventMessage_WhenFlagIsTrue() {
    when(this.eventMessagingPropertiesConfig.isMessagingEnabled()).thenReturn(true);
    when(this.tenantInfo.getName()).thenReturn(TENANT_NAME);
    when(this.headers.getPartitionIdWithFallbackToAccountId()).thenReturn(DATA_PARTITION_ID);
    doNothing().when(this.headers).addCorrelationIdIfMissing();
    when(this.headers.getCorrelationId()).thenReturn(CORRELATION_ID);

    this.messageBusImpl.publishMessage(SCHEMA_ID, EVENT_TYPE);

    verify(this.publisher, times(1)).publish(any(PubsubMessage.class));
  }
}
