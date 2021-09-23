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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperV2;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

@RunWith(MockitoJUnitRunner.class)
public class AwsSchemaInfoStoreTest {

	@InjectMocks
	private AwsSchemaInfoStore schemaInfoStore;

	@Mock
	private DpsHeaders headers;

	@Mock
	private DynamoDBQueryHelperFactory queryHelperFactory;

	@Mock
	private DynamoDBQueryHelperV2 queryHelper;

	@Mock
	private JaxRsDpsLog logger;

	@Mock
	private ITenantFactory tenantFactory;

	private static final String COMMON_TENANT_ID = "common";

	@Before
	public void setUp() throws Exception {

		ReflectionTestUtils.setField(schemaInfoStore, "sharedTenant", COMMON_TENANT_ID);

		when(queryHelperFactory.getQueryHelperForPartition(Mockito.any(String.class), Mockito.any()))
		.thenReturn(queryHelper);

	}

	@Test
	public void isUnique_ifNotExists_returnTrue() throws ApplicationException {
		String partitionId = "partitionId";
		String schemaId = "schemaId";
		when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);
		Boolean actual = schemaInfoStore.isUnique(schemaId, partitionId);
		assertEquals(true, actual);
	}

	@Test
	public void isUnique_ifNotExists_returnTrue_SystemSchema() throws ApplicationException {
		String schemaId = "schemaId";

		TenantInfo tenant1 = new TenantInfo();
		tenant1.setName(COMMON_TENANT_ID);
		tenant1.setDataPartitionId(COMMON_TENANT_ID);
		TenantInfo tenant2 = new TenantInfo();
		tenant2.setName("partitionId");
		tenant2.setDataPartitionId("partitionId");
		Collection<TenantInfo> tenants = Lists.newArrayList(tenant1, tenant2);
		when(this.tenantFactory.listTenantInfo()).thenReturn(tenants);

		when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);
		Boolean actual = schemaInfoStore.isUniqueSystemSchema(schemaId);
		assertEquals(true, actual);
	}

	@Test
	public void isUnique_ifExists_returnFalse() throws ApplicationException {
		String partitionId = "partitionId";
		String schemaId = "schemaId";
		when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(true);
		Boolean actual = schemaInfoStore.isUnique(schemaId, partitionId);
		assertEquals(false, actual);
	}

	@Test
	public void isUnique_ifExists_returnFalse_SystemSchema() throws ApplicationException {
		String schemaId = "schemaId";
		TenantInfo tenant1 = new TenantInfo();
		tenant1.setName(COMMON_TENANT_ID);
		tenant1.setDataPartitionId(COMMON_TENANT_ID);
		TenantInfo tenant2 = new TenantInfo();
		tenant2.setName("partitionId");
		tenant2.setDataPartitionId("partitionId");
		Collection<TenantInfo> tenants = Lists.newArrayList(tenant1, tenant2);
		when(this.tenantFactory.listTenantInfo()).thenReturn(tenants);
		
		when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(true);
		Boolean actual = schemaInfoStore.isUniqueSystemSchema(schemaId);
		assertEquals(false, actual);
	}
}