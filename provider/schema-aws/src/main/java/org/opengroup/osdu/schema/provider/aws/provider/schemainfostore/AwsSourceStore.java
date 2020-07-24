package org.opengroup.osdu.schema.provider.aws.provider.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.stereotype.Repository;

@Repository
public class AwsSourceStore implements ISourceStore {

  @Override
  public Source get(String sourceId) throws NotFoundException, ApplicationException {
    return null;
  }

  @Override
  public Source create(Source source) throws BadRequestException, ApplicationException {
    return null;
  }
}
