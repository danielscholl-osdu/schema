/*
 * Copyright 2021 Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.schema.azure.impl.messagebus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.eventgrid.EventGridTopicStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.azure.di.EventGridConfig;
import org.opengroup.osdu.schema.azure.di.SystemResourceConfig;
import org.opengroup.osdu.schema.azure.impl.messagebus.model.SchemaPubSubInfo;
import org.opengroup.osdu.schema.logging.AuditLogger;

import com.microsoft.azure.eventgrid.models.EventGridEvent;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

@ExtendWith(MockitoExtension.class)
public class MessageBusImplTest {

    private static final String DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID = "data-partition-account-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String PARTITION_ID = "partition-id";
    private static final String OTHER_TENANT = "other-tenant-id";
    private static final String systemCosmosDBName = "osdu-system-db";
    private static final String sharedTenantId = "common";

    @Mock
    private EventGridTopicStore eventGridTopicStore;
    
    @Mock
    private EventGridConfig eventGridConfig;
    
    @Mock
    private DpsHeaders dpsHeaders;
    
    @Mock
    private JaxRsDpsLog logger;
    
    @Mock
   	private AuditLogger auditLogger;

    @Mock
    private ITenantFactory tenantFactory;

    @Mock
    SystemResourceConfig systemResourceConfig;
    
    @InjectMocks
    private MessageBusImpl messageBusImpl;
    
    @Before
    public void init() throws ServiceBusException, InterruptedException {
        initMocks(this);
        doReturn(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();
        doReturn(PARTITION_ID).when(dpsHeaders).getPartitionId();
        doReturn(CORRELATION_ID).when(dpsHeaders).getCorrelationId();
        Mockito.when(systemResourceConfig.getCosmosDatabase()).thenReturn(systemCosmosDBName);
        Mockito.when(systemResourceConfig.getSharedTenant()).thenReturn(sharedTenantId);
    }

    @Test
    public void should_publishToEventGrid_WhenFlagIsFalse() {
    	//The schema-notification is turned off
    	when(this.eventGridConfig.isEventGridEnabled()).thenReturn(false);
        //Call publish Message
        messageBusImpl.publishMessage("dummy", "dummy");
        //Assert that eventGridTopicStore is not called even once
        verify(this.eventGridTopicStore, times(0)).publishToEventGridTopic(any(), any(), anyList());
    }

    @Test
    public void should_publishToEventGrid_WhenFlagIsFalse_PublicSchemas() {
        Mockito.when(dpsHeaders.getPartitionId()).thenReturn(sharedTenantId);
        //The schema-notification is turned off
        when(this.eventGridConfig.isEventGridEnabled()).thenReturn(false);
        //Call publish Message
        messageBusImpl.publishMessageForSystemSchema("dummy", "dummy");
        messageBusImpl.publishMessage("dummy", "dummy");
        //Assert that eventGridTopicStore is not called even once
        verify(this.eventGridTopicStore, times(0)).publishToEventGridTopic(any(), any(), anyList());
    }
    
    @Test
    public void should_publishToEventGrid_WhenFlagIsTrue() {
    	
    	//The schema-notification is turned on
    	when(this.eventGridConfig.isEventGridEnabled()).thenReturn(true);
    	//The schema-notification is turned off
    	when(this.eventGridConfig.getCustomTopicName()).thenReturn("dummy-topic");
    	//The schema-notification is turned off
    	doNothing().when(this.eventGridTopicStore).publishToEventGridTopic(anyString(), anyString(), anyList());;
    	ArgumentCaptor<ArrayList<EventGridEvent>> captorList = ArgumentCaptor.forClass(ArrayList.class);
    	
    	
    	SchemaPubSubInfo[] schemaPubSubMsgs = new SchemaPubSubInfo [1];
		schemaPubSubMsgs[0]=new SchemaPubSubInfo("dummy","schema_create");
    	
    	HashMap<String, Object> data = new HashMap<>();
		data.put("data", schemaPubSubMsgs);
		data.put(DpsHeaders.ACCOUNT_ID, DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID);
		data.put(DpsHeaders.DATA_PARTITION_ID, PARTITION_ID);
		data.put(DpsHeaders.CORRELATION_ID, CORRELATION_ID);
        
		//Call publish Message
    	messageBusImpl.publishMessage("dummy", "schema_create");
        
    	//Assert that eventGridTopicStore is called once
        verify(this.eventGridTopicStore, times(1)).publishToEventGridTopic(anyString(), anyString(), captorList.capture());
        ArrayList<EventGridEvent> eventGridList = captorList.getValue();
        assertNotNull(eventGridList);
        assertThat(eventGridList.size(), is(equalTo(1)));
        
        HashMap<String, Object> outputData = (HashMap<String, Object>)eventGridList.get(0).data();
        assertEquals(((SchemaPubSubInfo[])outputData.get("data"))[0].getKind(), "dummy");
        assertEquals(((SchemaPubSubInfo[])outputData.get("data"))[0].getOp(), "schema_create");

    }

    @Test
    public void should_publishToEventGrid_WhenFlagIsTrue_PublicSchemas() {

        Mockito.when(dpsHeaders.getPartitionId()).thenReturn(sharedTenantId);
        TenantInfo tenant1 = new TenantInfo();
        tenant1.setName(PARTITION_ID);
        tenant1.setDataPartitionId(PARTITION_ID);
        TenantInfo tenant2 = new TenantInfo();
        tenant2.setName(OTHER_TENANT);
        tenant2.setDataPartitionId(OTHER_TENANT);
        Collection<TenantInfo> tenants = Lists.newArrayList(tenant1, tenant2);
        when(this.tenantFactory.listTenantInfo()).thenReturn(tenants);

        //The schema-notification is turned on
        when(this.eventGridConfig.isEventGridEnabled()).thenReturn(true);
        when(this.eventGridConfig.getCustomTopicName()).thenReturn("dummy-topic");
        doNothing().when(this.eventGridTopicStore).publishToEventGridTopic(anyString(), anyString(), anyList());;
        ArgumentCaptor<ArrayList<EventGridEvent>> captorList = ArgumentCaptor.forClass(ArrayList.class);


        SchemaPubSubInfo[] schemaPubSubMsgs = new SchemaPubSubInfo [1];
        schemaPubSubMsgs[0]=new SchemaPubSubInfo("dummy","schema_create");

        HashMap<String, Object> data = new HashMap<>();
        data.put("data", schemaPubSubMsgs);
        data.put(DpsHeaders.ACCOUNT_ID, PARTITION_ID);
        data.put(DpsHeaders.DATA_PARTITION_ID, PARTITION_ID);
        data.put(DpsHeaders.CORRELATION_ID, CORRELATION_ID);

        //Call publish Message
        messageBusImpl.publishMessageForSystemSchema("dummy", "schema_create");

        //Assert that eventGridTopicStore is called once
        verify(this.eventGridTopicStore, times(2)).publishToEventGridTopic(anyString(), anyString(), captorList.capture());
        ArrayList<EventGridEvent> eventGridList = captorList.getValue();
        assertNotNull(eventGridList);
        assertThat(eventGridList.size(), is(equalTo(1)));

        HashMap<String, Object> outputData = (HashMap<String, Object>)eventGridList.get(0).data();
        assertEquals(((SchemaPubSubInfo[])outputData.get("data"))[0].getKind(), "dummy");
        assertEquals(((SchemaPubSubInfo[])outputData.get("data"))[0].getOp(), "schema_create");

    }

}
