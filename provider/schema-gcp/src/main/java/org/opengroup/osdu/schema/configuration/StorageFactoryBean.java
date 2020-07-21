package org.opengroup.osdu.schema.configuration;

import org.opengroup.osdu.core.gcp.multitenancy.GcsMultiTenantAccess;
import org.opengroup.osdu.core.gcp.multitenancy.TenantFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class StorageFactoryBean extends AbstractFactoryBean<GcsMultiTenantAccess> {

    @Autowired
    TenantFactory tenantFactory;

    @Override
    protected GcsMultiTenantAccess createInstance() throws Exception {

        return new GcsMultiTenantAccess();
    }

    @Override
    public Class<?> getObjectType() {
        return GcsMultiTenantAccess.class;
    }
}