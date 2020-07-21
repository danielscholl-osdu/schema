package org.opengroup.osdu.schema.security;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.entitlements.EntitlementsService;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.EntitlementsException;
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.UnauthorizedException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuthorizationFilterTest {

    @Mock
    IEntitlementsFactory entitlementsFactory;

    @Mock
    DpsHeaders headers;

    @Mock
    EntitlementsService en;

    @Mock
    Groups groups;

    @Mock
    JaxRsDpsLog log;

    @InjectMocks
    AuthorizationFilter authorizationFilter;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testHasRole_UnAuthorozed() throws BadRequestException, EntitlementsException {

        expectedException.expect(UnauthorizedException.class);
        expectedException.expectMessage(SchemaConstants.UNAUTHORIZED_EXCEPTION);

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        Mockito.when(en.getGroups()).thenReturn(groups);

        authorizationFilter.hasRole("service.schema.admins");
    }

    @Test
    public void testHasRole_EntitlementException_BadRequest() throws BadRequestException, EntitlementsException {

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(SchemaConstants.BAD_INPUT);

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        Mockito.when(en.getGroups()).thenThrow(
                new EntitlementsException("Invalid token", new HttpResponse(null, null, null, 400, null, null, 0L)));

        authorizationFilter.hasRole("service.schema.admins");
    }

    @Test
    public void testHasRole_EntitlementException_UnAuthenticated() throws BadRequestException, EntitlementsException {

        expectedException.expect(UnauthorizedException.class);
        expectedException.expectMessage(SchemaConstants.UNAUTHORIZED_EXCEPTION);

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        Mockito.when(en.getGroups()).thenThrow(
                new EntitlementsException("Invalid token", new HttpResponse(null, null, null, 403, null, null, 0L)));

        authorizationFilter.hasRole("service.schema.admins");
    }

    @Test
    public void testHasRole_EntitlementException_UnAuthorized() throws BadRequestException, EntitlementsException {

        expectedException.expect(UnauthorizedException.class);
        expectedException.expectMessage(SchemaConstants.UNAUTHORIZED_EXCEPTION);

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        Mockito.when(en.getGroups()).thenThrow(
                new EntitlementsException("Invalid token", new HttpResponse(null, null, null, 401, null, null, 0L)));

        authorizationFilter.hasRole("service.schema.admins");
    }

    @Test
    public void testHasRole_EntitlementException_Runtime() throws BadRequestException, EntitlementsException {

        expectedException.expect(RuntimeException.class);

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        Mockito.when(en.getGroups()).thenThrow(
                new EntitlementsException("Invalid token", new HttpResponse(null, null, null, 500, null, null, 0L)));

        authorizationFilter.hasRole("service.schema.admins");
    }

    @Test
    public void testHasRole_Authorized() throws BadRequestException, EntitlementsException {

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        Mockito.when(headers.getPartitionId()).thenReturn("test");
        Mockito.when(entitlementsFactory.create(headers)).thenReturn(en);
        Mockito.when(en.getGroups()).thenReturn(getMockGrooups());

        assertEquals(true, authorizationFilter.hasRole("service.schema.admins"));
    }

    @Test
    public void testHasRole_NoAuthorization_header() throws BadRequestException, EntitlementsException {

        expectedException.expect(UnauthorizedException.class);
        expectedException.expectMessage("Authorization header is mandatory");

        Mockito.when(headers.getPartitionId()).thenReturn("test");
        authorizationFilter.hasRole("service.schema.admins");

    }

    @Test
    public void testHasRole_NoPartition_header() throws BadRequestException, EntitlementsException {

        expectedException.expect(UnauthorizedException.class);
        expectedException.expectMessage("data-partition-id header is mandatory");

        Mockito.when(headers.getAuthorization()).thenReturn("test");
        authorizationFilter.hasRole("service.schema.admins");

    }

    private Groups getMockGrooups() {

        Groups groups = new Groups();
        List<GroupInfo> groupInfoList = new ArrayList<GroupInfo>();
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setName("service.schema.admins");
        groupInfo.setEmail("service.schema.admins");

        groupInfoList.add(groupInfo);
        groups.setGroups(groupInfoList);
        groups.setDesId("test@slb.com");

        return groups;

    }

}
