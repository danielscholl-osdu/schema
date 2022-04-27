package org.opengroup.osdu.schema.security;

import static org.opengroup.osdu.core.common.model.http.DpsHeaders.DATA_PARTITION_ID;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.schema.configuration.PropertiesConfiguration;
import org.opengroup.osdu.schema.provider.interfaces.authorization.IAuthorizationServiceForServiceAdmin;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Slf4j
@Component
@RequestScope
@RequiredArgsConstructor
public class AuthorizationServiceForServiceAdminImpl implements IAuthorizationServiceForServiceAdmin {

    private final DpsHeaders headers;
    private final String WORKFLOW_SYSTEM_ADMIN = "service.schema-service.system-admin";
    private final PropertiesConfiguration configuration;
    private final IAuthorizationService authorizationService;

    @Override
    public boolean isDomainAdminServiceAccount() {
        if (Objects.isNull(headers.getAuthorization()) || headers.getAuthorization().isEmpty()) {
            throw AppException.createUnauthorized("No JWT token. Access is Forbidden");
        }
        this.headers.put(DATA_PARTITION_ID, configuration.getSharedTenantName());
        AuthorizationResponse authResponse =
            authorizationService.authorizeAny(headers, WORKFLOW_SYSTEM_ADMIN);
        headers.put(DpsHeaders.USER_EMAIL, authResponse.getUser());
        return true;
    }
}
