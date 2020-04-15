package org.opengroup.osdu.schema.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaUtilTest {
    @InjectMocks
    SchemaUtil SchemaUtil;

    @Test
    public void testSchemaBreakingChanges_positive() throws BadRequestException {
        SchemaUtil.checkBreakingChange("{\"key1\":\"value1\"}", "{\"key1\":\"value1\"}");
    }

    @Test
    public void testSchemaBreakingChanges_WithNonBreakingChanges() throws BadRequestException {
        SchemaUtil.checkBreakingChange("{\"key1\":\"value1\",\"key2\":\"value2\"}", "{\"key1\":\"value1\"}");
    }

    @Test
    public void testSchemaBreakingChanges_WithBreakingChanges() throws BadRequestException {
        try {
            SchemaUtil.checkBreakingChange("{\"key1\":\"value3\",\"key2\":\"value2\"}", "{\"key1\":\"value1\"}");
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Breaking changes found, please change schema major version", e.getMessage());
        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testSchemaBreakingChanges_WithInCorrectJson() throws BadRequestException {
        try {
            SchemaUtil.checkBreakingChange("{\"key1\":{value3},\"key2\":\"value2\"}", "{\"key1\":\"value1\"}");
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Bad Input, invalid json", e.getMessage());
        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }
}