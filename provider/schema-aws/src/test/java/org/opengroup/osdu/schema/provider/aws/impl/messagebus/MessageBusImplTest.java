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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.sns.PublishRequestBuilder;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;

@RunWith(MockitoJUnitRunner.class)
public class MessageBusImplTest {

    @InjectMocks
    private MessageBusImpl messageBusImpl;

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private K8sLocalParameterProvider k8sLocalParameterProvider;

    @Mock
    private AmazonSNS snsClient;

    @Test
    public void publishMessagePublishesMessages() {

        try (MockedConstruction<PublishRequestBuilder> k8sParameterProvider =
                        Mockito.mockConstruction(PublishRequestBuilder.class, (mock, context) -> {
                            when(mock.generatePublishRequest(anyString(), anyString(), any())).thenReturn(new PublishRequest());
                        })) {


            messageBusImpl.publishMessage("schemaId", "eventType_create");
            verify(snsClient, times(1)).publish(any());
        }
    }

    @Test
    public void publishMessageForSystemSchemaPublishesMessages() {

        try (MockedConstruction<PublishRequestBuilder> k8sParameterProvider =
                        Mockito.mockConstruction(PublishRequestBuilder.class, (mock, context) -> {
                            when(mock.generatePublishRequest(anyString(), anyString(), any())).thenReturn(new PublishRequest());
                        })) {


            messageBusImpl.publishMessage("schemaId", "eventType_create");
            verify(snsClient, times(1)).publish(any());
        }
    }
}
