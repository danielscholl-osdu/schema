package org.opengroup.osdu.schema.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.TreeMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class VersionHierarchyUtilTest {

    @Test
    public void testVersionHierarchy() {

        TreeMap<VersionHierarchyUtil, String> sortedMap = new TreeMap<>(
                new VersionHierarchyUtil.SortingVersionComparator());

        sortedMap.put(new VersionHierarchyUtil(0l, 0l, 2l), "test1");
        sortedMap.put(new VersionHierarchyUtil(1l, 2l, 2l), "test2");
        sortedMap.put(new VersionHierarchyUtil(0l, 2l, 2l), "test3");
        assertEquals("test2", sortedMap.firstEntry().getValue());

    }
}
