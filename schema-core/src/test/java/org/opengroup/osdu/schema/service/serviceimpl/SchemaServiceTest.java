package org.opengroup.osdu.schema.service.serviceimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NoSchemaFoundException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.exceptions.SchemaVersionException;
import org.opengroup.osdu.schema.logging.AuditLogger;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.opengroup.osdu.schema.service.IAuthorityService;
import org.opengroup.osdu.schema.service.IEntityTypeService;
import org.opengroup.osdu.schema.service.ISourceService;
import org.opengroup.osdu.schema.util.FileUtils;
import org.opengroup.osdu.schema.util.SchemaResolver;
import org.opengroup.osdu.schema.util.SchemaUtil;
import org.opengroup.osdu.schema.validation.version.SchemaMinorVersionValidator;
import org.opengroup.osdu.schema.validation.version.SchemaPatchVersionValidator;
import org.opengroup.osdu.schema.validation.version.SchemaValidationType;
import org.opengroup.osdu.schema.validation.version.SchemaVersionValidatorFactory;
import org.opengroup.osdu.schema.validation.version.VersionValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaServiceTest {

	@InjectMocks
	SchemaService schemaService;

	@Mock
	ISchemaInfoStore schemaInfoStore;

	@Mock
	ISchemaStore schemaStore;

	@Mock
	IAuthorityService authorityService;

	@Mock
	ISourceService sourceService;

	@Mock
	IEntityTypeService entityTypeService;

	@Mock
	SchemaUtil schemaUtil;

	@Mock
	SchemaResolver schemaResolver;

	@Mock
	DpsHeaders headers;

	@Mock
	JaxRsDpsLog log;

	@Mock
	AuditLogger auditLogger;

	@Mock
	SchemaVersionValidatorFactory schemaVersionValidatorFactory;

	@Mock
	IMessageBus messageBus;

	@Mock
	SchemaMinorVersionValidator minorVersionValidator;

	@Mock
	SchemaPatchVersionValidator patchVersionValidator;

	private Date currDate = new Date();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		schemaService.setSchemaResolver(schemaResolver);
	}

	@Test
	public void testGetSchema_EmptySchemaId() throws BadRequestException, NotFoundException, ApplicationException {
		String schemaId = "";
		expectedException.expect(BadRequestException.class);
		expectedException.expectMessage(SchemaConstants.EMPTY_ID);
		schemaService.getSchema(schemaId);
	}

	@Test
	public void testGetSchema_FetchPrivateProjectTest()
			throws BadRequestException, NotFoundException, ApplicationException {
		String dataPartitionId = "private";
		Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
		String schemaId = "os..wks..well.1.1";
		Mockito.when(schemaStore.getSchema(dataPartitionId, schemaId)).thenReturn("{}");
		assertNotNull(schemaService.getSchema(schemaId));
	}

	@Test
	public void testGetSchema_FetchCommonProjectTest()
			throws BadRequestException, NotFoundException, ApplicationException {
		String dataPartitionId = "private";
		Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
		String schemaId = "os..wks..well.1.1";
		Mockito.when(schemaStore.getSchema(dataPartitionId, schemaId)).thenThrow(NotFoundException.class);
		Mockito.when(schemaStore.getSystemSchema(schemaId)).thenReturn("{}");
		assertNotNull(schemaService.getSchema(schemaId));
	}

	@Test
	public void testGetSchema_NotFoundException() throws BadRequestException, NotFoundException, ApplicationException {
		expectedException.expect(NotFoundException.class);
		expectedException.expectMessage(SchemaConstants.SCHEMA_NOT_PRESENT);
		String dataPartitionId = "private";
		Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
		String schemaId = "os..wks..well.1.1";
		Mockito.when(schemaStore.getSchema(dataPartitionId, schemaId)).thenThrow(NotFoundException.class);
		Mockito.when(schemaStore.getSystemSchema(schemaId))
				.thenThrow(new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT));
		schemaService.getSchema(schemaId);
	}

	@Test
	public void testCreateSchema_withPrivateSchema()
			throws ApplicationException, NotFoundException, BadRequestException, JsonProcessingException {
		String dataPartitionId = "tenant";
		String schemaId = "os:wks:well:1.1.1";

		SchemaRequest schReqPubInt = getMockSchemaObject_published_InternalScope();
		SchemaInfo schInfo =  getMockSchemaInfo_Published_InternalScope();

		when(schemaInfoStore.isUniqueSystemSchema(schemaId)).thenReturn(true);
		when(schemaInfoStore.isUnique(schemaId, "tenant")).thenReturn(true);
		when(headers.getPartitionId()).thenReturn(dataPartitionId);
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schInfo;
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
						schReqPubInt.getSchemaInfo().getSchemaIdentity().getAuthority()))
				.thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
						schReqPubInt.getSchemaInfo().getSchemaIdentity().getSource()))
				.thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
						schReqPubInt.getSchemaInfo().getSchemaIdentity().getEntityType()))
				.thenReturn(true);
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaStore.createSchema(Mockito.anyString(), Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaInfoStore.createSchemaInfo(schReqPubInt)).thenReturn(schInfo);
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn(schReqPubInt.getSchema().toString());

		Mockito.when(schemaUtil.findSchemaToCompare(schReqPubInt.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		assertEquals(SchemaStatus.PUBLISHED, schemaService.createSchema(schReqPubInt).getStatus());
		verify(messageBus, times(1)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testCreateSchema_SuccessScenario()
			throws ApplicationException, NotFoundException, BadRequestException, JsonProcessingException {
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		String schemaId = "os:wks:well:1.1.1";

		SchemaRequest schReq = getMockSchemaObject_published_InternalScope();
		SchemaInfo schInfo = getMockSchemaInfo_Published_InternalScope();

		when(schemaInfoStore.isUniqueSystemSchema(schemaId)).thenReturn(true);
		when(schemaInfoStore.isUnique(schemaId, "tenant")).thenReturn(true);
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schInfo;
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getAuthority()))
				.thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getSource()))
				.thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getEntityType()))
				.thenReturn(true);
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaStore.createSchema(Mockito.anyString(), Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaInfoStore.createSchemaInfo(schReq))
				.thenReturn(schInfo);
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn(schReq.getSchema().toString());

		Mockito.when(schemaUtil.findSchemaToCompare(schReq.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		assertEquals(schInfo, schemaService.createSchema(schReq));
		verify(messageBus, times(1)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testCreateSchema_FailScenario_CleanUpScenario()
			throws ApplicationException, NotFoundException, BadRequestException, JsonProcessingException {
		expectedException.expect(ApplicationException.class);

		SchemaRequest schReq = getMockSchemaObject_published_InternalScope();

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		String schemaId = "os:wks:well:1.1.1";
		when(schemaInfoStore.isUniqueSystemSchema(schemaId)).thenReturn(true);
		when(schemaInfoStore.isUnique(schemaId, "tenant")).thenReturn(true);
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schReq.getSchemaInfo();
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getAuthority()))
				.thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getSource()))
				.thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getEntityType()))
				.thenReturn(true);
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaStore.createSchema(Mockito.anyString(), Mockito.anyString())).thenThrow(ApplicationException.class);


		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn(schReq.getSchema().toString());

		Mockito.when(schemaUtil.findSchemaToCompare(schReq.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		schemaService.createSchema(schReq);
		verify(messageBus, times(0)).publishMessage(anyString(), anyString());
	}

	@Test
	public void testCreateSchema_SharedSchema()
			throws JsonProcessingException, ApplicationException, BadRequestException, NotFoundException {
		String dataPartitionId = "common";

		SchemaRequest  schReqInt = getMockSchemaObject_published_InternalScope();
		SchemaRequest schReqPub = getMockSchemaObject_published();
		SchemaInfo schInfoPub = getMockSchemaInfo_Published_SharedScope();

		Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
		when(schemaInfoStore.isUnique(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schInfoPub;
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
						schReqInt.getSchemaInfo().getSchemaIdentity().getAuthority()))
				.thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
						schReqInt.getSchemaInfo().getSchemaIdentity().getSource()))
				.thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
						schReqInt.getSchemaInfo().getSchemaIdentity().getEntityType()))
				.thenReturn(true);
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaStore.createSchema(Mockito.anyString(), Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaInfoStore.createSchemaInfo(schReqPub)).thenReturn(schInfoPub);
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn(schReqPub.getSchema().toString());

		Mockito.when(schemaUtil.findSchemaToCompare(schReqPub.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		assertEquals(SchemaStatus.PUBLISHED, schemaService.createSchema(schReqPub).getStatus());
		verify(messageBus, times(1)).publishMessage( anyString(), anyString());

	}

	public void testCreateSchema_Private_SchemaAlreadyRegistered()
			throws JsonProcessingException, ApplicationException, BadRequestException, NotFoundException {
		expectedException.expect(BadRequestException.class);
		expectedException.expectMessage(SchemaConstants.SCHEMA_ID_EXISTS);
		String schemaId = getMockSchemaObject_published().getSchemaInfo().getSchemaIdentity().getId();
		String tenantId = "testing";
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
						getMockSchemaObject_published_InternalScope().getSchemaInfo().getSchemaIdentity().getAuthority()))
				.thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
						getMockSchemaObject_published_InternalScope().getSchemaInfo().getSchemaIdentity().getSource()))
				.thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
						getMockSchemaObject_published_InternalScope().getSchemaInfo().getSchemaIdentity().getEntityType()))
				.thenReturn(true);
		Mockito.when(schemaInfoStore.isUnique(schemaId, tenantId)).thenReturn(false);
		schemaService.createSchema(getMockSchemaObject_published());
		verify(messageBus, times(0)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testCreateSchema_withBreakingChanges_FoundScenario()
			throws ApplicationException, NotFoundException, BadRequestException, IOException {
		expectedException.expect(BadRequestException.class);
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		ObjectMapper mapper = new ObjectMapper();
		SchemaRequest schReqBreakingChange = getMockSchemaObject_BreakingChanges();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schReqBreakingChange.getSchemaInfo();
		String inputSchema = mapper.writeValueAsString(schReqBreakingChange.getSchema());
		String latestSchema = getMockLatestSchema_BreakingChanges();
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn(latestSchema);
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn(inputSchema);
		Mockito.when(schemaUtil.findSchemaToCompare(schReqBreakingChange.getSchemaInfo(), false)).thenReturn(schemaInfoArr);

		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(SchemaValidationType.PATCH))
				.thenReturn(patchVersionValidator);
		Mockito.doThrow(SchemaVersionException.class).when(patchVersionValidator).validateVersionChange(anyString(), anyString());
		Mockito.when(schemaInfoStore.getLatestMinorVerSchema(getMockSchemaInfo_Published_InternalScope()))
				.thenReturn(latestSchema);
		schemaService.createSchema(schReqBreakingChange);
		verify(messageBus, times(0)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testCreateSchema_withBreakingChanges_NotFoundScenario()
			throws ApplicationException, NotFoundException, BadRequestException, IOException {
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		SchemaRequest mockSchReqPubInt = getMockSchemaObject_published_InternalScope();
		SchemaInfo mockSchInfoPubInt = getMockSchemaInfo_Published_InternalScope();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=mockSchInfoPubInt;
		SchemaRequest schReqBreakingChange = getMockSchemaObject_BreakingChanges();
		ObjectMapper mapper = new ObjectMapper();
		String inputSchema = mapper.writeValueAsString(schReqBreakingChange.getSchema());
		String latestSchema = "{\"key\":\"value1\"}";
		when(schemaInfoStore.isUniqueSystemSchema("os:wks:well:1.1.1")).thenReturn(true);
		when(schemaInfoStore.isUnique("os:wks:well:1.1.1", "tenant")).thenReturn(true);
		Mockito.when(schemaInfoStore.getLatestMinorVerSchema(mockSchInfoPubInt))
				.thenReturn(latestSchema);
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
						mockSchReqPubInt.getSchemaInfo().getSchemaIdentity().getAuthority()))
				.thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
						mockSchReqPubInt.getSchemaInfo().getSchemaIdentity().getSource()))
				.thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
						mockSchReqPubInt.getSchemaInfo().getSchemaIdentity().getEntityType()))
				.thenReturn(true);
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn(latestSchema);
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn(inputSchema);
		Mockito.when(schemaStore.createSchema(Mockito.anyString(), Mockito.anyString())).thenReturn(latestSchema);
		Mockito.when(schemaInfoStore.createSchemaInfo(mockSchReqPubInt))
				.thenReturn(mockSchInfoPubInt);
		Mockito.when(schemaUtil.findSchemaToCompare(mockSchReqPubInt.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		assertEquals(mockSchInfoPubInt, schemaService.createSchema(mockSchReqPubInt));

		verify(messageBus, times(1)).publishMessage(anyString(), anyString());
	}

	@Test
	public void testCreateSchema_ApplicationException_Entity_PrivateSchema()
			throws JsonProcessingException, ApplicationException, BadRequestException, NotFoundException {
		expectedException.expect(ApplicationException.class);
		expectedException.expectMessage("Internal server error");
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		when(schemaInfoStore.isUniqueSystemSchema("os:wks:well:1.1.1")).thenReturn(true);
		when(schemaInfoStore.isUnique("os:wks:well:1.1.1", "tenant")).thenReturn(true);
		SchemaRequest schReqPub = getMockSchemaObject_published();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schReqPub.getSchemaInfo();
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getAuthority())).thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getSource())).thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getEntityType())).thenReturn(false);
		Mockito.when(schemaInfoStore.createSchemaInfo(schReqPub)).thenReturn(schReqPub.getSchemaInfo());
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("");
		Mockito.when(schemaUtil.findSchemaToCompare(schReqPub.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		schemaService.createSchema(schReqPub);
		verify(messageBus, times(0)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testCreateSchema_ApplicationException_Authority_PrivateSchema()
			throws NotFoundException, BadRequestException, ApplicationException, JsonProcessingException {
		expectedException.expect(ApplicationException.class);
		expectedException.expectMessage(SchemaConstants.INTERNAL_SERVER_ERROR);
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		when(schemaInfoStore.isUniqueSystemSchema("os:wks:well:1.1.1")).thenReturn(true);
		when(schemaInfoStore.isUnique("os:wks:well:1.1.1", "tenant")).thenReturn(true);
		SchemaRequest schReqPub = getMockSchemaObject_published();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schReqPub.getSchemaInfo();
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getAuthority())).thenReturn(false);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getSource())).thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getEntityType())).thenReturn(true);
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("");
		Mockito.when(schemaUtil.findSchemaToCompare(schReqPub.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		schemaService.createSchema(schReqPub);
		verify(messageBus, times(0)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testCreateSchema_ApplicationException_Source_PrivateSchema()
			throws JsonProcessingException, ApplicationException, BadRequestException, NotFoundException {
		expectedException.expect(ApplicationException.class);
		expectedException.expectMessage("Internal server error");
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		when(schemaInfoStore.isUniqueSystemSchema("os:wks:well:1.1.1")).thenReturn(true);
		when(schemaInfoStore.isUnique("os:wks:well:1.1.1", "tenant")).thenReturn(true);
		SchemaRequest schReqPub = getMockSchemaObject_published();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schReqPub.getSchemaInfo();
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getAuthority())).thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getSource())).thenReturn(false);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getEntityType())).thenReturn(true);
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("");

		Mockito.when(schemaUtil.findSchemaToCompare(schReqPub.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		schemaService.createSchema(schReqPub);
		verify(messageBus, times(0)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testCreateSchema_schemaExists()
			throws ApplicationException, NotFoundException, BadRequestException, JsonProcessingException {
		expectedException.expect(BadRequestException.class);
		expectedException.expectMessage(SchemaConstants.SCHEMA_ID_EXISTS);
		String dataPartitionId = "common";
		String schemaId = "os:wks:well:1.1.1";
		Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
		when(schemaInfoStore.isUniqueSystemSchema(schemaId)).thenReturn(false);
		when(schemaInfoStore.isUnique(schemaId, "tenant")).thenReturn(false);
		schemaService.createSchema(getMockSchemaObject_published());
		verify(messageBus, times(0)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testUpdateSchema_WithPublishedState() {
		try {
			Mockito.when(headers.getPartitionId()).thenReturn("tenant");
			SchemaRequest schReqPub = getMockSchemaObject_published();

			Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
							schReqPub.getSchemaInfo().getSchemaIdentity().getAuthority()))
					.thenReturn(true);
			Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
					schReqPub.getSchemaInfo().getSchemaIdentity().getSource())).thenReturn(true);
			Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
							schReqPub.getSchemaInfo().getSchemaIdentity().getEntityType()))
					.thenReturn(true);
			Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenReturn(getMockSchemaInfo());
			schemaService.updateSchema(schReqPub);
			fail("Should not succeed");

		} catch (BadRequestException e) {
			verify(messageBus, times(0)).publishMessage( anyString(), anyString());
			assertEquals(SchemaConstants.SCHEMA_UPDATE_EXCEPTION, e.getMessage());
		} catch (Exception e) {
			fail("Should not get different exception");
		}
	}

	@Test
	public void testUpdateSchema_NotFoundException_Published() throws NotFoundException, ApplicationException {
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		when(schemaInfoStore.isUniqueSystemSchema("os:wks:well:1.1.1")).thenReturn(true);
		when(schemaInfoStore.isUnique("os:wks:well:1.1.1", "tenant")).thenReturn(true);
		SchemaRequest schReqPub = getMockSchemaObject_published();
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getAuthority())).thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getSource())).thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getEntityType())).thenReturn(true);
		Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenThrow(NotFoundException.class);
		try {
			schemaService.updateSchema(schReqPub);
			fail("Should not succeed");

		} catch (BadRequestException e) {
			verify(messageBus, times(0)).publishMessage( anyString(), anyString());
			assertEquals(SchemaConstants.SCHEMA_PUT_CREATE_EXCEPTION, e.getMessage());
		} catch (Exception e) {
			fail("Should not get different exception");
		}
	}

	@Test
	public void testUpdateSchema_NotFoundException_Development() throws NotFoundException, ApplicationException {
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		when(schemaInfoStore.isUniqueSystemSchema("os:wks:well:1.1.1")).thenReturn(true);
		when(schemaInfoStore.isUnique("os:wks:well:1.1.1", "tenant")).thenReturn(true);
		SchemaRequest schReqPub = getMockSchemaObject_published();
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getAuthority())).thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getSource())).thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
				schReqPub.getSchemaInfo().getSchemaIdentity().getEntityType())).thenReturn(true);
		Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenThrow(NotFoundException.class);
		try {
			schemaService.updateSchema(getMockSchemaObject_Development());
			fail("Should not succeed");

		} catch (NoSchemaFoundException e) {
			verify(messageBus, times(0)).publishMessage( anyString(), anyString());
			assertEquals(SchemaConstants.INVALID_SCHEMA_UPDATE, e.getMessage());
		} catch (Exception e) {
			fail("Should not get different exception");
		}
	}

	@Test
	public void testUpdateSchema_withUnregisteredSchema() {
		try {
			Mockito.when(headers.getPartitionId()).thenReturn("tenant");
			when(schemaInfoStore.isUniqueSystemSchema("os:wks:well:1.1.1")).thenReturn(true);
			when(schemaInfoStore.isUnique("os:wks:well:1.1.1", "tenant")).thenReturn(true);
			SchemaRequest schReqPub = getMockSchemaObject_published();
			Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
							schReqPub.getSchemaInfo().getSchemaIdentity().getAuthority()))
					.thenReturn(true);
			Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
					schReqPub.getSchemaInfo().getSchemaIdentity().getSource())).thenReturn(true);
			Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
							schReqPub.getSchemaInfo().getSchemaIdentity().getEntityType()))
					.thenReturn(true);
			Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenThrow(NotFoundException.class);
			schemaService.updateSchema(schReqPub);
			fail("Should not succeed");

		} catch (BadRequestException e) {
			verify(messageBus, times(0)).publishMessage( anyString(), anyString());
			assertEquals(SchemaConstants.SCHEMA_PUT_CREATE_EXCEPTION, e.getMessage());
		} catch (Exception e) {
			fail("Should not get different exception");
		}
	}

	@Test
	public void testUpdateSchema_DevelopmentState() throws ApplicationException, NotFoundException, BadRequestException,
			JSONException, JsonProcessingException {
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		SchemaInfo input = getMockSchemaInfo_development_status();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=input;
		Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenReturn(input);
		Mockito.when(schemaInfoStore.updateSchemaInfo(getMockSchemaObject_Development())).thenReturn(input);

		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("");
		Mockito.when(schemaUtil.findSchemaToCompare(input, false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		assertNotNull(schemaService.updateSchema(getMockSchemaObject_Development()));
		verify(messageBus, times(1)).publishMessage( anyString(), anyString());
	}

	@Test
	public void testGetSchemaInfoList() throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		schemaInfos.add(getMockSchemaInfo());
		QueryParams queryParams = QueryParams.builder().authority("test").limit(10).build();
		Mockito.when(headers.getPartitionId()).thenReturn("test");
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "test")).thenReturn(schemaInfos);
		Assert.assertEquals(1, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_SharedScope()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		schemaInfos.add(getMockSchemaInfo());
		QueryParams queryParams = QueryParams.builder().scope("SHARED").authority("test").limit(10).build();
		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaInfos);
		Assert.assertEquals(1, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_InvalidScope()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		schemaInfos.add(getMockSchemaInfo());
		QueryParams queryParams = QueryParams.builder().scope("random").authority("test").limit(10).build();
		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaInfos);
		Assert.assertEquals(0, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_PrivateScope()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		schemaInfos.add(getMockSchemaInfo());
		QueryParams queryParams = QueryParams.builder().scope("INTERNAL").authority("test").limit(10).build();
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Assert.assertEquals(1, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_MajorMinorConflict_NoLatestVersion()
			throws ApplicationException, NotFoundException, BadRequestException {

		List<SchemaInfo> schemaInt = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaPub = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaComb = new LinkedList<SchemaInfo>();

		schemaInt.add(getMockSchemaInfo_Published_InternalScope());
		schemaPub.add(getMockSchemaInfo());
		schemaComb.addAll(schemaPub);
		schemaComb.addAll(schemaInt);

		QueryParams queryParams = QueryParams.builder().authority("test").latestVersion(true).limit(10).build();

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaPub);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInt);

		Assert.assertEquals(1, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_MatchingVersion_MajorGiven()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().authority("test").schemaVersionMajor(1L).limit(10).build();
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		schemaInfos.add(getMockSchemaInfo());
		schemaInfos.add(getMockSchemaInfo());
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Assert.assertEquals(2, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_MatchingVersion_MinorGiven()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().authority("test").schemaVersionMinor(1L).limit(10).build();
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		schemaInfos.add(getMockSchemaInfo());
		schemaInfos.add(getMockSchemaInfo());
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Assert.assertEquals(2, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_MatchingVersion_MajorMinorGiven()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().authority("test").schemaVersionMajor(1L).schemaVersionMinor(1L)
				.limit(10).build();
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		schemaInfos.add(getMockSchemaInfo());
		schemaInfos.add(getMockSchemaInfo());
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Assert.assertEquals(2, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_MatchingVersion_PatchGiven()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().authority("test").schemaVersionMinor(1L).limit(10).build();
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		schemaInfos.add(getMockSchemaInfo());
		schemaInfos.add(getMockSchemaInfo());
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Assert.assertEquals(2, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_MajorMinorConflict_MajorVersionWithoutMinorAndPatch()
			throws ApplicationException, NotFoundException, BadRequestException {
		QueryParams queryParams = QueryParams.builder().authority("os").latestVersion(true).schemaVersionMajor(1l)
				.limit(10).build();

		List<SchemaInfo> schemaInt = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaPub = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaComb = new LinkedList<SchemaInfo>();

		schemaInt.add(getMockSchemaInfo_Published_InternalScope());
		schemaPub.add(getMockSchemaInfo());
		schemaComb.addAll(schemaPub);
		schemaComb.addAll(schemaInt);

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");

		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaPub);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInt);

		Assert.assertEquals(1, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_MajorMinorConflict_MinorVersionWithoutMajor()
			throws ApplicationException, NotFoundException, BadRequestException {
		QueryParams queryParams = QueryParams.builder().latestVersion(true).schemaVersionMinor(1l).limit(10).build();
		expectedException.expect(BadRequestException.class);
		expectedException.expectMessage(SchemaConstants.LATESTVERSION_MINORFILTER_WITHOUT_MAJOR);
		schemaService.getSchemaInfoList(queryParams);
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_MajorMinorPatch_NoLatestVersion()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().authority("test").latestVersion(true).schemaVersionMajor(1L)
				.schemaVersionMinor(1L).limit(10).build();
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);
		Assert.assertEquals(0, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_WithoutScope()
			throws ApplicationException, NotFoundException, BadRequestException {

		List<SchemaInfo> schemaInt = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaPub = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaComb = new LinkedList<SchemaInfo>();

		schemaInt.add(getMockSchemaInfo_Published_InternalScope());
		schemaPub.add(getMockSchemaInfo());
		schemaComb.addAll(schemaPub);
		schemaComb.addAll(schemaInt);

		QueryParams queryParams = QueryParams.builder().latestVersion(true).limit(10).build();

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaPub);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInt);

		Assert.assertEquals(1, schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size());
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_WithoutScope_ForOneAuthority()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInt = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaPub = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaComb = new LinkedList<SchemaInfo>();

		schemaInt.add(getMockSchemaInfo_Published_InternalScope());
		schemaPub.add(getMockSchemaInfo());
		schemaComb.addAll(schemaPub);
		schemaComb.addAll(schemaInt);

		QueryParams queryParams = QueryParams.builder().authority("os").latestVersion(true).limit(10).build();

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaPub);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInt);

		Assert.assertEquals(1, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_TestOffsetParameter_LatestVersion()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInt = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaPub = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaComb = new LinkedList<SchemaInfo>();

		schemaInt.add(getMockSchemaInfo_Published_InternalScope());
		schemaPub.add(getMockSchemaInfo());
		schemaComb.addAll(schemaPub);
		schemaComb.addAll(schemaInt);

		QueryParams queryParams = QueryParams.builder().authority("os").latestVersion(true).limit(10).offset(1).build();

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "common")).thenReturn(schemaPub);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInt);

		Assert.assertEquals(0, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
	}

	@Test
	public void testGetSchemaInfoList_TestOffsetParameter()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInt = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaPub = new LinkedList<SchemaInfo>();
		List<SchemaInfo> schemaComb = new LinkedList<SchemaInfo>();

		schemaInt.add(getMockSchemaInfo_INTERNAL_EntityWellBore());
		schemaPub.add(getMockSchemaInfo());
		schemaComb.addAll(schemaPub);
		schemaComb.addAll(schemaInt);

		QueryParams queryParams = QueryParams.builder().authority("os").limit(10).offset(1).build();

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaPub);
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInt);

		List<SchemaInfo> result = schemaService.getSchemaInfoList(queryParams).getSchemaInfos();

		Assert.assertEquals(1, (result.size()));
		Assert.assertEquals("os:abc:well:1.1.1", (result.get(0).getSchemaIdentity().getId()));
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_WithScopeInternal_WithoutAuthority()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().latestVersion(true).scope("INTERNAL").limit(10).build();

		schemaInfos.add(getMockSchemaInfo_INTERNAL_EntityWellBore());
		schemaInfos.add(getMockSchemaInfo_Published_InternalScope());

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos.subList(0, 1));

		Assert.assertTrue(schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size() > 0);

		for(SchemaInfo outputSchemaInfo : schemaService.getSchemaInfoList(queryParams).getSchemaInfos()) {
			Assert.assertTrue("INTERNAL".equals(outputSchemaInfo.getScope().toString()));
		}

	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_WithScopeInternal_WithOneAuthority()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().authority("os").latestVersion(true).scope("INTERNAL").limit(10).build();
		schemaInfos.add(getMockSchemaInfo_Published_InternalScope());

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfoList(queryParams, "tenant")).thenReturn(schemaInfos);

		Assert.assertEquals(1, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
		Assert.assertTrue("INTERNAL".equals(schemaService.getSchemaInfoList(queryParams).getSchemaInfos().get(0).getScope().toString()));
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_WithScopeShared_WithoutAuthority()
			throws ApplicationException, NotFoundException, BadRequestException {
		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().latestVersion(true).scope("SHARED").limit(10).build();
		schemaInfos.add(getMockSchemaInfo_Published_SharedScope());
		schemaInfos.add(getMockSchemaInfo_SHARED_EntityWellBore());

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaInfos);

		List<SchemaInfo> finalList = schemaService.getSchemaInfoList(queryParams).getSchemaInfos();

		Assert.assertTrue(finalList.size() == 2);

		for(SchemaInfo outputSchemaInfo : finalList) {
			Assert.assertTrue("SHARED".equals(outputSchemaInfo.getScope().toString()));
		}

	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_WithScopeShared_WithOneAuthority()
			throws ApplicationException, NotFoundException, BadRequestException {

		List<SchemaInfo> schemaInfos = new LinkedList<SchemaInfo>();
		QueryParams queryParams = QueryParams.builder().authority("os").latestVersion(true).scope("SHARED").limit(10).build();
		schemaInfos.add(getMockSchemaInfo_Published_SharedScope());

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSystemSchemaInfoList(queryParams)).thenReturn(schemaInfos);

		Assert.assertEquals(1, (schemaService.getSchemaInfoList(queryParams).getSchemaInfos().size()));
		Assert.assertTrue("SHARED".equals(schemaService.getSchemaInfoList(queryParams).getSchemaInfos().get(0).getScope().toString()));
		Assert.assertTrue("os".equals(schemaService.getSchemaInfoList(queryParams).getSchemaInfos().get(0).getSchemaIdentity().getAuthority()));


	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_MajorMinorPatchConflict_PatchVersionWithoutMinor()
			throws ApplicationException, NotFoundException, BadRequestException {
		QueryParams queryParams = QueryParams.builder().authority("test").latestVersion(true).schemaVersionMajor(1L)
				.schemaVersionPatch(1L).limit(10).build();
		expectedException.expect(BadRequestException.class);
		expectedException.expectMessage(SchemaConstants.LATESTVERSION_PATCHFILTER_WITHOUT_MINOR);
		schemaService.getSchemaInfoList(queryParams);
	}

	@Test
	public void testGetSchemaInfoList_LatestVersion_MajorMinorConflict_PatchVersionWithoutMajor()
			throws ApplicationException, NotFoundException, BadRequestException {
		QueryParams queryParams = QueryParams.builder().latestVersion(true).schemaVersionMinor(1l)
				.schemaVersionPatch(1L).limit(10).build();
		expectedException.expect(BadRequestException.class);
		expectedException.expectMessage(SchemaConstants.LATESTVERSION_MINORFILTER_WITHOUT_MAJOR);
		schemaService.getSchemaInfoList(queryParams);
	}

	@Test
	public void testUpsertSchema_SuccessfullUpdate()
			throws ApplicationException, NotFoundException, BadRequestException, JsonMappingException, JsonProcessingException {

		SchemaInfo schInfo = getMockSchemaInfo_development_status();
		SchemaRequest schReq = getMockSchemaObject_Development();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schInfo;
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenReturn(schInfo);
		Mockito.when(schemaInfoStore.updateSchemaInfo(schReq)).thenReturn(schInfo);
		Mockito.when(schemaStore.createSchema(Mockito.anyString(), Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("");

		Mockito.when(schemaUtil.findSchemaToCompare(schInfo, false)).thenReturn(schemaInfoArr);
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));

		assertEquals(HttpStatus.OK, schemaService.upsertSchema(schReq).getHttpCode());
	}

	@Test
	public void testUpsertSchema_SuccessfullCreate()
			throws ApplicationException, NotFoundException, BadRequestException, JsonMappingException, JsonProcessingException {

		//throw exception while updating the schema
		SchemaRequest schReq = getMockSchemaObject_Development();
		SchemaInfo schInfoCr = getMockSchemaInfo_development_status();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schReq.getSchemaInfo();
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenThrow(new NotFoundException());



		//Create schema call
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		String schemaId = "os:wks:well:1.1.1";

		when(schemaInfoStore.isUniqueSystemSchema(schemaId)).thenReturn(true);
		when(schemaInfoStore.isUnique(schemaId, "tenant")).thenReturn(true);
		Mockito.when(authorityService.checkAndRegisterAuthorityIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getAuthority()))
				.thenReturn(true);
		Mockito.when(sourceService.checkAndRegisterSourceIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getSource()))
				.thenReturn(true);
		Mockito.when(entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
						schReq.getSchemaInfo().getSchemaIdentity().getEntityType()))
				.thenReturn(true);
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaStore.createSchema(Mockito.anyString(), Mockito.anyString())).thenReturn("{}");
		Mockito.when(schemaInfoStore.createSchemaInfo(schReq))
				.thenReturn(schInfoCr);
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("");

		Mockito.when(schemaUtil.findSchemaToCompare(schReq.getSchemaInfo(), false)).thenReturn(schemaInfoArr);

		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(Matchers.any(SchemaValidationType.class)))
				.thenReturn(mock(VersionValidator.class));
		assertEquals(HttpStatus.CREATED, schemaService.upsertSchema(schReq).getHttpCode());
	}

	@Test(expected = BadRequestException.class)
	public void testUpsertSchema_WhenSchemaExistInOtherTenant()
			throws ApplicationException, NotFoundException, BadRequestException {
		//schemaInfoStore.isUnique(schemaId, dataPartitionId)


		//throw exception while updating the schema
		SchemaRequest schReq = getMockSchemaObject_Development();

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenThrow(new NotFoundException());

		//Create schema call
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		String schemaId = "os:wks:well:1.1.1";

		when(schemaInfoStore.isUnique(schemaId, "tenant")).thenReturn(false);
		schemaService.upsertSchema(schReq).getHttpCode();
	}

	@Test
	public void testSchemaCreate_Patch_BreakingChanges()
			throws ApplicationException, NotFoundException, BadRequestException, JsonMappingException, JsonProcessingException {

		//Exception is expected when comparing patch version fails
		expectedException.expect(BadRequestException.class);

		SchemaRequest schReq = getMockSchemaObject_Development();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=schReq.getSchemaInfo();
		//Schema is unique
		when(schemaInfoStore.isUnique(anyString(), anyString())).thenReturn(true);
		//Find nearest Schema version to compare with
		when(schemaUtil.findSchemaToCompare(schReq.getSchemaInfo(), false)).thenReturn(schemaInfoArr);
		//Schema is successfully resolved
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn("{}");
		//Nothing is returned
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("{}");
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(SchemaValidationType.PATCH))
				.thenReturn(patchVersionValidator);
		Mockito.doThrow(new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_PATCH)).when(patchVersionValidator).validateVersionChange(anyString(), anyString());
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		schemaService.createSchema(schReq);
		//Assert that createSchema is not called even once
		verify(schemaStore, times(0)).createSchema(anyString(), anyString());
	}

	@Test
	public void testSchemaCreate_WhenMajorVersionIsGreater_BreakingChanges()
			throws ApplicationException, NotFoundException, BadRequestException, JsonMappingException, JsonProcessingException {

		//Exception is expected when comparing patch version fails
		expectedException.expect(BadRequestException.class);

		SchemaRequest schReq = getMockSchemaObject_Development();

		SchemaInfo latestFoundInSystem = SchemaInfo.builder()
				.schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
						.schemaVersionMajor(1L).schemaVersionMinor(2L).schemaVersionPatch(1L)
						.id("os:wks:well:1.2.1").build())
				.dateCreated(currDate).scope(SchemaScope.INTERNAL).status(SchemaStatus.DEVELOPMENT).build();

		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=latestFoundInSystem;

		//Schema is unique
		when(schemaInfoStore.isUnique(anyString(), anyString())).thenReturn(true);

		//Find nearest Schema version to compare with
		when(schemaUtil.findSchemaToCompare(schReq.getSchemaInfo(), false)).thenReturn(schemaInfoArr);

		//Schema is successfully resolved
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn("{}");

		//Nothing is returned
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("{}");

		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(SchemaValidationType.MINOR))
				.thenReturn(minorVersionValidator);
		Mockito.doThrow(new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR)).when(minorVersionValidator).validateVersionChange(anyString(), anyString());
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");

		schemaService.createSchema(schReq);
		//Assert that createSchema is not called even once
		verify(schemaStore, times(0)).createSchema(anyString(), anyString());
	}

	@Test
	public void testSchemaCreate_WhenMajorVersionIsLesser_BreakingChanges()
			throws ApplicationException, NotFoundException, BadRequestException, JsonMappingException, JsonProcessingException {

		//Exception is expected when comparing patch version fails
		expectedException.expect(BadRequestException.class);

		SchemaRequest schReq = getMockSchemaObject_Development();

		SchemaInfo latestFoundInSystem = SchemaInfo.builder()
				.schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
						.schemaVersionMajor(1L).schemaVersionMinor(0L).schemaVersionPatch(1L)
						.id("os:wks:well:1.0.1").build())
				.dateCreated(currDate).scope(SchemaScope.INTERNAL).status(SchemaStatus.DEVELOPMENT).build();

		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		schemaInfoArr[1]=latestFoundInSystem;

		//Schema is unique
		when(schemaInfoStore.isUnique(anyString(), anyString())).thenReturn(true);

		//Find nearest Schema version to compare with
		when(schemaUtil.findSchemaToCompare(schReq.getSchemaInfo(), false)).thenReturn(schemaInfoArr);

		//Schema is successfully resolved
		Mockito.when(schemaResolver.resolveSchema(Mockito.anyString())).thenReturn("{}");

		//Nothing is returned
		Mockito.when(schemaStore.getSchema(anyString(), anyString())).thenReturn("{}");

		//Do nothing when minor level changes are compared
		Mockito.when(schemaVersionValidatorFactory.getVersionValidator(SchemaValidationType.MINOR))
				.thenReturn(minorVersionValidator);
		Mockito.doThrow(new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR)).when(minorVersionValidator).validateVersionChange(anyString(), anyString());

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");

		schemaService.createSchema(schReq);

		//Assert that createSchema is not called even once
		verify(schemaStore, times(0)).createSchema(anyString(), anyString());
	}

	@Test
	public void testUpsertSchema_Badrequest()
			throws ApplicationException, NotFoundException, BadRequestException {
		//schemaInfoStore.isUnique(schemaId, dataPartitionId)


		//throw exception while updating the schema
		SchemaRequest schReq = getMockSchemaObject_Development();

		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		Mockito.when(schemaInfoStore.getSchemaInfo("os:wks:well:1.1.1")).thenThrow(new NotFoundException());

		//Create schema call
		Mockito.when(headers.getPartitionId()).thenReturn("tenant");
		String schemaId = "os:wks:well:1.1.1";

		when(schemaInfoStore.isUnique(schemaId, "tenant")).thenReturn(false);

		try {
			schemaService.upsertSchema(schReq).getHttpCode();
		}catch (BadRequestException badreqEx) {
			assertTrue(SchemaConstants.INVALID_UPDATE_OPERATION.equals(badreqEx.getMessage()));
		}

	}

	private SchemaRequest getMockSchemaObject_published() {
		return SchemaRequest.builder().schema("{}")
				.schemaInfo(SchemaInfo.builder()
						.schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
								.schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
								.id("os:wks:well:1.1.1").build())
						.dateCreated(currDate).scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).build())
				.build();

	}

	private SchemaRequest getMockSchemaObject_published_InternalScope() {
		return SchemaRequest.builder().schema("{}")
				.schemaInfo(SchemaInfo.builder()
						.schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
								.schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
								.id("os:wks:well:1.1.1").build())
						.dateCreated(currDate).scope(SchemaScope.INTERNAL).status(SchemaStatus.PUBLISHED).build())
				.build();

	}

	private SchemaRequest getMockSchemaObject_published_SharedScope() {
		return SchemaRequest.builder().schema("{}")
				.schemaInfo(SchemaInfo.builder()
						.schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
								.schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
								.id("os:wks:well:1.1.1").build())
						.dateCreated(currDate).scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).build())
				.build();

	}

	private SchemaRequest getMockSchemaObject_BreakingChanges() throws IOException {

		String schema = new FileUtils().read("/schema_compare/original-schema.json");
		return SchemaRequest.builder().schema(schema)
				.schemaInfo(SchemaInfo.builder()
						.schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
								.schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
								.id("os:wks:well:1.1.1").build())
						.dateCreated(currDate).scope(SchemaScope.INTERNAL).status(SchemaStatus.PUBLISHED).build())
				.build();

	}

	private String getMockLatestSchema_BreakingChanges() throws IOException {

		String schema = new FileUtils().read("/schema_compare/schema-with-reqprop-extraelement.json");
		return schema;
	}

	private SchemaRequest getMockSchemaObject_Development() {
		return SchemaRequest.builder().schema("{}")
				.schemaInfo(SchemaInfo.builder()
						.schemaIdentity(SchemaIdentity.builder().authority("os").source("wks").entityType("well")
								.schemaVersionMajor(1L).schemaVersionMinor(1L).schemaVersionPatch(1L)
								.id("os:wks:well:1.1.1").build())
						.dateCreated(currDate).scope(SchemaScope.INTERNAL).status(SchemaStatus.DEVELOPMENT).build())
				.build();

	}

	private SchemaInfo getMockSchemaInfo() {
		return SchemaInfo.builder()
				.schemaIdentity(
						SchemaIdentity.builder().authority("os").source("wks").entityType("well").schemaVersionMajor(1L)
								.schemaVersionMinor(1L).schemaVersionPatch(1L).id("os:wks:well:1.1.1").build())
				.dateCreated(currDate).scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).build();

	}

	private SchemaInfo getMockSchemaInfo_SHARED_EntityWellBore() {
		return SchemaInfo.builder()
				.schemaIdentity(
						SchemaIdentity.builder().authority("os").source("abc").entityType("wellbore").schemaVersionMajor(1L)
								.schemaVersionMinor(1L).schemaVersionPatch(1L).id("os:wks:well:1.1.1").build())
				.dateCreated(currDate).scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).build();

	}

	private SchemaInfo getMockSchemaInfo_INTERNAL_EntityWellBore() {
		return SchemaInfo.builder()
				.schemaIdentity(
						SchemaIdentity.builder().authority("os").source("abc").entityType("wellbore").schemaVersionMajor(1L)
								.schemaVersionMinor(1L).schemaVersionPatch(1L).id("os:abc:well:1.1.1").build())
				.dateCreated(currDate).scope(SchemaScope.INTERNAL).status(SchemaStatus.PUBLISHED).build();

	}

	private SchemaInfo getMockSchemaInfo_Published_InternalScope() {
		return SchemaInfo.builder()
				.schemaIdentity(
						SchemaIdentity.builder().authority("os").source("wks").entityType("well").schemaVersionMajor(1L)
								.schemaVersionMinor(1L).schemaVersionPatch(1L).id("os:wks:well:1.1.1").build())
				.dateCreated(currDate).scope(SchemaScope.INTERNAL).status(SchemaStatus.PUBLISHED).build();
	}

	private SchemaInfo getMockSchemaInfo_Published_SharedScope() {
		return SchemaInfo.builder()
				.schemaIdentity(
						SchemaIdentity.builder().authority("os").source("wks").entityType("well").schemaVersionMajor(1L)
								.schemaVersionMinor(1L).schemaVersionPatch(1L).id("os:wks:well:1.1.1").build())
				.dateCreated(currDate).scope(SchemaScope.SHARED).status(SchemaStatus.PUBLISHED).build();
	}

	private SchemaInfo getMockSchemaInfo_development_status() {
		return SchemaInfo.builder()
				.schemaIdentity(
						SchemaIdentity.builder().authority("os").source("wks").entityType("well").schemaVersionMajor(1L)
								.schemaVersionMinor(1L).schemaVersionPatch(1L).id("os:wks:well:1.1.1").build())
				.dateCreated(currDate).scope(SchemaScope.INTERNAL).status(SchemaStatus.DEVELOPMENT).build();

	}
}