// Copyright © Amazon Web Services
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

package org.opengroup.osdu.schema.provider.aws.mongo.impl.schemainfostore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.aws.SchemaAwsApplication;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBAuthorityStore;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.AuthorityDto;
import org.opengroup.osdu.schema.provider.aws.mongo.config.SchemaTestConfig;
import org.opengroup.osdu.schema.provider.aws.mongo.util.ParentUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBAuthorityStore.AUTHORITY_PREFIX;


@DataMongoTest
@RunWith(SpringRunner.class)
@SpringJUnitConfig(classes = {SchemaTestConfig.class})
@ContextConfiguration(classes = {SchemaAwsApplication.class, MockServletContext.class})
public class MongoDBAuthorityStoreTest extends ParentUtil {

    @Autowired
    private MongoDBAuthorityStore authorityStore;

    @Test
    public void get() throws ApplicationException, NotFoundException {
        //given
        String id = "authorityId";
        AuthorityDto authority = new AuthorityDto(id);
        mongoTemplateHelper.insert(authority, AUTHORITY_PREFIX + DATA_PARTITION);

        //when
        Authority authorityFromStore = authorityStore.get(authority.getId());

        //then
        assertNotNull(authorityFromStore);
        assertEquals(authority.getId(), authorityFromStore.getAuthorityId());
    }

    @Test()
    public void getNotFound()  {
        //given
        String id = "authorityId";
        AuthorityDto byId;
        byId = (AuthorityDto) mongoTemplateHelper.findById(id, AuthorityDto.class, AUTHORITY_PREFIX + DATA_PARTITION);
        assertNull(byId);

        assertThrows(NotFoundException.class, ()->authorityStore.get(id));
        //then

    }

    @Test
    public void getSystemAuthority() throws ApplicationException, NotFoundException {
        //given
        String id = "authorityId";
        AuthorityDto authority = new AuthorityDto(id);
        mongoTemplateHelper.insert(authority, AUTHORITY_PREFIX + "common");

        ReflectionTestUtils.setField(authorityStore, "sharedTenant", "common");
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        Authority authorityFromStore = authorityStore.getSystemAuthority(authority.getId());

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

        Authority authorityFromStore = authorityStore.create(authority);

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
        authorityStore.create(authority);
    }

    @Test
    public void createSystemAuthority() throws ApplicationException, BadRequestException {
        //given
        Authority authority = new Authority();
        String authorityId = "authorityId";
        authority.setAuthorityId(authorityId);
        String common = "common";

        ReflectionTestUtils.setField(authorityStore, "sharedTenant", common);
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        Authority authorityFromStore = authorityStore.createSystemAuthority(authority);

        //then
        assertNotNull(authorityFromStore);
        assertEquals(authority.getAuthorityId(), authorityFromStore.getAuthorityId());
        AuthorityDto fromDb = (AuthorityDto) mongoTemplateHelper.findById(authorityId, AuthorityDto.class, AUTHORITY_PREFIX + common);
        assertNotNull(fromDb);
        assertEquals(authorityId, fromDb.getId());
    }
}