package org.opengroup.osdu.schema.provider.aws.provider.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.stereotype.Repository;

@Repository
public class AwsEntityTypeStore implements IEntityTypeStore {

  @Override
  public EntityType get(String entityTypeId) throws NotFoundException, ApplicationException {
    return null;
  }

  @Override
  public EntityType create(EntityType entityType) throws BadRequestException, ApplicationException {
    return null;
  }
}
