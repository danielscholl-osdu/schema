/* Copyright © Amazon Web Services

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package org.opengroup.osdu.schema.provider.aws.security;

import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.opengroup.osdu.core.aws.v2.entitlements.RequestKeys;
import org.opengroup.osdu.core.aws.v2.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.v2.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.provider.interfaces.authorization.IAuthorizationServiceForServiceAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationServiceForServiceAdminImpl implements IAuthorizationServiceForServiceAdmin {
    @Autowired
    private DpsHeaders headers;

    String memberEmail=null;
    String spuEmail=null;

    @PostConstruct
    public void init() throws K8sParameterNotFoundException {
        //get sp email
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();
        spuEmail = provider.getParameterAsString("SERVICE_PRINCIPAL");
    }

    @Override
    public boolean isDomainAdminServiceAccount() {

        try {
            Map<String, String> dpsheaders =   headers.getHeaders();
            String authorizationContents = dpsheaders.get(RequestKeys.AUTHORIZATION_HEADER_KEY);
            if(authorizationContents == null){
                authorizationContents = dpsheaders.get(RequestKeys.AUTHORIZATION_HEADER_KEY.toLowerCase());
            }
            //no JWT
            if(authorizationContents == null)
            {
                throw  AppException.createUnauthorized("No JWT token. Access is Forbidden");
            }


            memberEmail = headers.getUserId();
            if(memberEmail.equals(spuEmail)){
                return true;
            }
            else{
                throw  AppException.createUnauthorized("Unauthorized. The user is not Service Principal");
            }
        }
        catch (AppException appE) {
            throw appE;
        }
        catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Authentication Failure", e.getMessage(), e);
        }
    }
}
