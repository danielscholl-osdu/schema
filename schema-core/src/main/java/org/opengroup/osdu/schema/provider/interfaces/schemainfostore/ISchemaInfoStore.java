
package org.opengroup.osdu.schema.provider.interfaces.schemainfostore;

import java.util.List;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;

public interface ISchemaInfoStore {

    SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException;

    SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException;

    SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException;

    String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException;

    List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException;

    boolean isUnique(String schemaId, String tenantId) throws ApplicationException;

    boolean cleanSchema(String schemaId) throws ApplicationException;
}
