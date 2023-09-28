package org.opengroup.osdu.schema.provider.aws.security;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.aws.entitlements.RequestKeys;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations="classpath:application.properties")
public class AuthorizationServiceForServiceAdminImplTest {

	@InjectMocks
	private AuthorizationServiceForServiceAdminImpl authorizationServiceForServiceAdminImpl;

	@Mock
	private DpsHeaders headers;

	@Test(expected =  AppException.class)
	public void isDomainAdminServiceAccount_NoJWTtoken() {
		authorizationServiceForServiceAdminImpl.isDomainAdminServiceAccount();
	}
	
	@Test(expected =  AppException.class)
	public void isDomainAdminServiceAccount_UnauthorizedUser() {
		Map<String, String> header = new HashMap<>();
		header.put(RequestKeys.AUTHORIZATION_HEADER_KEY, "AUTHORIZATION_HEADER_KEY");
		header.put(DpsHeaders.USER_ID,"not-a-user@testing.com");
		Mockito.when(headers.getHeaders()).thenReturn(header);
		assertFalse(authorizationServiceForServiceAdminImpl.isDomainAdminServiceAccount());
	}
}
