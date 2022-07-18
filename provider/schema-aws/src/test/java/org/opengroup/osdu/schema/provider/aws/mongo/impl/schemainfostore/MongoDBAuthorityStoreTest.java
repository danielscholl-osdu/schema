package org.opengroup.osdu.schema.provider.aws.mongo.impl.schemainfostore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.AuthorityDto;
import org.opengroup.osdu.schema.provider.aws.mongo.config.SchemaTestConfig;
import org.opengroup.osdu.schema.provider.aws.mongo.util.ParentUtil;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBAuthorityStore.AUTHORITY_PREFIX;


@DataMongoTest
@RunWith(SpringRunner.class)
@SpringJUnitConfig(classes = {SchemaTestConfig.class})
public class MongoDBAuthorityStoreTest extends ParentUtil {

    @Autowired
    private IAuthorityStore awsAuthorityStore;

    @Test
    public void get() throws ApplicationException, NotFoundException {
        //given
        String id = "authorityId";
        AuthorityDto authority = new AuthorityDto(id);
        mongoTemplateHelper.insert(authority, AUTHORITY_PREFIX + DATA_PARTITION);

        //when
        Authority authorityFromStore = awsAuthorityStore.get(authority.getId());

        //then
        assertNotNull(authorityFromStore);
        assertEquals(authority.getId(), authorityFromStore.getAuthorityId());
    }

    @Test(expected = NotFoundException.class)
    public void getNotFound() throws ApplicationException, NotFoundException {
        //given
        String id = "authorityId";
        AuthorityDto byId = (AuthorityDto) mongoTemplateHelper.findById(id, AuthorityDto.class, AUTHORITY_PREFIX + DATA_PARTITION);
        assertNull(byId);

        //then
        awsAuthorityStore.get(id);
    }

    @Test
    public void getSystemAuthority() throws ApplicationException, NotFoundException {
        //given
        String id = "authorityId";
        AuthorityDto authority = new AuthorityDto(id);
        mongoTemplateHelper.insert(authority, AUTHORITY_PREFIX + "common");

        ReflectionTestUtils.setField(awsAuthorityStore, "sharedTenant", "common");
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        Authority authorityFromStore = awsAuthorityStore.getSystemAuthority(authority.getId());

        //then
        assertNotNull(authorityFromStore);
        assertEquals(authority.getId(), authorityFromStore.getAuthorityId());
    }

    @Test
    public void create() throws ApplicationException, BadRequestException {
        //given
        Authority authority = new Authority();
        String authorityId = "authorityId";
        authority.setAuthorityId(authorityId);
        //when

        Authority authorityFromStore = awsAuthorityStore.create(authority);

        //then
        assertNotNull(authorityFromStore);
        assertEquals(authority.getAuthorityId(), authorityFromStore.getAuthorityId());
        AuthorityDto fromDb = (AuthorityDto) mongoTemplateHelper.findById(authorityId, AuthorityDto.class, AUTHORITY_PREFIX + DATA_PARTITION);
        assertNotNull(fromDb);
        assertEquals(authorityId, fromDb.getId());
    }

    @Test(expected = BadRequestException.class)
    public void createDuplicate() throws ApplicationException, BadRequestException {
        //given
        String authorityId = "authorityId";
        mongoTemplateHelper.insert(new AuthorityDto(authorityId), AUTHORITY_PREFIX + DATA_PARTITION);
        Authority authority = new Authority();
        authority.setAuthorityId(authorityId);

        //then
        awsAuthorityStore.create(authority);
    }

    @Test
    public void createSystemAuthority() throws ApplicationException, BadRequestException {
        //given
        Authority authority = new Authority();
        String authorityId = "authorityId";
        authority.setAuthorityId(authorityId);
        String common = "common";

        ReflectionTestUtils.setField(awsAuthorityStore, "sharedTenant", common);
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        Authority authorityFromStore = awsAuthorityStore.createSystemAuthority(authority);

        //then
        assertNotNull(authorityFromStore);
        assertEquals(authority.getAuthorityId(), authorityFromStore.getAuthorityId());
        AuthorityDto fromDb = (AuthorityDto) mongoTemplateHelper.findById(authorityId, AuthorityDto.class, AUTHORITY_PREFIX + common);
        assertNotNull(fromDb);
        assertEquals(authorityId, fromDb.getId());
    }
}