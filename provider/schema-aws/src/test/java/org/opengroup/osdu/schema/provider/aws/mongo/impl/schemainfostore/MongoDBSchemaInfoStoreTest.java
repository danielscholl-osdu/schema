package org.opengroup.osdu.schema.provider.aws.mongo.impl.schemainfostore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBSchemaInfoStore.SCHEMA_INFO_PREFIX;
import static org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBSchemaInfoStore.createSchemaId;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.aws.SchemaAwsApplication;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBSchemaInfoStore;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.SchemaInfoDto;
import org.opengroup.osdu.schema.provider.aws.mongo.config.SchemaTestConfig;
import org.opengroup.osdu.schema.provider.aws.mongo.util.ParentUtil;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@DataMongoTest
@RunWith(SpringRunner.class)
@SpringJUnitConfig(classes = {SchemaTestConfig.class})
@ContextConfiguration(classes = {SchemaAwsApplication.class, MockServletContext.class})
public class MongoDBSchemaInfoStoreTest extends ParentUtil {

    @Autowired
    private MongoDBSchemaInfoStore schemaInfoStore;
    @Autowired
    private ISchemaStore awsSchemaStore;

    @Test
    public void updateSchemaInfo() throws ApplicationException, BadRequestException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        SchemaInfo requestSchemaInfo = schemaRequest.getSchemaInfo();
        String id = requestSchemaInfo.getSchemaIdentity().getId();
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(requestSchemaInfo);
        SchemaInfoDto inDb = (SchemaInfoDto) mongoTemplateHelper.insert(schemaInfoDto, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        assertNotNull(inDb);

        requestSchemaInfo.setScope(SchemaScope.SHARED);
        //when
        schemaInfoStore.updateSchemaInfo(schemaRequest);

        //then
        SchemaInfoDto fromDb = (SchemaInfoDto) mongoTemplateHelper.findById(id, SchemaInfoDto.class, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        assertNotNull(fromDb);
        assertEquals(SchemaScope.SHARED, fromDb.getData().getScope());
    }

    @Test(expected = BadRequestException.class)
    public void updateSchemaInfoNodFound() throws ApplicationException, BadRequestException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();

        //then
        schemaInfoStore.updateSchemaInfo(schemaRequest);
    }

    @Test
    public void updateSystemSchemaInfo() throws ApplicationException, BadRequestException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        SchemaInfo requestSchemaInfo = schemaRequest.getSchemaInfo();
        String id = requestSchemaInfo.getSchemaIdentity().getId();
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(requestSchemaInfo);
        SchemaInfoDto inDb = (SchemaInfoDto) mongoTemplateHelper.insert(schemaInfoDto, SCHEMA_INFO_PREFIX + "common");
        assertNotNull(inDb);

        requestSchemaInfo.setScope(SchemaScope.SHARED);
        ReflectionTestUtils.setField(schemaInfoStore, "sharedTenant", "common");
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        schemaInfoStore.updateSystemSchemaInfo(schemaRequest);

        //then
        SchemaInfoDto fromDb = (SchemaInfoDto) mongoTemplateHelper.findById(id, SchemaInfoDto.class, SCHEMA_INFO_PREFIX + "common");
        assertNotNull(fromDb);
        assertEquals(SchemaScope.SHARED, fromDb.getData().getScope());
    }

    @Test
    public void createSchemaInfo() throws ApplicationException, BadRequestException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();

        //when
        SchemaInfo schemaInfo = schemaInfoStore.createSchemaInfo(schemaRequest);

        //then
        assertNotNull(schemaInfo);
        String schemaId = schemaRequest.getSchemaInfo().getSchemaIdentity().getId();
        SchemaInfoDto schemaInfoDtoFromDb = (SchemaInfoDto) mongoTemplateHelper.findById(schemaId, SchemaInfoDto.class, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        assertNotNull(schemaInfoDtoFromDb);
        assertEquals(schemaInfo.getCreatedBy(), schemaInfoDtoFromDb.getData().getCreatedBy());
    }

    @Test(expected = BadRequestException.class)
    public void createSchemaInfoDuplicate() throws ApplicationException, BadRequestException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        SchemaInfo requestSchemaInfo = schemaRequest.getSchemaInfo();
        schemaInfoDto.setId(requestSchemaInfo.getSchemaIdentity().getId());
        schemaInfoDto.setData(requestSchemaInfo);
        SchemaInfoDto inDb = (SchemaInfoDto) mongoTemplateHelper.insert(schemaInfoDto, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        assertNotNull(inDb);

        //then
        schemaInfoStore.createSchemaInfo(schemaRequest);
    }

    @Test
    public void createSystemSchemaInfo() throws ApplicationException, BadRequestException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();

        ReflectionTestUtils.setField(schemaInfoStore, "sharedTenant", "common");
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        SchemaInfo schemaInfo = schemaInfoStore.createSystemSchemaInfo(schemaRequest);

        //then
        assertNotNull(schemaInfo);
        String schemaId = schemaRequest.getSchemaInfo().getSchemaIdentity().getId();
        SchemaInfoDto schemaInfoDtoFromDb = (SchemaInfoDto) mongoTemplateHelper.findById(schemaId, SchemaInfoDto.class, SCHEMA_INFO_PREFIX + "common");
        assertNotNull(schemaInfoDtoFromDb);
        assertEquals(schemaInfo.getCreatedBy(), schemaInfoDtoFromDb.getData().getCreatedBy());

    }

    @Test
    public void getSchemaInfo() throws ApplicationException, NotFoundException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        SchemaInfo requestSchemaInfo = schemaRequest.getSchemaInfo();
        String id = requestSchemaInfo.getSchemaIdentity().getId();
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(requestSchemaInfo);
        SchemaInfoDto inDb = (SchemaInfoDto) mongoTemplateHelper.insert(schemaInfoDto, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        assertNotNull(inDb);

        //when
        SchemaInfo schemaInfo = schemaInfoStore.getSchemaInfo(id);

        //then
        assertNotNull(schemaInfo);
        assertEquals(requestSchemaInfo.getDateCreated(), schemaInfo.getDateCreated());
    }

    @Test(expected = NotFoundException.class)
    public void getSchemaInfoNotFound() throws ApplicationException, NotFoundException {
        //given
        String id = "schemaInfoId";

        //when
        schemaInfoStore.getSchemaInfo(id);
    }

    @Test
    public void getSystemSchemaInfo() throws ApplicationException, NotFoundException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        SchemaInfo requestSchemaInfo = schemaRequest.getSchemaInfo();
        String id = requestSchemaInfo.getSchemaIdentity().getId();
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(requestSchemaInfo);
        SchemaInfoDto inDb = (SchemaInfoDto) mongoTemplateHelper.insert(schemaInfoDto, SCHEMA_INFO_PREFIX + "common");
        assertNotNull(inDb);
        ReflectionTestUtils.setField(schemaInfoStore, "sharedTenant", "common");
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        SchemaInfo schemaInfo = schemaInfoStore.getSystemSchemaInfo(id);

        //then
        assertNotNull(schemaInfo);
        assertEquals(requestSchemaInfo.getDateCreated(), schemaInfo.getDateCreated());
    }

    @Test
    public void getLatestMinorVerSchema() throws ApplicationException, NotFoundException {
        //given

        List<SchemaInfoDto> schemas = getSchemas("minor", 10);
        mongoTemplateHelper.insert(schemas, SCHEMA_INFO_PREFIX + DATA_PARTITION);

        String contentPrefix = "content:";

        Mockito.when(awsSchemaStore.getSchema(anyString(), anyString())).thenReturn("content:minor:minor:minor:5555.1000.10000");

        mongoTemplateHelper.insert(getSchemas("other", 10), SCHEMA_INFO_PREFIX + DATA_PARTITION);

        //when
        String latestMinorVerSchema = schemaInfoStore.getLatestMinorVerSchema(schemas.stream().findAny().get().getData());

        //then
        assertEquals(contentPrefix + schemas.get(9).getId(), latestMinorVerSchema);
    }

    @Test
    public void getSchemaInfoList() throws ApplicationException {

        List<SchemaInfoDto> a = getSchemas("A", 7);
        mongoTemplateHelper.insert(a, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> b = getSchemas("B", 5);
        mongoTemplateHelper.insert(b, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> c = getSchemas("C", 14);
        mongoTemplateHelper.insert(c, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> d = getSchemas("D", 9);
        mongoTemplateHelper.insert(d, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> e = getSchemas("E", 6);
        mongoTemplateHelper.insert(e, SCHEMA_INFO_PREFIX + DATA_PARTITION);


        String scope = SchemaScope.INTERNAL.toString();
        String status = SchemaStatus.DEVELOPMENT.toString();
        //match all items
        QueryParams queryParamsA = QueryParams.builder().authority("A").source("A").entityType("A").scope(scope).status(status).build();
        //match all items
        QueryParams queryParamsB = QueryParams.builder().authority("B").source("B").entityType("B").scope(scope).status(status).build();
        // wrong schemaVersionMajor(15l)
        QueryParams queryParamsC = QueryParams.builder().authority("C").source("C").entityType("C").scope(scope).status(status).schemaVersionMajor(15l).build();
        // match 1 item schemaVersionMinor(200l)
        QueryParams queryParamsD = QueryParams.builder().authority("D").source("D").entityType("D").scope(scope).status(status).schemaVersionMinor(200l).build();
        // match 1 item schemaVersionPatch(3000L)
        QueryParams queryParamsE = QueryParams.builder().authority("E").source("E").entityType("E").scope(scope).status(status).schemaVersionPatch(3000L).build();

        //when
        List<SchemaInfo> schemaInfoListA = schemaInfoStore.getSchemaInfoList(queryParamsA, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListB = schemaInfoStore.getSchemaInfoList(queryParamsB, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListC = schemaInfoStore.getSchemaInfoList(queryParamsC, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListD = schemaInfoStore.getSchemaInfoList(queryParamsD, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListE = schemaInfoStore.getSchemaInfoList(queryParamsE, DATA_PARTITION);

        //then
        assertEquals(a.size(), schemaInfoListA.size());
        assertEquals(b.size(), schemaInfoListB.size());
        assertEquals(0, schemaInfoListC.size());
        assertEquals(1, schemaInfoListD.size());
        assertEquals(1, schemaInfoListE.size());
    }

    @Test
    public void getSchemaInfoLatest() throws ApplicationException {

        List<SchemaInfoDto> a = getSchemas("A", 7);
        mongoTemplateHelper.insert(a, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> b = getSchemas("B", 5);
        mongoTemplateHelper.insert(b, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> c = getSchemas("C", 14);
        mongoTemplateHelper.insert(c, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> d = getSchemas("D", 9);
        mongoTemplateHelper.insert(d, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> e = getSchemas("E", 1);
        mongoTemplateHelper.insert(e, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        List<SchemaInfoDto> f = getSchemas("F", 13);
        mongoTemplateHelper.insert(f, SCHEMA_INFO_PREFIX + DATA_PARTITION);

        String scope = SchemaScope.INTERNAL.toString();
        String status = SchemaStatus.DEVELOPMENT.toString();
        //match all items but one last version
        QueryParams queryParamsA = QueryParams.builder().authority("A").source("A").entityType("A").scope(scope).status(status).latestVersion(true).build();
        //match all items
        QueryParams queryParamsB = QueryParams.builder().authority("B").source("B").entityType("B").scope(scope).status(status).latestVersion(false).build();
        //wrong source
        QueryParams queryParamsC = QueryParams.builder().authority("C").source("B").entityType("C").scope(scope).status(status).build();
        //wrong entityType
        QueryParams queryParamsD = QueryParams.builder().authority("D").source("D").entityType("A").scope(scope).status(status).build();
        //wrong scope
        QueryParams queryParamsE = QueryParams.builder().authority("E").source("E").entityType("E").scope("any").status(status).build();
        //wrong status
        QueryParams queryParamsF = QueryParams.builder().authority("F").source("F").entityType("F").scope(scope).status("any").build();

        //when
        List<SchemaInfo> schemaInfoListA = schemaInfoStore.getSchemaInfoList(queryParamsA, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListB = schemaInfoStore.getSchemaInfoList(queryParamsB, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListC = schemaInfoStore.getSchemaInfoList(queryParamsC, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListD = schemaInfoStore.getSchemaInfoList(queryParamsD, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListE = schemaInfoStore.getSchemaInfoList(queryParamsE, DATA_PARTITION);
        List<SchemaInfo> schemaInfoListF = schemaInfoStore.getSchemaInfoList(queryParamsF, DATA_PARTITION);

        //then
        assertEquals(1, schemaInfoListA.size());
        assertEquals(b.size(), schemaInfoListB.size());
        assertEquals(0, schemaInfoListC.size());
        assertEquals(0, schemaInfoListD.size());
        assertEquals(0, schemaInfoListE.size());
        assertEquals(0, schemaInfoListF.size());
    }

    @Test
    public void getSystemSchemaInfoList() throws ApplicationException {
        String tenant = "common";
        ReflectionTestUtils.setField(schemaInfoStore, "sharedTenant", tenant);

        List<SchemaInfoDto> a = getSchemas("A", 7);
        mongoTemplateHelper.insert(a, SCHEMA_INFO_PREFIX + tenant);
        List<SchemaInfoDto> c = getSchemas("C", 14);
        mongoTemplateHelper.insert(c, SCHEMA_INFO_PREFIX + tenant);
        List<SchemaInfoDto> d = getSchemas("D", 9);
        mongoTemplateHelper.insert(d, SCHEMA_INFO_PREFIX + tenant);
        List<SchemaInfoDto> e = getSchemas("E", 6);
        mongoTemplateHelper.insert(e, SCHEMA_INFO_PREFIX + tenant);


        String scope = SchemaScope.INTERNAL.toString();
        String status = SchemaStatus.DEVELOPMENT.toString();
        QueryParams queryParamsA = QueryParams.builder().authority("A").source("A").entityType("A").scope(scope).status(status).build();
        QueryParams queryParamsC = QueryParams.builder().authority("C").source("C").entityType("C").scope(scope).status(status).schemaVersionMajor(15l).build();
        QueryParams queryParamsD = QueryParams.builder().authority("D").source("D").entityType("D").scope(scope).status(status).schemaVersionMinor(200l).build();
        QueryParams queryParamsE = QueryParams.builder().authority("E").source("E").entityType("E").scope(scope).status(status).schemaVersionPatch(3000L).build();

        //when
        List<SchemaInfo> schemaInfoListA = schemaInfoStore.getSystemSchemaInfoList(queryParamsA);
        List<SchemaInfo> schemaInfoListC = schemaInfoStore.getSystemSchemaInfoList(queryParamsC);
        List<SchemaInfo> schemaInfoListD = schemaInfoStore.getSystemSchemaInfoList(queryParamsD);
        List<SchemaInfo> schemaInfoListE = schemaInfoStore.getSystemSchemaInfoList(queryParamsE);

        //then
        assertEquals(a.size(), schemaInfoListA.size());
        assertEquals(0, schemaInfoListC.size());
        assertEquals(1, schemaInfoListD.size());
        assertEquals(1, schemaInfoListE.size());
    }

    @Test
    public void isUnique() throws ApplicationException {
        //given
        SchemaInfoDto schemaInfDto = createSchemaInfDto("test", 1);

        //when
        boolean before = schemaInfoStore.isUnique(schemaInfDto.getId(), DATA_PARTITION);

        mongoTemplateHelper.insert(schemaInfDto, SCHEMA_INFO_PREFIX + DATA_PARTITION);

        boolean after = schemaInfoStore.isUnique(schemaInfDto.getId(), DATA_PARTITION);

        //then
        assertTrue(before);
        assertFalse(after);
    }

    @Test
    public void isUniqueSystemSchema() throws ApplicationException {
        //given
        String tenant = "common";
        ReflectionTestUtils.setField(schemaInfoStore, "sharedTenant", tenant);
        SchemaInfoDto schemaInfDto = createSchemaInfDto("test", 1);

        //when
        boolean before = schemaInfoStore.isUniqueSystemSchema(schemaInfDto.getId());

        mongoTemplateHelper.insert(schemaInfDto, SCHEMA_INFO_PREFIX + tenant);

        boolean after = schemaInfoStore.isUniqueSystemSchema(schemaInfDto.getId());

        //then
        assertTrue(before);
        assertFalse(after);
    }

    @Test
    public void cleanSchema() throws ApplicationException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        SchemaInfo requestSchemaInfo = schemaRequest.getSchemaInfo();
        String id = requestSchemaInfo.getSchemaIdentity().getId();
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(requestSchemaInfo);
        SchemaInfoDto inDb = (SchemaInfoDto) mongoTemplateHelper.insert(schemaInfoDto, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        assertNotNull(inDb);

        //when
        boolean isCleaned = schemaInfoStore.cleanSchema(id);

        //then
        assertTrue(isCleaned);
        SchemaInfoDto schemaDtoFromDb = (SchemaInfoDto) mongoTemplateHelper.findById(id, SchemaInfoDto.class, SCHEMA_INFO_PREFIX + DATA_PARTITION);
        assertNull(schemaDtoFromDb);
    }

    @Test
    public void cleanSystemSchema() throws ApplicationException {
        //given
        SchemaRequest schemaRequest = createSchemaRequest();
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        SchemaInfo requestSchemaInfo = schemaRequest.getSchemaInfo();
        String id = requestSchemaInfo.getSchemaIdentity().getId();
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(requestSchemaInfo);
        SchemaInfoDto inDb = (SchemaInfoDto) mongoTemplateHelper.insert(schemaInfoDto, SCHEMA_INFO_PREFIX + "common");
        assertNotNull(inDb);
        ReflectionTestUtils.setField(schemaInfoStore, "sharedTenant", "common");
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        boolean isCleaned = schemaInfoStore.cleanSystemSchema(id);

        //then
        assertTrue(isCleaned);
        SchemaInfoDto schemaDtoFromDb = (SchemaInfoDto) mongoTemplateHelper.findById(id, SchemaInfoDto.class, SCHEMA_INFO_PREFIX + "common");
        assertNull(schemaDtoFromDb);
    }

    private SchemaRequest createSchemaRequest() {
        SchemaInfo schemaInfo = new SchemaInfo();
        schemaInfo.setDateCreated(new Date());
        schemaInfo.setScope(SchemaScope.INTERNAL);
        SchemaIdentity schemaIdentity = new SchemaIdentity();
        schemaIdentity.setId(createSchemaId(schemaIdentity));
        schemaInfo.setSchemaIdentity(schemaIdentity);
        SchemaRequest schemaRequest = SchemaRequest.builder()
                .schemaInfo(schemaInfo)
                .build();
        return schemaRequest;
    }


    private List<SchemaInfoDto> getSchemas(String value, int elementsCount) {
        List<SchemaInfoDto> schemaInfoDtos = new ArrayList<>();
        for (int i = 0; i < elementsCount; ) {
            schemaInfoDtos.add(createSchemaInfDto(value, ++i));
        }
        return schemaInfoDtos;
    }

    private SchemaInfoDto createSchemaInfDto(String value, int i) {
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        SchemaInfo schemaInfo = new SchemaInfo();
        schemaInfo.setStatus(SchemaStatus.DEVELOPMENT);
        schemaInfo.setScope(SchemaScope.INTERNAL);
        SchemaIdentity schemaIdentity = new SchemaIdentity();
        schemaIdentity.setAuthority(value);
        schemaIdentity.setEntityType(value);
        schemaIdentity.setSource(value);
        schemaIdentity.setEntityType(value);
        schemaIdentity.setSchemaVersionMajor(5555l);
        schemaIdentity.setSchemaVersionMinor(i * 100l);
        schemaIdentity.setSchemaVersionPatch(i * 1000l);
        String id = createSchemaId(schemaIdentity);
        schemaIdentity.setId(id);
        schemaInfo.setSchemaIdentity(schemaIdentity);
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(schemaInfo);
        return schemaInfoDto;
    }
}