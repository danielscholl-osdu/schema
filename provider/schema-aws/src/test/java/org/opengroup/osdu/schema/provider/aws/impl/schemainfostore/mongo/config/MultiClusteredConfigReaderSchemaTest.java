package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.ssm.SSMManagerUtil;
import static org.hamcrest.CoreMatchers.instanceOf;



@RunWith(MockitoJUnitRunner.class)
public class MultiClusteredConfigReaderSchemaTest {
	
	@InjectMocks
	private MultiClusteredConfigReaderSchema multiClusteredConfigReaderSchema;


	@Test
	public void getDatabaseName_GetsDatabaseName() {
		String environment = "Dev";		
		String expected = environment + "_osdu_schema";
		
		String actual = multiClusteredConfigReaderSchema.getDatabaseName(environment);
		assertEquals(actual, expected);
	}
	
	@Test
	public void MultiClusteredConfigReaderSchema_CreatesObject() {
		MultiClusteredConfigReaderSchema multiClusteredConfigReaderSchemaObeject = new MultiClusteredConfigReaderSchema(new SSMManagerUtil());
		assertThat(multiClusteredConfigReaderSchemaObeject, instanceOf(MultiClusteredConfigReaderSchema.class));
	}
}
