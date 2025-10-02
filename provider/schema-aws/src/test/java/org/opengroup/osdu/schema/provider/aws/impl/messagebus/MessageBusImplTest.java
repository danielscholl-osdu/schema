// Copyright © Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.schema.provider.aws.impl.messagebus;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.sns.PublishRequestBuilder;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.provider.aws.impl.messagebus.model.SchemaPubSubMessage;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@ExtendWith(MockitoExtension.class)
class MessageBusImplTest {

    private MessageBusImpl messageBusImpl;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private SnsClient snsClient;

    @Mock
    private ITenantFactory tenantFactory;

    @Mock
    private DpsHeaders headers;
    
    private final String osduSchemaTopic = "test-topic";
    private final String amazonSNSRegion = "us-west-2";
    private final String amazonSNSTopic = "test-topic-arn";

    @BeforeEach
    void setup() {
        messageBusImpl = new MessageBusImpl(osduSchemaTopic, amazonSNSRegion, tenantFactory, headers, logger);
        // Set the mocked SNS client and topic ARN using reflection
        try {
            java.lang.reflect.Field snsClientField = MessageBusImpl.class.getDeclaredField("snsClient");
            snsClientField.setAccessible(true);
            snsClientField.set(messageBusImpl, snsClient);
            
            java.lang.reflect.Field topicField = MessageBusImpl.class.getDeclaredField("amazonSNSTopic");
            topicField.setAccessible(true);
            topicField.set(messageBusImpl, amazonSNSTopic);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test", e);
        }
    }

    @Test
    void publishMessagePublishesMessages() {
        try (MockedConstruction<PublishRequestBuilder> publishRequestBuilder =
                mockConstruction(PublishRequestBuilder.class, (mock, context) -> {
                    when(mock.generatePublishRequest(anyString(), anyString(), any(SchemaPubSubMessage.class)))
                        .thenReturn(PublishRequest.builder().build());
                })) {

            messageBusImpl.publishMessage("schemaId", "eventType_create");
            verify(snsClient, times(1)).publish(any(PublishRequest.class));
        }
    }

    @Test
    void publishMessageForSystemSchemaPublishesMessages() {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setName("testTenant");
        tenantInfo.setDataPartitionId("testPartition");

        List<TenantInfo> tenantInfoList = Arrays.asList(tenantInfo);

        when(tenantFactory.listTenantInfo()).thenReturn(tenantInfoList);
        when(headers.getCorrelationId()).thenReturn("correlationId");

        try (MockedConstruction<PublishRequestBuilder> publishRequestBuilder =
                mockConstruction(PublishRequestBuilder.class, (mock, context) -> {
                    when(mock.generatePublishRequest(anyString(), anyString(), any(SchemaPubSubMessage.class)))
                        .thenReturn(PublishRequest.builder().build());
                })) {

            messageBusImpl.publishMessageForSystemSchema("schemaId", "eventType_create");
            verify(snsClient, times(1)).publish(any(PublishRequest.class));
        }
    }
}
