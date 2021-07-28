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
public class SchemaPatchVersionValidatorTest {

	@InjectMocks
	SchemaPatchVersionValidator patchVersionValidator;

	@Mock
	ISchemaInfoStore schemaInfoStore;

	@Mock
	DpsHeaders headers;

	@Mock
	JaxRsDpsLog log;

	@Spy
	JSONUtil jsonUtil;
	
	@Test
	public void testVerifyPatchLevelChanges_WithAllPermissibleChanges() throws IOException, BadRequestException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/original-schema.json");

		File folder = new File("src/test/resources/schema_compare/patch_level_changes");

		for (final File fileEntry : folder.listFiles()) {
			System.out.println(fileEntry.getPath());
			String existingSchema = new FileUtils().read(fileEntry);
			patchVersionValidator.validate(sourceSchema, existingSchema);
			assertTrue(true);
		}
	}

	@Test
	public void testVerifyMinorLevelChanges_WithAllNonPermissibleChanges() throws IOException, ApplicationException {
		String sourceSchema = new FileUtils().read("/schema_compare/original-schema.json");

		File folder = new File("src/test/resources/schema_compare/minor_level_changes/fail");

		for (final File fileEntry : folder.listFiles()) {
			System.out.println(fileEntry.getPath());
			String existingSchema = new FileUtils().read(fileEntry);
			try {
				patchVersionValidator.validate(sourceSchema, existingSchema);
				assertTrue(false);
			} catch (BadRequestException e) {
				System.out.println(e.getErrorMsg());
				assertTrue(true);
			}

		}
	}
	
}
