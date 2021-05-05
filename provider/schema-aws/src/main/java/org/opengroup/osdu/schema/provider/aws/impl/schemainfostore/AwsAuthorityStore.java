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
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;
import org.opengroup.osdu.schema.provider.aws.models.AuthorityDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.text.MessageFormat;

@Repository
public class AwsAuthorityStore implements IAuthorityStore {

  @Inject
  private DpsHeaders headers;

  @Inject
  private JaxRsDpsLog logger;

  @Inject
  private DynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;

  @Value("${aws.dynamodb.authorityTable.ssm.relativePath}")
  String authorityTableParameterRelativePath;

  private DynamoDBQueryHelperV2 getAuthorityTableQueryHelper() {
    return dynamoDBQueryHelperFactory.getQueryHelperForPartition(headers, authorityTableParameterRelativePath);
  }


  @Override
  public Authority get(String authorityId) throws NotFoundException, ApplicationException {
    
    DynamoDBQueryHelperV2 queryHelper = getAuthorityTableQueryHelper();
    
    String id = headers.getPartitionId() + ":" + authorityId;
    AuthorityDoc result = queryHelper.loadByPrimaryKey(AuthorityDoc.class, id);
    if (result == null) {
      throw new NotFoundException(SchemaConstants.INVALID_INPUT);
    }
    return result.getAuthority();
  }

  @Override
  public Authority create(Authority authority) throws ApplicationException, BadRequestException {    
    
    DynamoDBQueryHelperV2 queryHelper = getAuthorityTableQueryHelper();

    String id = headers.getPartitionId() + ":" + authority.getAuthorityId();

    AuthorityDoc doc = new AuthorityDoc();
    doc.setId(id);

    try {
      if (queryHelper.keyExistsInTable(AuthorityDoc.class, doc)) {
        logger.warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
        throw new BadRequestException(
                MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_EXCEPTION, authority.getAuthorityId()));
      }
    } catch (Exception ex) {
      logger.error("queryHelper.keyExistsInTable threw exception", ex);
    }

    try {
      doc = new AuthorityDoc(id, headers.getPartitionId(), authority);
      queryHelper.save(doc);
    } catch (Exception ex) {
      logger.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }

    logger.info(SchemaConstants.AUTHORITY_CREATED);
    return authority;
  }
}
