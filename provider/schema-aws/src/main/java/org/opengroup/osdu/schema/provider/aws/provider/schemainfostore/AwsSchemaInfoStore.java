package org.opengroup.osdu.schema.provider.aws.provider.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AwsSchemaInfoStore implements ISchemaInfoStore {

  @Override
  public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    return null;
  }

  @Override
  public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    return null;
  }

  @Override
  public SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
    return null;
  }

  @Override
  public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {
    return null;
  }

  @Override
  public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException {
    return null;
  }

  @Override
  public boolean isUnique(String schemaId, String tenantId) throws ApplicationException {
    return false;
  }

  @Override
  public boolean cleanSchema(String schemaId) throws ApplicationException {
    return false;
  }
}
