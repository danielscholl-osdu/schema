package org.opengroup.osdu.schema.provider.aws.security;

import org.opengroup.osdu.core.aws.entitlements.Authorizer;
import org.opengroup.osdu.core.aws.entitlements.RequestKeys;
import org.opengroup.osdu.core.aws.ssm.SSMUtil;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.provider.interfaces.authorization.IAuthorizationServiceForServiceAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class AuthorizationServiceForServiceAdminImpl implements IAuthorizationServiceForServiceAdmin {
    @Autowired
    private DpsHeaders headers;


    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.environment}")
    private String awsEnvironment;

    Authorizer authorizer;
    String memberEmail=null;
    SSMUtil ssmUtil = null;
    String spu_email=null;

    @PostConstruct
    public void init() {
        authorizer = new Authorizer(awsRegion, awsEnvironment);
        if (ssmUtil == null) {
            ssmUtil = new SSMUtil("/osdu/" + awsEnvironment + "/");
        }
        //get sp email
        spu_email = ssmUtil.getSsmParameterAsString("service-principal-user");

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


            memberEmail = authorizer.validateJWT(authorizationContents);
            if(memberEmail != null)
            {
                if(memberEmail.equals(spu_email)){
                    return true;
                }
                else{
                    throw  AppException.createUnauthorized("Unauthorized. The user is not Service Principal");
                }
            }
            if(memberEmail == null){
                throw  AppException.createUnauthorized("Unauthorized. The JWT token could not be validated");
            }

        }
        catch (AppException appE) {
            throw appE;
        }
        catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Authentication Failure", e.getMessage(), e);
        }
        return false;
    }
}
