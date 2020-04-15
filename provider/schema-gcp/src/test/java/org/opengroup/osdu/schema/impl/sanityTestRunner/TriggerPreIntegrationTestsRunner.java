package org.opengroup.osdu.schema.impl.sanityTestRunner;

import org.junit.Test;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.opengroup.osdu.schema.runner.PreIntegrationTestsRunner;

public class TriggerPreIntegrationTestsRunner {
    @Test
    public void testError() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new TextListener(System.out));
        junit.run(PreIntegrationTestsRunner.class);
    }
}
