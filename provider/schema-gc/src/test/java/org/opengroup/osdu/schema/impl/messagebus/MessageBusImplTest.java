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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmMessage;
import org.opengroup.osdu.core.gcp.oqm.model.OqmTopic;
import org.opengroup.osdu.schema.configuration.EventMessagingPropertiesConfig;
import org.opengroup.osdu.schema.destination.provider.impl.OqmDestinationProvider;
import org.opengroup.osdu.schema.logging.AuditLogger;

@RunWith(MockitoJUnitRunner.class)
public class MessageBusImplTest {

  private static final String SCHEMA_ID = "schemaId";
  private static final String EVENT_TYPE = "eventType";
  private static final String TENANT_NAME = "tenantName";
  private static final String DATA_PARTITION_ID = "partitionId";
  private static final String CORRELATION_ID = "correlationId";

  @Mock
  private OqmTopic oqmTopic;

  @Mock
  private OqmDriver driver;

  @Mock
  private TenantInfo tenantInfo;

  @Mock
  private DpsHeaders headers;

  @Mock
  private OqmDestinationProvider destinationProvider;

  @Mock
  private EventMessagingPropertiesConfig eventMessagingPropertiesConfig;

  @Mock
  private JaxRsDpsLog logger;

  @Mock
  private AuditLogger auditLogger;

  @InjectMocks
  private MessageBusImpl sut;

  @Test
  public void shouldNot_publishEventMessage_WhenFlagIsFalse() {
    when(this.eventMessagingPropertiesConfig.isMessagingEnabled()).thenReturn(false);

    this.sut.publishMessage(SCHEMA_ID, EVENT_TYPE);

    verify(this.driver, never()).publish(any(OqmMessage.class), any(), any());
  }

  @Test
  public void should_publishEventMessage_WhenFlagIsTrue() {
    when(this.eventMessagingPropertiesConfig.isMessagingEnabled()).thenReturn(true);
    when(this.tenantInfo.getName()).thenReturn(TENANT_NAME);
    HashMap<String, String> headersMap = new HashMap<>();
    headersMap.put(DpsHeaders.DATA_PARTITION_ID, DATA_PARTITION_ID);
    headersMap.put(DpsHeaders.CORRELATION_ID, CORRELATION_ID);
    when(this.headers.getHeaders()).thenReturn(headersMap);

    this.sut.publishMessage(SCHEMA_ID, EVENT_TYPE);

    verify(this.driver, times(1)).publish(any(OqmMessage.class), any(), any());
  }
}
