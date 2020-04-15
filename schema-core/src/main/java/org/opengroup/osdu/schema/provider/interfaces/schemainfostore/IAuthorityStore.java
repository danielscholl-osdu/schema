package org.opengroup.osdu.schema.provider.interfaces.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;

public interface IAuthorityStore {

	Authority get(String authorityId) throws NotFoundException, ApplicationException;

	Authority create(Authority authority) throws ApplicationException, BadRequestException;

}
