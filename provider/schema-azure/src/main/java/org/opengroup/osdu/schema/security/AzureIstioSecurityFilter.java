// Copyright © Microsoft Corporation
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

package org.opengroup.osdu.schema.security;

import java.io.IOException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import static org.springframework.util.StringUtils.hasText;
import org.springframework.web.filter.OncePerRequestFilter;

import com.azure.spring.cloud.autoconfigure.implementation.aad.filter.UserPrincipal;
import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.minidev.json.JSONArray;

@Component
@ConditionalOnProperty(value = "azure.istio.auth.enabled", havingValue = "true", matchIfMissing = true)
public class AzureIstioSecurityFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureIstioSecurityFilter.class);

    private static final String X_ISTIO_CLAIMS_PAYLOAD = "x-payload";

    /**
     * Filter logic.
     * @param servletRequest Request object.
     * @param servletResponse Response object.
     * @param filterChain Filter Chain object.
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doFilterInternal(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final FilterChain filterChain) throws ServletException, IOException {
        final String istioPayload = servletRequest.getHeader(X_ISTIO_CLAIMS_PAYLOAD);

        LOGGER.debug("Received headers list: {}", Collections.list(servletRequest.getHeaderNames()));

        try {
            if (hasText(istioPayload)) {
                JWTClaimsSet claimsSet = JWTClaimsSet.parse(new String(Base64.getDecoder().decode(istioPayload)));

                // By default the authenticated is set to true as part PreAuthenticatedAuthenticationToken constructor.
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                new PreAuthenticatedAuthenticationToken(
                                        new UserPrincipal(null,null, claimsSet),
                                        null,
                                        null
                                ));
            } else {
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                new PreAuthenticatedAuthenticationToken(
                                        null, null, null
                                ));
            }
        } catch (ParseException ex) {
            LOGGER.error("Failed to initialize UserPrincipal.", ex);
            throw new AppException(500, "Unable to parse claims in istio payload", ex.getMessage());
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
