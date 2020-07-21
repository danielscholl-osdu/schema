package org.opengroup.osdu.schema.configuration;

import org.opengroup.osdu.core.gcp.multitenancy.DatastoreFactory;
import org.opengroup.osdu.core.gcp.multitenancy.TenantFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class DatastoreFactoryBean extends AbstractFactoryBean<DatastoreFactory> {

    @Autowired
    TenantFactory tenantFactory;

    @Override
    protected DatastoreFactory createInstance() throws Exception {

        return new DatastoreFactory(tenantFactory);
    }

    @Override
    public Class<?> getObjectType() {
        return DatastoreFactory.class;
    }
}
