package org.opengroup.osdu.schema.provider.aws.provider.schemastore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.stereotype.Repository;

@Repository
public class AwsSchemaStore implements ISchemaStore {
  @Override
  public String createSchema(String filePath, String content) throws ApplicationException {
    return null;
  }

  @Override
  public String getSchema(String dataPartitionId, String filePath) throws NotFoundException, ApplicationException {
    return null;
  }

  @Override
  public boolean cleanSchemaProject(String schemaId) throws ApplicationException {
    return false;
  }
}
