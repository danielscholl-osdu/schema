package org.opengroup.osdu.schema.util;

import com.google.common.base.Strings;
import org.opengroup.osdu.azure.util.AzureServicePrincipal;
import org.opengroup.osdu.core.ibm.util.IdentityClient;
import org.opengroup.osdu.schema.util.gcp.GoogleServiceAccount;
import org.opengroup.osdu.schema.util.gcp.OpenIDTokenProvider;


public class AuthUtil {
    public synchronized String getToken() throws Exception {
        String token = null;
        String vendor = System.getProperty("VENDOR", System.getenv("VENDOR"));
        if (Strings.isNullOrEmpty(token) && vendor.equals("gcp")) {
            String serviceAccountFile = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
            token = new GoogleServiceAccount(serviceAccountFile).getAuthToken();
        }else if (Strings.isNullOrEmpty(token) && vendor.equals("aws")) {
            token = AwsServicePrincipalUtil.getAccessToken();
        } else if (Strings.isNullOrEmpty(token) && vendor.equals("azure")) {
            String sp_id = System.getProperty("INTEGRATION_TESTER", System.getenv("INTEGRATION_TESTER"));
            String sp_secret = System.getProperty("TESTER_SERVICEPRINCIPAL_SECRET", System.getenv("TESTER_SERVICEPRINCIPAL_SECRET"));
            String tenant_id = System.getProperty("AZURE_AD_TENANT_ID", System.getenv("AZURE_AD_TENANT_ID"));
            String app_resource_id = System.getProperty("AZURE_AD_APP_RESOURCE_ID", System.getenv("AZURE_AD_APP_RESOURCE_ID"));
            token = new AzureServicePrincipal().getIdToken(sp_id, sp_secret, tenant_id, app_resource_id);
        } else if (Strings.isNullOrEmpty(token) && vendor.equals("ibm")) {
            token = IdentityClient.getTokenForUserWithAccess();
        } else if (Strings.isNullOrEmpty(token) && vendor.equals("anthos")){
            token = new OpenIDTokenProvider().getToken();
        }
        return "Bearer " + token;
    }
}
