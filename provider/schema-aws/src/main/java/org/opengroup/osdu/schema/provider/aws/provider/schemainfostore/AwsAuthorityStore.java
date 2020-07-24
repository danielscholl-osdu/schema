package org.opengroup.osdu.schema.provider.aws.provider.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.stereotype.Repository;

@Repository
public class AwsAuthorityStore implements IAuthorityStore {

  @Override
  public Authority get(String authorityId) throws NotFoundException, ApplicationException {
    return null;
  }

  @Override
  public Authority create(Authority authority) throws ApplicationException, BadRequestException {
    return null;
  }
}
