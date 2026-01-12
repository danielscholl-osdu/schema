/*
 * Copyright © Amazon Web Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.aws.models.EntityTypeDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.util.Optional;

@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "dynamodb",
        matchIfMissing = true)
@Repository
public class AwsEntityTypeStore implements IEntityTypeStore {

  private final DpsHeaders headers;
  private final JaxRsDpsLog log;
  private final IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;
  private final String entityTypeTableParameterRelativePath;
  private final String sharedTenant;

  public AwsEntityTypeStore(
          DpsHeaders headers,
          JaxRsDpsLog log,
          IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory,
          @Value("${aws.dynamodb.entityTypeTable.ssm.relativePath}") String entityTypeTableParameterRelativePath,
          @Value("${shared.tenant.name:common}") String sharedTenant) {
    this.headers = headers;
    this.log = log;
    this.dynamoDBQueryHelperFactory = dynamoDBQueryHelperFactory;
    this.entityTypeTableParameterRelativePath = entityTypeTableParameterRelativePath;
    this.sharedTenant = sharedTenant;
  }

  private DynamoDBQueryHelper<EntityTypeDoc> getEntityTypeTableQueryHelper() {
    return dynamoDBQueryHelperFactory.createQueryHelper(headers, entityTypeTableParameterRelativePath, EntityTypeDoc.class);
  }

  @Override
  public EntityType get(String entityTypeId) throws NotFoundException, ApplicationException {
    DynamoDBQueryHelper<EntityTypeDoc> queryHelper = getEntityTypeTableQueryHelper();

    String id = headers.getPartitionId() + ":" + entityTypeId;
    Optional<EntityTypeDoc> result = queryHelper.getItem(id);
    if (result.isEmpty()) {
      throw new NotFoundException(SchemaConstants.INVALID_INPUT);
    }
    return result.get().getEntityType();
  }

  @Override
  public EntityType getSystemEntity(String entityTypeId) throws NotFoundException, ApplicationException {
    updateDataPartitionId();
    return this.get(entityTypeId);
  }

  @Override
  public EntityType create(EntityType entityType) throws BadRequestException, ApplicationException {
    DynamoDBQueryHelper<EntityTypeDoc> queryHelper = getEntityTypeTableQueryHelper();

    String id = headers.getPartitionId() + ":" + entityType.getEntityTypeId();

    try {
      if (queryHelper.getItem(id).isPresent()) {
        log.warning(SchemaConstants.ENTITY_TYPE_EXISTS);
        throw new BadRequestException(
                MessageFormat.format(SchemaConstants.ENTITY_TYPE_EXISTS_EXCEPTION, entityType.getEntityTypeId()));
      }

      EntityTypeDoc doc = new EntityTypeDoc();
      doc.setId(id);
      doc.setDataPartitionId(headers.getPartitionId());
      doc.setEntityType(entityType);
      
      queryHelper.putItem(doc);
    } catch (BadRequestException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }
    
    log.info(SchemaConstants.ENTITY_TYPE_CREATED);
    return entityType;
  }

  @Override
  public EntityType createSystemEntity(EntityType entityType) throws BadRequestException, ApplicationException {
    this.updateDataPartitionId();
    return this.create(entityType);
  }

  private void updateDataPartitionId() {
    headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
  }
}
