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

import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;
import org.opengroup.osdu.schema.provider.aws.models.EntityTypeDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.MessageFormat;

@Repository
public class AwsEntityTypeStore implements IEntityTypeStore {

  @Inject
  private DpsHeaders headers;

  @Inject
  private JaxRsDpsLog log;

  @Inject
  private AwsServiceConfig serviceConfig;

  private DynamoDBQueryHelper queryHelper;

  @PostConstruct
  public void init() {
    queryHelper = new DynamoDBQueryHelper(serviceConfig.getDynamoDbEndpoint(),
            serviceConfig.getAmazonRegion(),
            serviceConfig.getDynamoDbTablePrefix());
  }

  @Override
  public EntityType get(String entityTypeId) throws NotFoundException, ApplicationException {
    String id = headers.getPartitionId() + ":" + entityTypeId;
    EntityTypeDoc result = queryHelper.loadByPrimaryKey(EntityTypeDoc.class, id);
    if (result == null) {
      throw new NotFoundException(SchemaConstants.INVALID_INPUT);
    }
    return result.getEntityType();
  }

  @Override
  public EntityType create(EntityType entityType) throws BadRequestException, ApplicationException {
    String id = headers.getPartitionId() + ":" + entityType.getEntityTypeId();

    EntityTypeDoc doc = new EntityTypeDoc();
    doc.setId(id);
    if (queryHelper.keyExistsInTable(EntityTypeDoc.class, doc)) {
      log.warning(SchemaConstants.ENTITY_TYPE_EXISTS);
      throw new BadRequestException(
              MessageFormat.format(SchemaConstants.ENTITY_TYPE_EXISTS_EXCEPTION, entityType.getEntityTypeId()));
    }

    try{
      doc = new EntityTypeDoc(id, headers.getPartitionId(), entityType);
      queryHelper.save(doc);
    } catch (Exception ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }
    log.info(SchemaConstants.ENTITY_TYPE_CREATED);
    return entityType;
  }
}
