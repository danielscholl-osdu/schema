package org.opengroup.osdu.schema.provider.aws.security;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.entitlements.RequestKeys;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;


@RunWith(MockitoJUnitRunner.class)
public class AuthorizationServiceForServiceAdminImplTest {

	@InjectMocks
	private AuthorizationServiceForServiceAdminImpl authorizationServiceForServiceAdminImpl;

	@Mock
	private DpsHeaders headers;

	@Test(expected = AppException.class)
	public void isDomainAdminServiceAccount_NoJWTtoken() {
		authorizationServiceForServiceAdminImpl.isDomainAdminServiceAccount();
	}
	
	@Test(expected = AppException.class)
	public void isDomainAdminServiceAccount_UnauthorizedUser() {
		Map<String, String> header = new HashMap<>();
		header.put(RequestKeys.AUTHORIZATION_HEADER_KEY, "AUTHORIZATION_HEADER_KEY");
		header.put("x-user-id", DpsHeaders.USER_ID);
		Mockito.when(headers.getHeaders()).thenReturn(header);
		
		assertTrue(authorizationServiceForServiceAdminImpl.isDomainAdminServiceAccount());
	}
}
