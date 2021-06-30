package org.opengroup.osdu.schema.azure.auth;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.interfaces.IAuthorizationServiceForServicePrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("authorizationFilterSP")
@RequestScope
public class AuthorizationFilterSP {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private IAuthorizationServiceForServicePrincipal authorizationService;

    public boolean hasPermissions() {
        headers.put(DpsHeaders.USER_EMAIL, "ServicePrincipalUser");
        return authorizationService.isDomainAdminServiceAccount();
    }
}
