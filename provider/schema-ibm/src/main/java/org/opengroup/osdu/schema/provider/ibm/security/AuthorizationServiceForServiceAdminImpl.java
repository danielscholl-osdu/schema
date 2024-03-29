package org.opengroup.osdu.schema.provider.ibm.security;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.schema.provider.interfaces.authorization.IAuthorizationServiceForServiceAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import org.springframework.beans.factory.annotation.Autowired;

@Component
@RequestScope
@Slf4j
public class AuthorizationServiceForServiceAdminImpl implements IAuthorizationServiceForServiceAdmin {
    
 
	@Autowired
	DpsHeaders dpsheaders;

    @Override
    public boolean isDomainAdminServiceAccount() {
        
		try {
			Map<String, String> headers = dpsheaders.getHeaders();
			String rootUser = headers.get("x-is-root-user");
			boolean isRootUser = Boolean.parseBoolean(rootUser);
			log.debug("logged in root user : " + isRootUser);
			if (isRootUser) {
				return true;
			} else {
				throw AppException.createUnauthorized("Unauthorized. The user is not Service Principal");
			}

		} catch (AppException e) {
			throw e;
		} catch (Exception e) {
			throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Authentication Failure", e.getMessage(),
					e);
		}

    }
}
