package org.opengroup.osdu.schema.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.azure.util.AzureServicePrincipal;
import org.opengroup.osdu.core.aws.cognito.AWSCognitoClient;
import org.opengroup.osdu.core.ibm.util.IdentityClient;


public class AuthUtil {
    public synchronized String getToken() throws Exception {
        String token = null;
        String vendor = System.getProperty("VENDOR", System.getenv("VENDOR"));
        if (Strings.isNullOrEmpty(token) && vendor.equals("gcp")) {
            String serviceAccountFile = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
            String audience = System.getProperty("INTEGRATION_TEST_AUDIENCE",
                    System.getenv("INTEGRATION_TEST_AUDIENCE"));
            token = new GoogleServiceAccount(serviceAccountFile).getAuthToken(audience);
        }else if (Strings.isNullOrEmpty(token) && vendor.equals("aws")) {
            String awsCognitoClientId = System.getProperty("AWS_COGNITO_CLIENT_ID", System.getenv("AWS_COGNITO_CLIENT_ID"));
            String awsCognitoAuthFlow = "USER_PASSWORD_AUTH";
            String awsCognitoAuthParamsUser = System.getProperty("AWS_COGNITO_AUTH_PARAMS_USER", System.getenv("AWS_COGNITO_AUTH_PARAMS_USER"));
            String awsCognitoAuthParamsPassword = System.getProperty("AWS_COGNITO_AUTH_PARAMS_PASSWORD", System.getenv("AWS_COGNITO_AUTH_PARAMS_PASSWORD"));
            AWSCognitoClient client = new AWSCognitoClient(awsCognitoClientId, awsCognitoAuthFlow, awsCognitoAuthParamsUser,  awsCognitoAuthParamsPassword);
            token=client.getToken();
        } else if (Strings.isNullOrEmpty(token) && vendor.equals("azure")) {
            String sp_id = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
            String sp_secret = System.getProperty("TESTER_SERVICEPRINCIPAL_SECRET", System.getenv("TESTER_SERVICEPRINCIPAL_SECRET"));
            String tenant_id = System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
            String app_resource_id = System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
            token = new AzureServicePrincipal().getIdToken(sp_id, sp_secret, tenant_id, app_resource_id);
        } else if (Strings.isNullOrEmpty(token) && vendor.equals("ibm")) {
            token = IdentityClient.getTokenForUserWithAccess();
        }
        return "Bearer " + token;
    }
}
