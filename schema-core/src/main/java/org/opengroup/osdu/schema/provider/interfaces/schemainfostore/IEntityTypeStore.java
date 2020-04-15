package org.opengroup.osdu.schema.provider.interfaces.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;

public interface IEntityTypeStore {

	EntityType get(String entityId) throws NotFoundException, ApplicationException;

	EntityType create(EntityType entityType) throws BadRequestException, ApplicationException;

}
