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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperV2;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.aws.models.SchemaInfoDoc;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

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
	public void setUp() {
		ReflectionTestUtils.setField(schemaInfoStore, "sharedTenant", COMMON_TENANT_ID);

		when(queryHelperFactory.getQueryHelperForPartition(Mockito.any(String.class), Mockito.any()))
				.thenReturn(queryHelper);

		when(queryHelperFactory.getQueryHelperForPartition(Mockito.any(DpsHeaders.class), Mockito.any()))
				.thenReturn(queryHelper);
	}

	@Test
	public void isUnique_ifNotExists_returnTrue() {
		String partitionId = "partitionId";
		String schemaId = "schemaId";
		when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);
		Boolean actual = schemaInfoStore.isUnique(schemaId, partitionId);
		assertEquals(true, actual);
	}

	@Test
	public void isUnique_ifNotExists_returnTrue_SystemSchema() {
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
	public void isUnique_ifExists_returnFalse() {
		String partitionId = "partitionId";
		String schemaId = "schemaId";
		when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(true);
		Boolean actual = schemaInfoStore.isUnique(schemaId, partitionId);
		assertEquals(false, actual);
	}

	@Test
	public void isUnique_ifExists_returnFalse_SystemSchema() {
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

	@Test
	public void getSchemaInfo_GetsSchemaInfro() throws NotFoundException {
		String schemaId = "schemaId";
		SchemaInfo schemaInfo = new SchemaInfo();
		SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc(null, null, null, null, null, null, null, null, null, null,
				null, schemaInfo);

		when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(schemaInfoDoc);
		SchemaInfo actual = schemaInfoStore.getSchemaInfo(schemaId);
		assertEquals(schemaInfo, actual);
	}

	@Test(expected = NotFoundException.class)
	public void getSchemaInfo_ThrowsException() throws NotFoundException {
		String schemaId = "schemaId";

		when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(null);
		schemaInfoStore.getSchemaInfo(schemaId);
	}

	@Test
	public void getSystemSchemaInfo_GetsSystemSchemaInfo() throws NotFoundException {
		String schemaId = "schemaId";
		SchemaInfo schemaInfo = new SchemaInfo();
		SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc(null, null, null, null, null, null, null, null, null, null,
				null, schemaInfo);

		when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(schemaInfoDoc);
		SchemaInfo actual = schemaInfoStore.getSystemSchemaInfo(schemaId);
		assertEquals(schemaInfo, actual);
	}

	@Test(expected = BadRequestException.class)
	public void updateSchemaInfo_UpdatesSchemaInfo() throws ApplicationException{
		SchemaIdentity schemaIdentity = new SchemaIdentity(null, null, null, null, null, null, "schemaId");
		SchemaInfo schemaInfo = new SchemaInfo(schemaIdentity, null, null, SchemaStatus.DEVELOPMENT,
				SchemaScope.INTERNAL, new SchemaIdentity());
		SchemaRequest schemaRequest = new SchemaRequest(schemaInfo, null);

		SchemaInfo schemaInfo1 =  schemaInfoStore.updateSchemaInfo(schemaRequest);
		assertNotNull(schemaInfo1);
	}

	@Test(expected = BadRequestException.class)
	public void updateSystemSchemaInfo_UpdatesSystemSchemaInfo() throws ApplicationException {
		SchemaIdentity schemaIdentity = new SchemaIdentity(null, null, null, null, null, null, "schemaId");
		SchemaInfo schemaInfo = new SchemaInfo(schemaIdentity, null, null, SchemaStatus.DEVELOPMENT,
				SchemaScope.INTERNAL, new SchemaIdentity());
		SchemaRequest schemaRequest = new SchemaRequest(schemaInfo, null);

		SchemaInfo schemaInfo1 = schemaInfoStore.updateSystemSchemaInfo(schemaRequest);
		assertNotNull(schemaInfo1);
	}

	@Test(expected = BadRequestException.class)
	public void createSchemaInfo_throwsException() throws ApplicationException, BadRequestException {
		SchemaIdentity schemaIdentity = new SchemaIdentity(null, null, null, null, null, null, "schemaId");
		SchemaInfo schemaInfo = new SchemaInfo(schemaIdentity, null, null, SchemaStatus.DEVELOPMENT,
				SchemaScope.INTERNAL, new SchemaIdentity());
		SchemaRequest schemaRequest = new SchemaRequest(schemaInfo, null);

		when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(true);
		schemaInfoStore.createSchemaInfo(schemaRequest);
	}

	@Test(expected =  BadRequestException.class)
	public void createSchemaInfo_ThrowsApplicationException() throws ApplicationException, BadRequestException {
		SchemaIdentity schemaIdentity = new SchemaIdentity(null, null, null, null, null, null, "schemaId");
		SchemaInfo schemaInfo = new SchemaInfo(schemaIdentity, null, null, SchemaStatus.DEVELOPMENT,
				SchemaScope.INTERNAL, new SchemaIdentity());
		SchemaRequest schemaRequest = new SchemaRequest(schemaInfo, null);

		when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);

		SchemaInfo schemaInfo1 = schemaInfoStore.createSchemaInfo(schemaRequest);
		assertNotNull(schemaInfo1);
	}

	@Test(expected = BadRequestException.class)
	public void createSystemSchemaInfo_ThrowsApplicationException() throws ApplicationException, BadRequestException {
		SchemaIdentity schemaIdentity = new SchemaIdentity(null, null, null, null, null, null, "schemaId");
		SchemaInfo schemaInfo = new SchemaInfo(schemaIdentity, null, null, SchemaStatus.DEVELOPMENT,
				SchemaScope.INTERNAL, new SchemaIdentity());
		SchemaRequest schemaRequest = new SchemaRequest(schemaInfo, null);

		when(queryHelper.keyExistsInTable(eq(SchemaInfoDoc.class), Mockito.any())).thenReturn(false);
		SchemaInfo schemaInfo1 = schemaInfoStore.createSystemSchemaInfo(schemaRequest);
		assertNotNull(schemaInfo1);
	}

	@Test
	public void getLatestMinorVerSchema() throws ApplicationException{
 		SchemaIdentity schemaIdentity = new SchemaIdentity(null, null, null, null, 1001L, null, "schema_id");
 		SchemaInfo schemaInfo = new SchemaInfo(schemaIdentity, "user@opendes.com", new Date(),
 				SchemaStatus.PUBLISHED, SchemaScope.INTERNAL, new SchemaIdentity());

 		when(queryHelper.queryByGSI(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(null);
 		String actual = schemaInfoStore.getLatestMinorVerSchema(schemaInfo);
 		assertEquals("", actual);
 	}
	
	@Test
	public void getSchemaInfoList() {
		String tenantId = "tenantId";
		QueryParams queryParams = new QueryParams("authority", "source", "entityType", 10L, 20L, 30L, 3, 3, "status",
				"scope", true);
		List<SchemaInfo> expected = new ArrayList<SchemaInfo>();
		List<SchemaInfo> actual = schemaInfoStore.getSchemaInfoList(queryParams, tenantId);
		assertEquals(expected, actual);
	}
	@Test
	public void getSchemaInfoListWithEmptyQueryParams() {
		String tenantId = "tenantId";
		QueryParams queryParams = new QueryParams(null, null, null, null, null, null, 3,3, null,
				"scope", true);
		List<SchemaInfo> expected = new ArrayList<SchemaInfo>();
		List<SchemaInfo> actual = schemaInfoStore.getSchemaInfoList(queryParams, tenantId);
		assertEquals(expected, actual);
	}
	@Test
	public void getSystemSchemaInfoList() throws BadRequestException {
		QueryParams queryParams = new QueryParams("authority", "source", "entityType", 10L, 20L, 30L, 3, 3, "status",
				"scope", false);
		List<SchemaInfo> expected = new ArrayList<SchemaInfo>();

		List<SchemaInfo> actual = schemaInfoStore.getSystemSchemaInfoList(queryParams);
		assertEquals(expected, actual);
	}

	@Test
	public void cleanSchema_ReturnsTrue() {
		String schemaId = "schemaId";
		doNothing().when(queryHelper).deleteByPrimaryKey(Mockito.any(), Mockito.any());
		boolean actual = schemaInfoStore.cleanSchema(schemaId);
		assertTrue(actual);
	}
	
	@Test
	public void cleanSchema_ReturnsFalseOnException() {
		String schemaId = "schemaId";
		doThrow(new RuntimeException()).when(queryHelper).deleteByPrimaryKey(Mockito.any(), Mockito.any());
		
		boolean actual = schemaInfoStore.cleanSchema(schemaId);
		assertFalse(actual);
	}

	@Test
	public void cleanSystemSchema()  {
		String schemaId = "schemaId";
		doNothing().when(queryHelper).deleteByPrimaryKey(Mockito.any(), Mockito.any());
		boolean actual = schemaInfoStore.cleanSystemSchema(schemaId);
		assertTrue(actual);
	}
}