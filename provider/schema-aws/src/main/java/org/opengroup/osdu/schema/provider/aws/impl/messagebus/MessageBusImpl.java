/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opengroup.osdu.schema.provider.aws.impl.messagebus;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.opengroup.osdu.core.aws.sns.AmazonSNSConfig;
import org.opengroup.osdu.core.aws.sns.PublishRequestBuilder;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.provider.aws.impl.messagebus.model.SchemaPubSubMessage;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;

@Component
public class MessageBusImpl implements IMessageBus {

    private String amazonSNSTopic;
    private final String schemaSNSTopic = "schema-sns-topic-arn";

    private AmazonSNS snsClient;

    @Value("${OSDU_TOPIC}")
    private String osduSchemaTopic;

    @Value("${AWS.REGION}")
    private String amazonSNSRegion;

    @Autowired
    private ITenantFactory tenantFactory;

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private JaxRsDpsLog logger;

    @PostConstruct
    public void init() throws K8sParameterNotFoundException {
        K8sLocalParameterProvider k8sLocalParameterProvider = new K8sLocalParameterProvider();
        snsClient = new AmazonSNSConfig(amazonSNSRegion).AmazonSNS();

        amazonSNSTopic = k8sLocalParameterProvider.getParameterAsString(schemaSNSTopic);
    }

    @Override
    public void publishMessage(String schemaId, String eventType) {
        try {
            publishSchemaEvent(schemaId, eventType, headers);
        } catch (Exception ex) {
            logger.warning(SchemaConstants.SCHEMA_NOTIFICATION_FAILED, ex);
        }
    }

    @Override
    public void publishMessageForSystemSchema(String schemaId, String eventType) {
        try {
            // Publish the event for all the tenants.
            List<String> privateTenantList = tenantFactory.listTenantInfo().stream().map(TenantInfo::getName).collect(Collectors.toList());

            for (String tenant : privateTenantList) {
                DpsHeaders newHeaders = createTenantHeaders(tenant);
                publishSchemaEvent(schemaId, eventType, newHeaders);
            }

        } catch (Exception ex) {
            logger.warning(SchemaConstants.SYSTEM_SCHEMA_NOTIFICATION_FAILED, ex);
        }
    }

    private DpsHeaders createTenantHeaders(String tenant) {
        HashMap<String, String> headersMap = new HashMap<>();
        headersMap.put(DpsHeaders.ACCOUNT_ID, tenant);
        headersMap.put(DpsHeaders.DATA_PARTITION_ID, tenant);
        headersMap.put(DpsHeaders.CORRELATION_ID, headers.getCorrelationId());
        return DpsHeaders.createFromMap(headersMap);
    }

    private void publishSchemaEvent(String schemaId, String eventType, DpsHeaders headers) {
        PublishRequestBuilder<SchemaPubSubMessage> publishRequestBuilder = new PublishRequestBuilder<>();

        if (headers != null) {
            publishRequestBuilder.setGeneralParametersFromHeaders(headers);
        }

        PublishRequest publishRequest =
                        publishRequestBuilder.generatePublishRequest(osduSchemaTopic, amazonSNSTopic, new SchemaPubSubMessage(schemaId, eventType));

        snsClient.publish(publishRequest);
        logger.info(String.format("Message is published to OSDU topic: %s SNS topic: %s to SNS region: %s", osduSchemaTopic, amazonSNSTopic,
                        amazonSNSRegion));
    }

}
