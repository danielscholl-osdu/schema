package org.opengroup.osdu.schema.azure.service.serviceimpl;


import org.opengroup.osdu.schema.azure.interfaces.IAuthorizationServiceForServicePrincipal;
import com.azure.spring.autoconfigure.aad.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthorizationServiceForServicePrincipalImpl implements IAuthorizationServiceForServicePrincipal {
    enum UserType {
        REGULAR_USER,
        GUEST_USER,
        SERVICE_PRINCIPAL
    }

    @Override
    public boolean isDomainAdminServiceAccount() {
        final Object principal = getUserPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            return false;
        }

        final UserPrincipal userPrincipal = (UserPrincipal) principal;

        UserType type = getType(userPrincipal);
        if (type == UserType.SERVICE_PRINCIPAL) {
            return true;
        }
        return false;
    }

    /**
     * The internal method to get the user principal.
     *
     * @return user principal
     */
    private Object getUserPrincipal() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getPrincipal();
    }

    /**
     * Convenience method returning the type of user
     *
     * @param u user principal to check
     * @return the user type
     */
    private UserType getType(UserPrincipal u) {
        UserType type;
        Map<String, Object> claims = u.getClaims();
        if (claims != null && claims.get("upn") != null) {
            type = UserType.REGULAR_USER;
        } else if (claims != null && claims.get("unique_name") != null) {
            type = UserType.GUEST_USER;
        } else {
            type = UserType.SERVICE_PRINCIPAL;
        }
        return type;
    }
}
