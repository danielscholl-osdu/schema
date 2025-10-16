package org.opengroup.osdu.schema.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class EntitlementClientFactoryTest {

    @InjectMocks
    EntitlementsClientFactory entitlementsClientFactory;

    @Test
    public void test_createInstance() throws Exception {

        assertNotNull(entitlementsClientFactory.createInstance());
    }

    @Test
    public void test_getObjectType() throws Exception {

        assertNotNull(entitlementsClientFactory.getObjectType());
    }

}
