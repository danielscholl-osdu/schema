package org.opengroup.osdu.schema.impl.schemainfostore;

import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.osm.model.Destination;
import org.opengroup.osdu.core.gcp.osm.model.Kind;
import org.opengroup.osdu.core.gcp.osm.model.Namespace;
import org.opengroup.osdu.core.gcp.osm.service.Context;
import org.opengroup.osdu.core.gcp.osm.translate.TranslatorRuntimeException;
import org.opengroup.osdu.schema.configuration.PropertiesConfiguration;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.impl.OsmDestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class OsmSourceStoreTest {

    private static final String SOURCE_ID = "sourceId";
    private static final String COMMON_TENANT_ID = "common";
    private static final Destination DESTINATION = Destination.builder()
            .partitionId("partitionId")
            .namespace(new Namespace("namespace"))
            .kind(new Kind("testKind"))
            .build();

    @InjectMocks
    OsmSourceStore osmSourceStore;

    @Mock
    Source mockSource;

    @Mock
    DpsHeaders headers;

    @Mock
    TenantInfo tenantInfo;

    @Mock
    ITenantFactory tenantFactory;

    @Mock
    JaxRsDpsLog log;

    @Mock
    Context context;

    @Mock
    OsmDestinationProvider destinationProvider;

    @Mock
    PropertiesConfiguration configuration;

    @Before
    public void setUp() {
        when(configuration.getSharedTenantName()).thenReturn(COMMON_TENANT_ID);
        ReflectionTestUtils.setField(osmSourceStore, "configuration", configuration);
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(tenantFactory.getTenantInfo("test")).thenReturn(tenantInfo);

        Mockito.when(mockSource.getSourceId()).thenReturn("wks");

        when(destinationProvider.getDestination(any(), any(), any())).thenReturn(DESTINATION);

        when(context.getOne(any())).thenReturn(null);
        when(context.findOne(any())).thenReturn(Optional.of(mockSource));
        when(context.createAndGet(any(), any())).thenReturn(mockSource);
    }

    @Test
    public void testGet() throws NotFoundException, ApplicationException {
        assertNotNull(osmSourceStore.get(SOURCE_ID));
    }

    @Test
    public void testGet_SystemSchemas() throws NotFoundException, ApplicationException {
        Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
        Mockito.when(tenantFactory.getTenantInfo(COMMON_TENANT_ID)).thenReturn(tenantInfo);

        assertNotNull(osmSourceStore.getSystemSource(SOURCE_ID));
    }

    @Test
    public void testGet_NotFoundException() {
        when(context.findOne(any())).thenReturn(Optional.empty());
        try {
            osmSourceStore.get(Strings.EMPTY);
            fail("Should not succeed");
        } catch (NotFoundException e) {
            assertEquals("bad input parameter", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testGet_NotFoundException_SystemSchemas() {
        when(context.findOne(any())).thenReturn(Optional.empty());
        try {
            when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
            when(tenantFactory.getTenantInfo(COMMON_TENANT_ID)).thenReturn(tenantInfo);
            osmSourceStore.getSystemSource(Strings.EMPTY);
            fail("Should not succeed");
        } catch (NotFoundException e) {
            assertEquals("bad input parameter", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreate() throws NotFoundException, ApplicationException, BadRequestException {
        assertNotNull(osmSourceStore.create(mockSource));
    }

    @Test
    public void testCreate_SystemSchemas() throws NotFoundException, ApplicationException, BadRequestException {
        Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
        Mockito.when(tenantFactory.getTenantInfo(COMMON_TENANT_ID)).thenReturn(tenantInfo);

        assertNotNull(osmSourceStore.createSystemSource(mockSource));
    }

    @Test
    public void testCreate_BadRequestException() throws NotFoundException, ApplicationException, BadRequestException {
        osmSourceStore = Mockito.spy(osmSourceStore);
        when(context.getOne(any())).thenReturn(mockSource);
        try {
            osmSourceStore.create(mockSource);
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Source already registered with Id: wks", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreate_BadRequestException_SystemSchemas() throws NotFoundException, ApplicationException, BadRequestException {
        osmSourceStore = Mockito.spy(osmSourceStore);
        Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
        Mockito.when(tenantFactory.getTenantInfo(COMMON_TENANT_ID)).thenReturn(tenantInfo);
        when(context.getOne(any())).thenReturn(mockSource);
        try {
            osmSourceStore.createSystemSource(mockSource);
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Source already registered with Id: wks", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreate_ApplicationException() throws NotFoundException, ApplicationException, BadRequestException {
        osmSourceStore = Mockito.spy(osmSourceStore);
        when(context.createAndGet(any(), any())).thenThrow(TranslatorRuntimeException.class);
        try {
            osmSourceStore.create(mockSource);
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals(SchemaConstants.INVALID_INPUT, e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreate_ApplicationException_SystemSchemas() throws NotFoundException, ApplicationException, BadRequestException {
        osmSourceStore = Mockito.spy(osmSourceStore);
        Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
        Mockito.when(tenantFactory.getTenantInfo(COMMON_TENANT_ID)).thenReturn(tenantInfo);
        when(context.createAndGet(any(), any())).thenThrow(TranslatorRuntimeException.class);
        try {
            osmSourceStore.createSystemSource(mockSource);
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals(SchemaConstants.INVALID_INPUT, e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }
}
