package org.opengroup.osdu.schema.credentials;

import java.util.concurrent.TimeUnit;

import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.util.Crc32c;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.springframework.stereotype.Component;
import org.threeten.bp.Duration;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Component
public class DatastoreFactory {

    private Cache<String, CloudCredentials> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(60 * 60, TimeUnit.SECONDS).maximumSize(100).build();

    // The numbers used for settings are selected on random basis, We can update
    // them with experience/issue faced
    private static final RetrySettings RETRY_SETTINGS = RetrySettings.newBuilder().setMaxAttempts(6)
            .setInitialRetryDelay(Duration.ofSeconds(1)).setMaxRetryDelay(Duration.ofSeconds(10))
            .setRetryDelayMultiplier(2.0).setTotalTimeout(Duration.ofSeconds(50))
            .setInitialRpcTimeout(Duration.ofSeconds(50)).setRpcTimeoutMultiplier(1.1)
            .setMaxRpcTimeout(Duration.ofSeconds(50)).build();

    public Datastore getDatastore(TenantInfo tenantInfo) throws ApplicationException {

        String cacheKey = this.getCredentialsCacheKey(tenantInfo.getName());
        CloudCredentials credential = this.cache.getIfPresent(cacheKey);

        if (credential == null) {
            credential = new CloudCredentials(tenantInfo);
            this.cache.put(cacheKey, credential);
        }

        return DatastoreOptions.newBuilder().setRetrySettings(RETRY_SETTINGS).setCredentials(credential)
                .setProjectId(tenantInfo.getProjectId()).build().getService();
    }

    private String getCredentialsCacheKey(String tenantName) {
        return Crc32c.hashToBase64EncodedString(String.format("CloudCredential:%s", tenantName));
    }

}