// Copyright © 2020 Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperV2;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.aws.models.EntityTypeDoc;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AwsEntityTypeStoreTest {

	@InjectMocks
	private AwsEntityTypeStore entityTypeStore;

	@Mock
	private DpsHeaders headers;

	@Mock
	private DynamoDBQueryHelperFactory queryHelperFactory;

	@Mock
	private DynamoDBQueryHelperV2 queryHelper;

	@Mock
	private JaxRsDpsLog logger;

	private static final String COMMON_TENANT_ID = "common";

	@Before
	public void setUp() throws Exception {

		Mockito.when(queryHelperFactory.getQueryHelperForPartition(Mockito.any(DpsHeaders.class), Mockito.any()))
				.thenReturn(queryHelper);
		ReflectionTestUtils.setField(entityTypeStore, "sharedTenant", COMMON_TENANT_ID);

	}

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void get() throws NotFoundException, ApplicationException {
		String entityTypeId = "id";
		String partitionId = "partitionId";
		EntityType expected = new EntityType();
		EntityTypeDoc entityTypeDoc = new EntityTypeDoc("id", partitionId, expected);

		Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(entityTypeDoc);
		Mockito.when(headers.getPartitionId()).thenReturn(partitionId);

		EntityType actual = entityTypeStore.get(entityTypeId);
		Assert.assertEquals(expected, actual);
	}

	@Test(expected = NotFoundException.class)
	public void getThrowsNotFoundException() throws NotFoundException, ApplicationException {
		String entityTypeId = "id";
		String partitionId = "partitionId";

		Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(null);
		Mockito.when(headers.getPartitionId()).thenReturn(partitionId);

		entityTypeStore.get(entityTypeId);
	}

	@Test
	public void get_SystemSchemas() throws NotFoundException, ApplicationException {
		String entityTypeId = "id";
		EntityType expected = new EntityType();
		EntityTypeDoc entityTypeDoc = new EntityTypeDoc("id", COMMON_TENANT_ID, expected);

		Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(entityTypeDoc);
		Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);

		EntityType actual = entityTypeStore.get(entityTypeId);
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void create() throws BadRequestException, ApplicationException {
		String partitionId = "partitionId";
		EntityType expected = new EntityType();

		Mockito.when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);
		Mockito.doNothing().when(queryHelper).save(Mockito.any());
		Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
		EntityType actual = entityTypeStore.create(expected);
		assertEquals(expected, actual);
	}

	@Test(expected = BadRequestException.class)
	public void createHandleskeyExistsInTable() throws BadRequestException, ApplicationException {
		String partitionId = "partitionId";
		EntityType expected = new EntityType();

		Mockito.when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(true);
		Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
		entityTypeStore.create(expected);
	}
	
	@Test
	public void create_SystemSchemas() throws BadRequestException, ApplicationException {
		EntityType expected = new EntityType();

		Mockito.when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);
		Mockito.doNothing().when(queryHelper).save(Mockito.any());
		Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
		EntityType actual = entityTypeStore.create(expected);
		assertEquals(expected, actual);
	}
}