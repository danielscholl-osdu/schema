package org.opengroup.osdu.schema.provider.aws.mongo.impl.schemainfostore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.EntityTypeDto;
import org.opengroup.osdu.schema.provider.aws.mongo.config.SchemaTestConfig;
import org.opengroup.osdu.schema.provider.aws.mongo.util.ParentUtil;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.MongoDBEntityTypeStore.ENTITY_TYPE_PREFIX;

@DataMongoTest
@RunWith(SpringRunner.class)
@SpringJUnitConfig(classes = {SchemaTestConfig.class})
public class MongoDBEntityTypeStoreTest extends ParentUtil {
    @Autowired
    private IEntityTypeStore awsEntityTypeStore;

    @Test
    public void get() throws ApplicationException, NotFoundException {
        //given
        String id = "entityTypeId";
        EntityTypeDto entityTypeDto = new EntityTypeDto();
        entityTypeDto.setId(id);
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(id);
        entityTypeDto.setData(entityType);
        mongoTemplateHelper.insert(entityTypeDto, ENTITY_TYPE_PREFIX + DATA_PARTITION);

        //when
        EntityType authorityfromstore = awsEntityTypeStore.get(entityTypeDto.getId());

        //then
        assertNotNull(authorityfromstore);
        assertEquals(id, authorityfromstore.getEntityTypeId());
    }

    @Test(expected = NotFoundException.class)
    public void getNotFound() throws ApplicationException, NotFoundException {
        //given
        String id = "entityTypeId";
        EntityTypeDto byId = (EntityTypeDto) mongoTemplateHelper.findById(id, EntityTypeDto.class, ENTITY_TYPE_PREFIX + DATA_PARTITION);
        assertNull(byId);

        //then
        awsEntityTypeStore.get(id);
    }

    @Test
    public void getSystemEntityType() throws ApplicationException, NotFoundException {
        //given
        String id = "entityTypeId";
        EntityTypeDto entityTypeDto = new EntityTypeDto();
        entityTypeDto.setId(id);
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(id);
        entityTypeDto.setData(entityType);
        mongoTemplateHelper.insert(entityTypeDto, ENTITY_TYPE_PREFIX + "common");

        ReflectionTestUtils.setField(awsEntityTypeStore, "sharedTenant", "common");
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        EntityType systemEntity = awsEntityTypeStore.getSystemEntity(id);
        //then
        assertNotNull(systemEntity);
        assertEquals(id, systemEntity.getEntityTypeId());
    }

    @Test
    public void create() throws ApplicationException, BadRequestException {
        //given
        String id = "entityTypeId";
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(id);

        //when
        EntityType entityTypeFromStore = awsEntityTypeStore.create(entityType);

        //then
        assertNotNull(entityTypeFromStore);
        assertEquals(id, entityTypeFromStore.getEntityTypeId());
        EntityTypeDto entityTypeDto = (EntityTypeDto) mongoTemplateHelper.findById(id, EntityTypeDto.class, ENTITY_TYPE_PREFIX + DATA_PARTITION);
        assertNotNull(entityTypeDto);
        assertEquals(id, entityTypeDto.getId());
        assertEquals(id, entityTypeDto.getData().getEntityTypeId());
    }

    @Test(expected = BadRequestException.class)
    public void createDuplicate() throws ApplicationException, BadRequestException {
        //given
        String id = "entityTypeId";
        EntityTypeDto entityTypeDto = new EntityTypeDto();
        entityTypeDto.setId(id);
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(id);
        entityTypeDto.setData(entityType);
        mongoTemplateHelper.insert(entityTypeDto, ENTITY_TYPE_PREFIX + DATA_PARTITION);

        //then
        awsEntityTypeStore.create(entityType);
    }

    @Test
    public void createSystemEntityType() throws ApplicationException, BadRequestException {
        //given
        String id = "entityTypeId";
        EntityType entityType = new EntityType();
        entityType.setEntityTypeId(id);

        String common = "common";

        ReflectionTestUtils.setField(awsEntityTypeStore, "sharedTenant", common);
        Mockito.when(headers.getPartitionId()).thenReturn("common");

        //when
        EntityType systemEntity = awsEntityTypeStore.createSystemEntity(entityType);

        //then
        assertNotNull(systemEntity);
        assertEquals(id, systemEntity.getEntityTypeId());
        EntityTypeDto fromDb = (EntityTypeDto) mongoTemplateHelper.findById(id, EntityTypeDto.class, ENTITY_TYPE_PREFIX + common);
        assertNotNull(fromDb);
        assertEquals(id, fromDb.getId());
    }
}