package org.opengroup.osdu.schema.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaUtilTest {
	@InjectMocks
	SchemaUtil schemaUtil;

	@Mock
	ISchemaInfoStore schemaInfoStore;

	@Mock
	DpsHeaders headers;

	@Mock
	JaxRsDpsLog log;

	@Spy
	JSONUtil jsonUtil;

	@Value("${shared.tenant.name:common}")
	private String sharedTenant;

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(schemaUtil, "sharedTenant", "common");
	}

	/***
	 * Mock SchemaInfoList with versions: [1.1.0], [1.4.7],[1.6.6],[1.27.5]
	 */
	private List<SchemaInfo> getMockedList(){
		List<SchemaInfo> schemaInfoList = new ArrayList<SchemaInfo>();
		SchemaInfo schInf = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(1l)
				.schemaVersionPatch(0l).build()).build();
		schemaInfoList.add(schInf);

		schInf = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(27l)
				.schemaVersionPatch(5l).build()).build();
		schemaInfoList.add(schInf);

		schInf = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(6l)
				.schemaVersionPatch(6l).build()).build();
		schemaInfoList.add(schInf);

		schInf = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(4l)
				.schemaVersionPatch(7l).build()).build();
		schemaInfoList.add(schInf);

		return schemaInfoList;
	}

	@Test
	public void testFindSchemaWithClosestAndLatestPatchVersion_ReturnNull() throws BadRequestException, ApplicationException {

		SchemaIdentity schemaIdentity = SchemaIdentity.builder().schemaVersionMajor(1l).schemaVersionMinor(1l)
				.schemaVersionPatch(0l).build();
		SchemaInfo schInf = SchemaInfo.builder().schemaIdentity(schemaIdentity).build();

		Mockito.when(schemaInfoStore.getSchemaInfoList( Mockito.any(QueryParams.class), Mockito.anyString()))
		.thenReturn(new LinkedList<>());
		Mockito.when(headers.getPartitionId()).thenReturn("opendes");
		SchemaInfo[] schemaInfoArr = schemaUtil.findSchemaToCompare(schInf);
		assertNull(schemaInfoArr[0]);
		assertNull(schemaInfoArr[1]);


	}

	@Test
	public void testFindSchemaWithClosestAndLatestPatchVersion_ReturnsLatestPatch() throws BadRequestException, ApplicationException {
		List<SchemaInfo> schemaInfoList = new ArrayList<>();
		SchemaInfo inputSchemaInfo = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(1l)
				.schemaVersionPatch(0l).build()).build();

		SchemaInfo schInfLatest = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(0l)
				.schemaVersionPatch(0l).build()).build();

		schemaInfoList.add(schInfLatest);
		Mockito.when(schemaInfoStore.getSchemaInfoList( Mockito.any(QueryParams.class), Mockito.anyString()))
		.thenReturn(schemaInfoList);
		Mockito.when(headers.getPartitionId()).thenReturn("opendes");
		SchemaInfo[] schemaInfoArr = schemaUtil.findSchemaToCompare(inputSchemaInfo);
		assertEquals(schemaInfoArr[0], schInfLatest);
		assertNull(schemaInfoArr[1]);



	}

	/****
	 * Example1:
	 * Existing version in the system: [1.1.0], [1.4.7],[1.6.6],[1.27.5]
	 * Incoming version: 1.4.0
	 * Records fetched to compare: [1.1.0], [1.4.7],[1.6.6],[1.27.5]
	 * Comparison happens with: [1.1.0 Vs 1.4.0] & [1.4.0 Vs 1.4.7]
	 * 
	 */
	@Test
	public void testFindSchemaWithClosestMinorVersion_ReturnsLatestMinor() throws BadRequestException, ApplicationException {


		SchemaIdentity schId = SchemaIdentity.builder()
				.authority("test")
				.source("test")
				.entityType("test")
				.schemaVersionMajor(1l)
				.schemaVersionMinor(4l)
				.schemaVersionPatch(0l).build();
		SchemaInfo inputSchemaInfo = SchemaInfo.builder().schemaIdentity(schId).build();

		SchemaInfo smaller = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(1l)
				.schemaVersionPatch(0l).build()).build();

		SchemaInfo bigger = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(4l)
				.schemaVersionPatch(7l).build()).build();

		QueryParams latestPatchQueryParams = QueryParams.builder().authority(schId.getAuthority())
				.source(schId.getSource())
				.entityType(schId.getEntityType())
				.schemaVersionMajor(schId.getSchemaVersionMajor())
				.schemaVersionMinor(schId.getSchemaVersionMinor())
				.latestVersion(true).build();

		QueryParams queryParamsForAllMajorVersion = QueryParams.builder().authority(schId.getAuthority())
				.source(schId.getSource())
				.entityType(schId.getEntityType())
				.schemaVersionMajor(schId.getSchemaVersionMajor()).build();

		Mockito.when(schemaInfoStore.getSchemaInfoList( latestPatchQueryParams, "opendes"))
		.thenReturn(new LinkedList<>());

		Mockito.when(schemaInfoStore.getSchemaInfoList( latestPatchQueryParams, "common"))
		.thenReturn(new LinkedList<>());

		Mockito.when(schemaInfoStore.getSchemaInfoList( queryParamsForAllMajorVersion, "opendes"))
		.thenReturn(getMockedList());

		Mockito.when(headers.getPartitionId()).thenReturn("opendes");
		SchemaInfo[] schemaInfoArr = schemaUtil.findSchemaToCompare(inputSchemaInfo);
		assertEquals(schemaInfoArr[0], smaller);
		assertEquals(schemaInfoArr[1], bigger);


	}

	/****
	 * Existing version in the system: [1.1.0], [1.4.7],[1.6.6],[1.27.5]
	 * Incoming version: 1.30.0
	 * Records fetched to compare: [1.1.0], [1.4.7],[1.6.6],[1.27.5]
	 * Comparison happens with: 1.27.5 Vs 1.30.0
	 * 
	 */
	@Test
	public void testFindSchemaWithClosestMinorVersion_ReturnsClosesGreaterMinor() throws BadRequestException, ApplicationException {


		SchemaIdentity schId = SchemaIdentity.builder()
				.authority("test")
				.source("test")
				.entityType("test")
				.schemaVersionMajor(1l)
				.schemaVersionMinor(30l)
				.schemaVersionPatch(0l).build();
		SchemaInfo inputSchemaInfo = SchemaInfo.builder().schemaIdentity(schId).build();

		SchemaInfo schInfLatest = SchemaInfo.builder().schemaIdentity(SchemaIdentity.builder()
				.schemaVersionMajor(1l)
				.schemaVersionMinor(27l)
				.schemaVersionPatch(5l).build()).build();

		QueryParams latestPatchqueryParams = QueryParams.builder().authority(schId.getAuthority())
				.source(schId.getSource())
				.entityType(schId.getEntityType())
				.schemaVersionMajor(schId.getSchemaVersionMajor())
				.schemaVersionMinor(schId.getSchemaVersionMinor())
				.latestVersion(true).build();

		QueryParams queryParamsForAllMajorVersion = QueryParams.builder().authority(schId.getAuthority())
				.source(schId.getSource())
				.entityType(schId.getEntityType())
				.schemaVersionMajor(schId.getSchemaVersionMajor()).build();

		Mockito.when(schemaInfoStore.getSchemaInfoList( latestPatchqueryParams, "opendes"))
		.thenReturn(new LinkedList<>());

		Mockito.when(schemaInfoStore.getSchemaInfoList( queryParamsForAllMajorVersion, "opendes"))
		.thenReturn(getMockedList());
		Mockito.when(headers.getPartitionId()).thenReturn("opendes");
		SchemaInfo[] schemaInfoArr = schemaUtil.findSchemaToCompare(inputSchemaInfo);
		assertEquals(schemaInfoArr[0], schInfLatest);
		assertNull(schemaInfoArr[1]);
	}

}