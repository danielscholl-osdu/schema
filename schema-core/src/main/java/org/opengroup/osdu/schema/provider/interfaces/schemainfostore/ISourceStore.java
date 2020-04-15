package org.opengroup.osdu.schema.provider.interfaces.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;

public interface ISourceStore {

	Source get(String sourceId) throws NotFoundException, ApplicationException;

	Source create(Source source) throws BadRequestException, ApplicationException;

}
