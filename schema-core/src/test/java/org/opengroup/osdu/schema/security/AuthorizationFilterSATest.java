package org.opengroup.osdu.schema.security;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.provider.interfaces.authorization.IAuthorizationServiceForServiceAdmin;
import org.opengroup.osdu.schema.provider.interfaces.authorization.SystemPartitionAuthService;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuthorizationFilterSATest {

    @Mock
    IEntitlementsFactory entitlementsFactory;

    @Mock
    DpsHeaders headers;

    @Mock
    EntitlementsService en;

    @Mock
    JaxRsDpsLog log;

    @Mock
    SystemPartitionAuthService authorizationServiceForSystemPartition;

    @Mock
    IAuthorizationServiceForServiceAdmin authorizationServiceForServiceAdmin;

    @InjectMocks
    AuthorizationFilterSA authorizationFilterSA;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void testRuntimeException_when_DataPartitionIsPresentInSystemHeader()  {

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("data-partition-id header should not be passed");
        authorizationFilterSA.hasPermissions();
    }


    @Test
    public void testSystemAuthentication_is_checked_when_NotServiceAccount()  {

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        Mockito.when(authorizationServiceForServiceAdmin.isDomainAdminServiceAccount()).thenReturn(false);

        boolean response = authorizationFilterSA.hasPermissions();

        Mockito.verify(authorizationServiceForSystemPartition, Mockito.times(1)).hasSystemLevelPermissions();
        assertFalse(response);
    }

    @Test
    public void testSystemAuthentication_is_not_checked_when_ServiceAccount()  {

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        Mockito.when(authorizationServiceForServiceAdmin.isDomainAdminServiceAccount()).thenReturn(true);

        boolean response = authorizationFilterSA.hasPermissions();

        Mockito.verify(authorizationServiceForSystemPartition, Mockito.never()).hasSystemLevelPermissions();
        assertTrue(response);
    }
}
