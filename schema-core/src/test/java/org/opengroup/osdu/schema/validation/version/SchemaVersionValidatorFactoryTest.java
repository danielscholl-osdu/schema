package org.opengroup.osdu.schema.validation.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaVersionValidatorFactoryTest {
	
	@InjectMocks 
	private SchemaVersionValidatorFactory schemaVersionValidatorFactory;
	
	@Mock
	private List<VersionValidator> versionValidator;
	
	@Test
	public void testGetPatchValidator() {
		VersionValidator[] arr = { new SchemaPatchVersionValidator() };
		Mockito.when(versionValidator.stream()).thenReturn(Arrays.stream(arr));
		assertThat(schemaVersionValidatorFactory.getVersionValidator(SchemaValidationType.PATCH)).isNotNull();
	}
	
	@Test
	public void testGetMinorValidator() {
		VersionValidator[] arr = { new SchemaMinorVersionValidator() };
		Mockito.when(versionValidator.stream()).thenReturn(Arrays.stream(arr));
		assertThat(schemaVersionValidatorFactory.getVersionValidator(SchemaValidationType.MINOR)).isNotNull();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testGetVersionValidator_Fail() {
		VersionValidator[] arr = { new SchemaMinorVersionValidator() };
		Mockito.when(versionValidator.stream()).thenReturn(Arrays.stream(arr));
		schemaVersionValidatorFactory.getVersionValidator(SchemaValidationType.COMMON);
	}
}
