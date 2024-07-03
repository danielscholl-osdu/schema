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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBSourceStore;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.SourceDto;
import org.opengroup.osdu.schema.provider.aws.mongo.config.SchemaTestConfig;
import org.opengroup.osdu.schema.provider.aws.mongo.util.ParentUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBSourceStore.SOURCE_PREFIX;


@DataMongoTest
@SpringJUnitConfig(classes = {SchemaTestConfig.class})
public class MongoDBSourceStoreTest extends ParentUtil {

    @Autowired
    private MongoDBSourceStore sourceStore;

    @Test
    public void get() throws ApplicationException, NotFoundException {
        //given
        String id = "sourceId";
        SourceDto sourceDto = new SourceDto(id);
        mongoTemplateHelper.insert(sourceDto, SOURCE_PREFIX + DATA_PARTITION);

        //when
        Source sourceFromStore = sourceStore.get(sourceDto.getId());

        //then
        assertNotNull(sourceFromStore);
        assertEquals(sourceDto.getId(), sourceFromStore.getSourceId());
    }

    @Test()
    public void getNotFound() throws ApplicationException, NotFoundException {
        //given
        String id = "sourceId";
        SourceDto byId;

        try {
            byId = (SourceDto) mongoTemplateHelper.findById(id, SourceDto.class, SOURCE_PREFIX + DATA_PARTITION);
            assertNull(byId);
            sourceStore.get(id);
        }
        catch (NotFoundException e) {

        }
        //then

    }

    @Test
    public void getSystemSource() throws ApplicationException, NotFoundException {
        //given
        String id = "sourceId";
        SourceDto sourceDto = new SourceDto(id);
        mongoTemplateHelper.insert(sourceDto, SOURCE_PREFIX + "common");

        ReflectionTestUtils.setField(sourceStore, "sharedTenant", "common");
        Mockito.when(headers.getPartitionId()).thenReturn("common");
        //when
        Source sourceFromStore = sourceStore.getSystemSource(sourceDto.getId());

        //then
        assertNotNull(sourceFromStore);
        assertEquals(sourceDto.getId(), sourceFromStore.getSourceId());
    }

    @Test
    public void create() throws ApplicationException, BadRequestException {
        //given
        Source source = new Source();
        String id = "sourceId";
        source.setSourceId(id);

        //when
        Source sourceFromStore = sourceStore.create(source);

        //then
        assertNotNull(sourceFromStore);
        assertEquals(source.getSourceId(), sourceFromStore.getSourceId());
        SourceDto fromDb = (SourceDto) mongoTemplateHelper.findById(id, SourceDto.class, SOURCE_PREFIX + DATA_PARTITION);
        assertNotNull(fromDb);
        assertEquals(id, fromDb.getId());
    }

    @Test
    public void createDuplicate() throws ApplicationException, BadRequestException {
        //given
        String id = "sourceId";
        mongoTemplateHelper.insert(new SourceDto(id), SOURCE_PREFIX + DATA_PARTITION);
        Source source = new Source();
        source.setSourceId(id);

        assertThrows(BadRequestException.class, () -> sourceStore.create(source));
    }

    @Test
    public void createSystemSource() throws ApplicationException, BadRequestException {
        //given
        Source source = new Source();
        String sourceId = "sourceId";
        source.setSourceId(sourceId);
        String common = "common";

        ReflectionTestUtils.setField(sourceStore, "sharedTenant", common);
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        Source sourceFromStore = sourceStore.createSystemSource(source);

        //then
        assertNotNull(sourceFromStore);
        assertEquals(source.getSourceId(), sourceFromStore.getSourceId());
        SourceDto fromDb = (SourceDto) mongoTemplateHelper.findById(sourceId, SourceDto.class, SOURCE_PREFIX + common);
        assertNotNull(fromDb);
        assertEquals(sourceId, fromDb.getId());
    }
}