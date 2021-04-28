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
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.eventgrid.EventGridTopicStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.di.EventGridConfig;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.logging.AuditLogger;

import com.microsoft.azure.eventgrid.models.EventGridEvent;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;

@ExtendWith(MockitoExtension.class)
public class MessageBusImplTest {

    private static final String DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID = "data-partition-account-id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String PARTITION_ID = "partition-id";

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
    
    @InjectMocks
    private MessageBusImpl messageBusImpl;
    
    @Before
    public void init() throws ServiceBusException, InterruptedException {
        initMocks(this);
        doReturn(DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID).when(dpsHeaders).getPartitionIdWithFallbackToAccountId();
        doReturn(PARTITION_ID).when(dpsHeaders).getPartitionId();
        doReturn(CORRELATION_ID).when(dpsHeaders).getCorrelationId();
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
    public void should_publishToEventGrid_WhenFlagIsTrue() {
    	
    	//The schema-notification is turned off
    	when(this.eventGridConfig.isEventGridEnabled()).thenReturn(true);
    	//The schema-notification is turned off
    	when(this.eventGridConfig.getCustomTopicName()).thenReturn("dummy-topic");
    	//The schema-notification is turned off
    	doNothing().when(this.eventGridTopicStore).publishToEventGridTopic(anyString(), anyString(), anyList());;
    	ArgumentCaptor<ArrayList<EventGridEvent>> captorList = ArgumentCaptor.forClass(ArrayList.class);
    	
    	HashMap<String, Object> data = new HashMap<>();
		data.put(SchemaConstants.KIND, "dummy");
		data.put(DpsHeaders.ACCOUNT_ID, DATA_PARTITION_WITH_FALLBACK_ACCOUNT_ID);
		data.put(DpsHeaders.DATA_PARTITION_ID, PARTITION_ID);
		data.put(DpsHeaders.CORRELATION_ID, CORRELATION_ID);
        
		//Call publish Message
    	messageBusImpl.publishMessage("dummy", "dummy");
        
    	//Assert that eventGridTopicStore is called once
        verify(this.eventGridTopicStore, times(1)).publishToEventGridTopic(anyString(), anyString(), captorList.capture());
        ArrayList<EventGridEvent> eventGridList = captorList.getValue();
        assertNotNull(eventGridList);
        assertThat(eventGridList.size(), is(equalTo(1)));
        assertEquals(eventGridList.get(0).data(), data);

    }

}
