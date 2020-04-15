package org.opengroup.osdu.schema.credentials;

import java.util.concurrent.TimeUnit;

import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.springframework.stereotype.Component;
import org.threeten.bp.Duration;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.TransportOptions;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Component
public class StorageFactory {

    // The numbers used for settings are selected on random basis, We can update
    // them with experience/issue faced
    private static final RetrySettings RETRY_SETTINGS = RetrySettings.newBuilder().setMaxAttempts(6)
            .setInitialRetryDelay(Duration.ofSeconds(1)).setMaxRetryDelay(Duration.ofSeconds(10))
            .setRetryDelayMultiplier(2.0).setTotalTimeout(Duration.ofSeconds(50))
            .setInitialRpcTimeout(Duration.ofSeconds(50)).setRpcTimeoutMultiplier(1.1)
            .setMaxRpcTimeout(Duration.ofSeconds(50)).build();

    // The numbers used for settings are selected on random basis, We can update
    // them with experience/issue faced
    private static final TransportOptions TRANSPORT_OPTIONS = HttpTransportOptions.newBuilder()
            .setReadTimeout(40 * 1000).setConnectTimeout(10 * 1000).build();

    private Cache<String, CloudCredentials> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(60 * 60, TimeUnit.SECONDS).maximumSize(100).build();

    public Storage getStorage(TenantInfo tenantInfo) throws ApplicationException {
        CloudCredentials credential = this.cache.getIfPresent(tenantInfo.getName());
        if (credential == null) {
            credential = new CloudCredentials(tenantInfo);
            this.cache.put(tenantInfo.getName(), credential);
        }

        return StorageOptions.newBuilder().setCredentials(credential).setProjectId(tenantInfo.getProjectId())
                .setRetrySettings(RETRY_SETTINGS).setTransportOptions(TRANSPORT_OPTIONS).build().getService();
    }
}