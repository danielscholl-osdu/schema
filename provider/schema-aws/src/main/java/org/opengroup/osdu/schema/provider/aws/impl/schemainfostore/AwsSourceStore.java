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

import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperV2;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.aws.models.SourceDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.text.MessageFormat;

@Repository
public class AwsSourceStore implements ISourceStore {

  public static String SOURCE_NOT_FOUND = "source not found";

  @Inject
  private DpsHeaders headers;

  @Inject
  private JaxRsDpsLog log;

  @Inject
  private DynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;

  @Value("${aws.dynamodb.sourceTable.ssm.relativePath}")
  String sourceTableParameterRelativePath;

  @Value("${shared.tenant.name:common}")
  private String sharedTenant;

  private DynamoDBQueryHelperV2 getSourceTableQueryHelper() {
    return dynamoDBQueryHelperFactory.getQueryHelperForPartition(headers, sourceTableParameterRelativePath);
  }

  @Override
  public Source get(String sourceId) throws NotFoundException, ApplicationException {

    DynamoDBQueryHelperV2 queryHelper = getSourceTableQueryHelper();

    String id = headers.getPartitionId() + ":" + sourceId;
    SourceDoc result = queryHelper.loadByPrimaryKey(SourceDoc.class, id);
    if (result == null) {
      throw new NotFoundException(SOURCE_NOT_FOUND);
    }
    return result.getSource();
  }

  @Override
  public Source getSystemSource(String sourceId) throws NotFoundException, ApplicationException {
    this.updateDataPartitionId();
    return this.get(sourceId);
  }

  @Override
  public Source create(Source source) throws BadRequestException, ApplicationException {

    Source result = null;
    try {
      result = this.get(source.getSourceId());
      if (result != null) {
        throw new BadRequestException(MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION,
                source.getSourceId()));
      }
    } catch (NotFoundException e) { }

    try {

      DynamoDBQueryHelperV2 queryHelper = getSourceTableQueryHelper();

      String id = headers.getPartitionId() + ":" + source.getSourceId();
      SourceDoc sourceDoc = new SourceDoc(id, headers.getPartitionId(), source);
      queryHelper.save(sourceDoc);

    } catch (Exception e) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, e.getMessage()));
      throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }

    log.info(SchemaConstants.SOURCE_CREATED);
    return source;
  }

  @Override
  public Source createSystemSource(Source source) throws BadRequestException, ApplicationException {
    this.updateDataPartitionId();
    return this.create(source);
  }

  private void updateDataPartitionId() {
    headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
  }
}
