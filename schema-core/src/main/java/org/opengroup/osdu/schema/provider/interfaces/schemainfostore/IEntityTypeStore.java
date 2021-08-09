package org.opengroup.osdu.schema.provider.interfaces.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public interface IEntityTypeStore {

	EntityType get(String entityTypeId) throws NotFoundException, ApplicationException;

	default EntityType getPublicEntity(String entityTypeId) throws NotFoundException, ApplicationException {
		throw new NotImplementedException();
	}

	EntityType create(EntityType entityType) throws BadRequestException, ApplicationException;

	default EntityType createPublicEntity(EntityType entityType) throws BadRequestException, ApplicationException {
		throw new NotImplementedException();
	}

}
