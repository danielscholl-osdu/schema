package org.opengroup.osdu.schema.security;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.entitlements.EntitlementsService;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.EntitlementsException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuthorizationFilterSAtest {

    @Mock
    IEntitlementsFactory entitlementsFactory;

    @Mock
    DpsHeaders headers;

    @Mock
    EntitlementsService en;



    @Mock
    JaxRsDpsLog log;


    @InjectMocks
    AuthorizationFilterSA authorizationFilterSA;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void testRuntimeException_when_DataPartitionIsPresentInSystemHeader() throws BadRequestException, EntitlementsException {

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("data-partition-id header should not be passed");
        authorizationFilterSA.hasPermissions();
    }




}
