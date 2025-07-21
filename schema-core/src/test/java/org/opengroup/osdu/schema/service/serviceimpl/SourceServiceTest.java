
package org.opengroup.osdu.schema.service.serviceimpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class SourceServiceTest {

    @InjectMocks
    SourceService sourceService;

    @MockBean
    Source mockSource;

    @Mock
    ISourceStore iSourceStore;

    @Test
    public void testCheckAndRegisterEntityIfNotPresent() throws NotFoundException, ApplicationException {
        Mockito.when(iSourceStore.get("testEntityType")).thenReturn(getMockSourceObject());
        assertEquals(true, sourceService.checkAndRegisterSourceIfNotPresent(mockSource.getSourceId()));
    }

    @Test
    public void testCheckAndRegisterEntityIfNotPresent_ApplicationException()
            throws ApplicationException, BadRequestException {
        getMockSourceObject();
        when(iSourceStore.create(mockSource)).thenThrow(ApplicationException.class);
        assertEquals(false, sourceService.checkAndRegisterSourceIfNotPresent(mockSource.getSourceId()));
    }

    @Test
    public void testCheckAndRegisterEntityIfNotPresent_BadRequestException()
            throws ApplicationException, BadRequestException {
        getMockSourceObject();
        when(iSourceStore.create(mockSource)).thenThrow(BadRequestException.class);
        assertEquals(true, sourceService.checkAndRegisterSourceIfNotPresent(mockSource.getSourceId()));
    }

    private Source getMockSourceObject() {
        mockSource = new Source();
        mockSource.setSourceId("testSourceId");
        return mockSource;
    }

}
