package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelper;
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
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.MessageFormat;

@Repository
public class AwsAuthorityStore implements IAuthorityStore {

  @Inject
  private DpsHeaders headers;

  @Inject
  private JaxRsDpsLog logger;

  @Inject
  private AwsServiceConfig serviceConfig;

  private DynamoDBQueryHelper queryHelper;

  @PostConstruct
  public void init() {
    // TODO: serviceConfig.environment isn't correct and needs to be table prefix. Maybe the "-" will fix it
    queryHelper = new DynamoDBQueryHelper(serviceConfig.getDynamoDbEndpoint(),
            serviceConfig.getAmazonRegion(),
            serviceConfig.getDynamoDbTablePrefix());
  }

  @Override
  public Authority get(String authorityId) throws NotFoundException, ApplicationException {
     String id = headers.getPartitionId() + ":" + authorityId;
     AuthorityDoc result = queryHelper.loadByPrimaryKey(AuthorityDoc.class, id);
     if (result == null) {
       throw new NotFoundException(SchemaConstants.INVALID_INPUT);
     }
     return result.getAuthority();
  }

  @Override
  public Authority create(Authority authority) throws ApplicationException, BadRequestException {
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
