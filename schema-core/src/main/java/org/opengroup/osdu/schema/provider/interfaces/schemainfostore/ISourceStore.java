package org.opengroup.osdu.schema.provider.interfaces.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public interface ISourceStore {

	Source get(String sourceId) throws NotFoundException, ApplicationException;

	default Source getPublicSource(String sourceId) throws NotFoundException, ApplicationException {
		throw new NotImplementedException();
	}

	Source create(Source source) throws BadRequestException, ApplicationException;

	default Source createPublicSource(Source source) throws BadRequestException, ApplicationException {
		throw new NotImplementedException();
	}

}
