package org.opengroup.osdu.schema.configuration;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opengroup.osdu.core.gcp.multitenancy.TenantFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class StorageFactoryBeanTest {

    @InjectMocks
    StorageFactoryBean storageFactoryBean;

    @Mock
    TenantFactory tenantFactory;

    @Test
    public void test_createInstance() throws Exception {

        assertNotNull(storageFactoryBean.createInstance());
    }

    @Test
    public void test_getObjectType() throws Exception {

        assertNotNull(storageFactoryBean.getObjectType());
    }

}
