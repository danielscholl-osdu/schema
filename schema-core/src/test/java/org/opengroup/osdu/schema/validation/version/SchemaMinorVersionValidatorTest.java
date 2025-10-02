package org.opengroup.osdu.schema.validation.version;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.opengroup.osdu.schema.exceptions.SchemaVersionException;
import org.opengroup.osdu.schema.validation.version.model.SchemaBreakingChanges;
import org.opengroup.osdu.schema.validation.version.model.SchemaPatch;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaMinorVersionValidatorTest {

	@InjectMocks 
	SchemaMinorVersionValidator minorVersionValidator;

	@Test 
	public void testGetType() {
		assertTrue(minorVersionValidator.getType() == SchemaValidationType.MINOR);
	}

	@Test 
	public void testHandleBreakingChanges_NoException() {
		try {
			minorVersionValidator.handleBreakingChanges(new ArrayList<SchemaBreakingChanges>());
			assertTrue(true);
		} catch (SchemaVersionException e) {
			assertTrue(false);
		}
	}

	@Test(expected = SchemaVersionException.class)
	public void testHandleBreakingChanges_Exception() throws SchemaVersionException {
		List<SchemaBreakingChanges> breakingChanges = new ArrayList<SchemaBreakingChanges>();
		breakingChanges.add(new SchemaBreakingChanges(new SchemaPatch(), "It's breaking change"));
		minorVersionValidator.handleBreakingChanges(breakingChanges);
	}
}
