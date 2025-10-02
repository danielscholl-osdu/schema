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
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.aws.models.AuthorityDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.util.Optional;

@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "dynamodb",
        matchIfMissing = true)
@Repository
public class AwsAuthorityStore implements IAuthorityStore {

  private final DpsHeaders headers;
  private final JaxRsDpsLog logger;
  private final IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;
  private final String authorityTableParameterRelativePath;
  private final String sharedTenant;

  public AwsAuthorityStore(
          DpsHeaders headers,
          JaxRsDpsLog logger,
          IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory,
          @Value("${aws.dynamodb.authorityTable.ssm.relativePath}") String authorityTableParameterRelativePath,
          @Value("${shared.tenant.name:common}") String sharedTenant) {
    this.headers = headers;
    this.logger = logger;
    this.dynamoDBQueryHelperFactory = dynamoDBQueryHelperFactory;
    this.authorityTableParameterRelativePath = authorityTableParameterRelativePath;
    this.sharedTenant = sharedTenant;
  }

  private DynamoDBQueryHelper<AuthorityDoc> getAuthorityTableQueryHelper() {
    return dynamoDBQueryHelperFactory.createQueryHelper(headers, authorityTableParameterRelativePath, AuthorityDoc.class);
  }

  @Override
  public Authority get(String authorityId) throws NotFoundException, ApplicationException {
    DynamoDBQueryHelper<AuthorityDoc> queryHelper = getAuthorityTableQueryHelper();

    String id = headers.getPartitionId() + ":" + authorityId;
    Optional<AuthorityDoc> result = queryHelper.getItem(id);
    if (result.isEmpty()) {
      throw new NotFoundException(SchemaConstants.INVALID_INPUT);
    }
    return result.get().getAuthority();
  }

  @Override
  public Authority getSystemAuthority(String authorityId) throws NotFoundException, ApplicationException {
    this.updateDataPartitionId();
    return this.get(authorityId);
  }

  @Override
public Authority create(Authority authority) throws ApplicationException, BadRequestException {
    DynamoDBQueryHelper<AuthorityDoc> queryHelper = getAuthorityTableQueryHelper();
    String id = headers.getPartitionId() + ":" + authority.getAuthorityId();

    try {
        // Check if the authority already exists
        if (queryHelper.getItem(id).isPresent()) {
            logger.info(MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED, authority.getAuthorityId()));
            return authority;
        }

        // Create and save the document
        AuthorityDoc doc = new AuthorityDoc(id, headers.getPartitionId(), authority);
        queryHelper.putItem(doc);

        logger.info(SchemaConstants.AUTHORITY_CREATED);
        return authority;
    } catch (Exception ex) {
        logger.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
        throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }
}

  @Override
  public Authority createSystemAuthority(Authority authority) throws ApplicationException, BadRequestException {
    updateDataPartitionId();
    return this.create(authority);
  }

  private void updateDataPartitionId() {
    headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
  }
}
