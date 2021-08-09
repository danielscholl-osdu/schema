package org.opengroup.osdu.schema.provider.interfaces.schemainfostore;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public interface IAuthorityStore {

	Authority get(String authorityId) throws NotFoundException, ApplicationException;

	default Authority getPublicAuthority(String authorityId) throws NotFoundException, ApplicationException {
		throw new NotImplementedException();
	}

	Authority create(Authority authority) throws ApplicationException, BadRequestException;

	default Authority createPublicAuthority(Authority authority) throws ApplicationException, BadRequestException {
		throw new NotImplementedException();
	}

}
