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
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.aws.models.SourceDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.util.Optional;

@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "dynamodb",
        matchIfMissing = true)
@Repository
public class AwsSourceStore implements ISourceStore {

  public static final String SOURCE_NOT_FOUND = "source not found";

  private final DpsHeaders headers;
  private final JaxRsDpsLog log;
  private final IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;
  private final String sourceTableParameterRelativePath;
  private final String sharedTenant;

  public AwsSourceStore(
          DpsHeaders headers,
          JaxRsDpsLog log,
          IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory,
          @Value("${aws.dynamodb.sourceTable.ssm.relativePath}") String sourceTableParameterRelativePath,
          @Value("${shared.tenant.name:common}") String sharedTenant) {
    this.headers = headers;
    this.log = log;
    this.dynamoDBQueryHelperFactory = dynamoDBQueryHelperFactory;
    this.sourceTableParameterRelativePath = sourceTableParameterRelativePath;
    this.sharedTenant = sharedTenant;
  }

  private DynamoDBQueryHelper<SourceDoc> getSourceTableQueryHelper() {
    return dynamoDBQueryHelperFactory.createQueryHelper(headers, sourceTableParameterRelativePath, SourceDoc.class);
  }

  @Override
  public Source get(String sourceId) throws NotFoundException, ApplicationException {
    DynamoDBQueryHelper<SourceDoc> queryHelper = getSourceTableQueryHelper();

    String id = headers.getPartitionId() + ":" + sourceId;
    Optional<SourceDoc> result = queryHelper.getItem(id);
    if (result.isEmpty()) {
      throw new NotFoundException(SOURCE_NOT_FOUND);
    }
    return result.get().getSource();
  }

  @Override
  public Source getSystemSource(String sourceId) throws NotFoundException, ApplicationException {
    this.updateDataPartitionId();
    return this.get(sourceId);
  }

  private boolean checkExist(Source source) throws ApplicationException {
    try {
      Source result = this.get(source.getSourceId());
      if (result != null) {
        throw new BadRequestException(MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION,
                source.getSourceId()));
      }
      return true;
    } catch (NotFoundException e) {
      return false;
    }
  }

  @Override
  public Source create(Source source) throws BadRequestException, ApplicationException {
    if (!checkExist(source)) {
      try {
        DynamoDBQueryHelper<SourceDoc> queryHelper = getSourceTableQueryHelper();

        String id = headers.getPartitionId() + ":" + source.getSourceId();
        SourceDoc sourceDoc = new SourceDoc(id, headers.getPartitionId(), source);
        queryHelper.putItem(sourceDoc);

        log.info(SchemaConstants.SOURCE_CREATED);
      } catch (Exception e) {
        log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, e.getMessage()));
        throw new ApplicationException(SchemaConstants.INVALID_INPUT);
      }
    }
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
