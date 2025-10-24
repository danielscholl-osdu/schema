// Copyright © Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.schema.provider.aws.security;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.aws.v2.entitlements.RequestKeys;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations="classpath:application.properties")
class AuthorizationServiceForServiceAdminImplTest {

	@InjectMocks
	private AuthorizationServiceForServiceAdminImpl authorizationServiceForServiceAdminImpl;

	@Mock
	private DpsHeaders headers;

	@Test
	void isDomainAdminServiceAccount_NoJWTtoken() {
		assertThrows(AppException.class, () -> {
			authorizationServiceForServiceAdminImpl.isDomainAdminServiceAccount();
		});
	}
	
	@Test
	void isDomainAdminServiceAccount_UnauthorizedUser() {
		Map<String, String> header = new HashMap<>();
		header.put(RequestKeys.AUTHORIZATION_HEADER_KEY, "AUTHORIZATION_HEADER_KEY");
		header.put(DpsHeaders.USER_ID,"not-a-user@testing.com");
		Mockito.when(headers.getHeaders()).thenReturn(header);
		Mockito.when(headers.getUserId()).thenReturn("not-a-user@testing.com");
		
		assertThrows(AppException.class, () -> {
			authorizationServiceForServiceAdminImpl.isDomainAdminServiceAccount();
		});
	}
}
