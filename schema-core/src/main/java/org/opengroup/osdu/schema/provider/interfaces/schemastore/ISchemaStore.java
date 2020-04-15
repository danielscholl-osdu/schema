package org.opengroup.osdu.schema.provider.interfaces.schemastore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;

public interface ISchemaStore {

    String createSchema(String filePath, String content) throws ApplicationException;

    String getSchema(String dataPartitionId, String filePath) throws NotFoundException, ApplicationException;

    boolean cleanSchemaProject(String schemaId) throws ApplicationException;
}
