package org.opengroup.osdu.schema.security;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.UnauthorizedException;
import org.opengroup.osdu.schema.provider.interfaces.authorization.IAuthorizationServiceForServiceAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("authorizationFilterSA")
@RequestScope
public class AuthorizationFilterSA {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private IAuthorizationServiceForServiceAdmin authorizationService;

    public boolean hasPermissions()  {
        headers.put(DpsHeaders.USER_EMAIL, "ServiceAdminUser");
        validateMandatoryHeaders();
        if (!StringUtils.isEmpty(this.headers.getPartitionId())) {
            throw new BadRequestException("data-partition-id header should not be passed");
        }
        return authorizationService.isDomainAdminServiceAccount();
    }

    private void validateMandatoryHeaders() {
        if (StringUtils.isEmpty(this.headers.getAuthorization())) {
            throw new UnauthorizedException("Authorization header is mandatory");
        }
    }
}
