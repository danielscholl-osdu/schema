package org.opengroup.osdu.schema.credentials;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpHeaders;
import org.opengroup.osdu.core.common.http.HttpClient;
import org.opengroup.osdu.core.common.http.HttpRequest;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.Iam.Projects;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts;
import com.google.api.services.iam.v1.Iam.Projects.ServiceAccounts.SignJwt;
import com.google.api.services.iam.v1.IamScopes;
import com.google.api.services.iam.v1.model.SignJwtRequest;
import com.google.api.services.iam.v1.model.SignJwtResponse;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.JsonObject;

import lombok.extern.java.Log;

/**
 * This class is used to implement the domain wide delegation using the service
 * account token creator role. This class extends GoogleCredentials, and creates
 * access token by creating the JWT token without private keys, and signing it
 * with Google OAuth2 service.
 * 
 */

@Log
public class CloudCredentials extends GoogleCredentials {

    private static final long serialVersionUID = -8461791038757192780L;

    private static final String JWT_AUDIENCE = "https://www.googleapis.com/oauth2/v4/token";
    private static final String SERVICE_ACCOUNT_NAME_FORMAT = "projects/%s/serviceAccounts/%s";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String EXPIRES_IN = "expires_in";
    private static final String SCOPE = "https://www.googleapis.com/auth/devstorage.full_control https://www.googleapis.com/auth/datastore";

    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private final TenantInfo tenant;
    private Iam iam;

    public CloudCredentials(TenantInfo tenant) throws ApplicationException {

        if ((tenant == null) || StringUtils.isBlank(tenant.getName()) || StringUtils.isBlank(tenant.getProjectId())
                || StringUtils.isBlank(tenant.getServiceAccount())) {
            throw new ApplicationException("Tenant name, project id or service account is not set.");
        }
        this.tenant = tenant;
    }

    /**
     * Overrides the GoogleCredentials refreshAccessToken. Returns JWT token for the
     * service account of the tenant.
     * 
     * @throws IOException
     */
    @Override
    public AccessToken refreshAccessToken() throws IOException {

        try {

            Map<String, Object> signJwtPayload = this.getPayload();
            String signedJwt = getSignedJwt(signJwtPayload);
            return getAccessToken(signedJwt);

        } catch (ApplicationException e) {
            log.log(Level.SEVERE, "Error : An unexpected error occurred while geting refresh access token : ", e);
            throw new IOException("An error occurred when accessing third-party APIs");
        }

    }

    /**
     * Creates the payload for JWT signing
     * 
     * @return payload as map
     */
    private Map<String, Object> getPayload() {

        Map<String, Object> payload = new HashMap<>();

        payload.put("scope", SCOPE);
        // This will get current time in seconds and add 1 hours (i.e 3600 seconds to
        // it)
        payload.put("exp", System.currentTimeMillis() / 1000 + 3600);
        payload.put("iat", System.currentTimeMillis() / 1000);
        payload.put("iss", this.tenant.getServiceAccount());
        payload.put("aud", JWT_AUDIENCE);

        return payload;
    }

    /**
     * Signs the JWT token of the tenant service account, without private keys.
     * 
     * @param signJwtPayload
     * @return the signed JWT
     * @throws ApplicationException
     */
    private String getSignedJwt(Map<String, Object> signJwtPayload) throws ApplicationException {
        try {
            SignJwtRequest signJwtRequest = new SignJwtRequest();
            signJwtRequest.setPayload(JSON_FACTORY.toString(signJwtPayload));

            String serviceAccountName = String.format(SERVICE_ACCOUNT_NAME_FORMAT, this.tenant.getProjectId(),
                    this.tenant.getServiceAccount());

            Iam iamInstance = this.getIam();
            Projects projects = iamInstance.projects();
            ServiceAccounts serviceAccounts = projects.serviceAccounts();
            SignJwt signJwt = serviceAccounts.signJwt(serviceAccountName, signJwtRequest);

            SignJwtResponse signJwtResponse = signJwt.execute();
            return signJwtResponse.getSignedJwt();

        } catch (IOException | GeneralSecurityException e) {
            log.log(Level.SEVERE, "Error occoured: ", e);
            throw new ApplicationException("An error occurred when accessing third-party APIs");
        }

    }

    /**
     * Creates the access token from the signed JWT for the tenant service account
     * by signing it with Google OAuth2 service.
     * 
     * @param signedJwt the signed JWT
     * @return Access token that is used by Google services such as GCS, PubSub,
     *         Datastore at the time of the access
     * @throws GeneralSecurityException
     * @throws ApplicationException
     */
    private AccessToken getAccessToken(String signedJwt) throws ApplicationException {

        HttpRequest request = HttpRequest.post().url(JWT_AUDIENCE)
                .headers(Collections.singletonMap(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded"))
                .body(String.format("%s=%s&%s=%s", "grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer",
                        "assertion", signedJwt))
                .build();

        HttpResponse response = new HttpClient().send(request);
        JsonObject jsonResult = response.getAsJsonObject();

        if (!jsonResult.has(ACCESS_TOKEN)) {
            throw new ApplicationException("An error occurred when accessing third-party APIs");
        }

        return new AccessToken(jsonResult.get(ACCESS_TOKEN).getAsString(),
                DateUtils.addSeconds(new Date(), jsonResult.get(EXPIRES_IN).getAsInt()));

    }

    /**
     * Gets the Iam object of the services project, which is further used for
     * signing the JWT
     * 
     * @return the Iam object of the services project
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private Iam getIam() throws GeneralSecurityException, IOException {
        if (this.iam == null) {

            Iam.Builder builder = new Iam.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY,
                    GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(IamScopes.CLOUD_PLATFORM)))
                            .setApplicationName(SchemaConstants.APPLICATION_NAME);

            this.iam = builder.build();
        }
        return this.iam;
    }
}