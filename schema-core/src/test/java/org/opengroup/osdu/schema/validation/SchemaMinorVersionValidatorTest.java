package org.opengroup.osdu.schema.validation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.util.FileUtils;
import org.opengroup.osdu.schema.util.JSONUtil;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaMinorVersionValidatorTest {
	
	@InjectMocks
	SchemaMinorVersionValidator minorVersionValidator;

	@Mock
	ISchemaInfoStore schemaInfoStore;

	@Mock
	DpsHeaders headers;

	@Mock
	JaxRsDpsLog log;

	@Spy
	JSONUtil jsonUtil;
	
	@Test
	public void testVerifyMinorLevelChanges_WithAllPermissibleChanges() throws IOException, BadRequestException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/minor_level_changes/original.json");

		File folder = new File("src/test/resources/schema_compare/minor_level_changes/pass");

		for (final File fileEntry : folder.listFiles()) {
			System.out.println(fileEntry.getPath());
			String newSchemaSchema = new FileUtils().read(fileEntry);
			minorVersionValidator.validate(sourceSchema, newSchemaSchema);
			assertTrue(true);
		}
	}

	@Test
	public void testVerifyMinorLevelChanges_WithOneOfAttr_WithTitle_OrderChanged() throws IOException, BadRequestException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/minor_level_changes/original-schema-with-oneOf-Title.json");
		String newSchemaSchema = new FileUtils().read("/schema_compare/minor_level_changes/pass-with-title/schema-with-oneOf-Orderchanged-Title.json");
		minorVersionValidator.validate(sourceSchema, newSchemaSchema);
		assertTrue(true);
	}
	
	@Test
	public void testVerifyMinorLevelChanges_WithOneOfAttr_WithTitle_OrderChanged_AdditionalRefAttr() throws IOException, BadRequestException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/minor_level_changes/original-schema-with-oneOf-Title.json");
		String newSchemaSchema = new FileUtils().read("/schema_compare/minor_level_changes/pass-with-title/schema-with-oneOf-Orderchanged-Title-WithRefAttr.json");
		minorVersionValidator.validate(sourceSchema, newSchemaSchema);
		assertTrue(true);
	}
	
	@Test
	public void testVerifyMinorLevelChanges_ComplexSchema_NestedAllOf() throws IOException, BadRequestException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/minor_level_changes/original-complex-oneOf-With-Title.json");
		File folder = new File("src/test/resources/schema_compare/minor_level_changes/pass-complex");

		for (final File fileEntry : folder.listFiles()) {
			System.out.println(fileEntry.getPath());
			String newSchemaSchema = new FileUtils().read(fileEntry);
			minorVersionValidator.validate(sourceSchema, newSchemaSchema);
			assertTrue(true);
		}
	}
	
	@Test
	public void testVerifyMinorLevelChanges_WhenTitleBlockIsMissing() throws IOException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/minor_level_changes/original-schema-with-oneOf-Title.json");

		File folder = new File("src/test/resources/schema_compare/minor_level_changes/fail-with-title");

		for (final File fileEntry : folder.listFiles()) {
			System.out.println(fileEntry.getPath());
			String existingSchema = new FileUtils().read(fileEntry);
			try {
				minorVersionValidator.validate(sourceSchema, existingSchema);
				assertTrue(false);
			} catch (BadRequestException e) {
				System.out.println(e.getErrorMsg());
				assertTrue(true);
			}

		}
	}
	
	@Test
	public void testVerifyMinorLevelChanges_WithAllNonPermissibleChanges() throws IOException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/minor_level_changes/original.json");

		File folder = new File("src/test/resources/schema_compare/minor_level_changes/fail");

		for (final File fileEntry : folder.listFiles()) {
			System.out.println(fileEntry.getPath());
			String existingSchema = new FileUtils().read(fileEntry);
			try {
				minorVersionValidator.validate(sourceSchema, existingSchema);
				assertTrue(false);
			} catch (BadRequestException e) {
				System.out.println(e.getErrorMsg());
				assertTrue(true);
			}

		}
	}
	
	

	@Test(expected = BadRequestException.class)
	public void testVerifyMinorLevelChanges_WhenAdditionalAttribute_Changed_FromTrueToFalse() throws IOException, BadRequestException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/minor_level_changes/fail/schema-wth-addprop-add-false.json");

		String existingSchema = new FileUtils().read("/schema_compare/minor_level_changes/pass/schema-with-addprop-add-true.json");

		minorVersionValidator.validate(sourceSchema, existingSchema);
	}

	@Test
	public void testVerifyMinorLevelChanges_WhenAdditionalAttributeExistAsTrue_ButRemoved() throws IOException, BadRequestException, ApplicationException {

		String existingSchema = new FileUtils().read("/schema_compare/minor_level_changes/pass/schema-with-addprop-add-true.json");

		String sourceSchema = new FileUtils().read("/schema_compare/minor_level_changes/original.json");

		minorVersionValidator.validate(sourceSchema, existingSchema);
	}
}
