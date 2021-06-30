package org.opengroup.osdu.schema.azure.auth;

import org.opengroup.osdu.schema.azure.interfaces.IAuthorizationServiceForServicePrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("authorizationFilterSP")
@RequestScope
public class AuthorizationFilterSP {

    @Autowired
    private IAuthorizationServiceForServicePrincipal authorizationService;

    public boolean hasPermissions() {
        return authorizationService.isDomainAdminServiceAccount();
    }
}
