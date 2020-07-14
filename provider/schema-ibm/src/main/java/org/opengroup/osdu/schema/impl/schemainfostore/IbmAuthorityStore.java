package org.opengroup.osdu.schema.impl.schemainfostore;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import javax.annotation.PostConstruct;

import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.impl.schemainfostore.util.IbmDocumentStore;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.ibm.AuthorityDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Repository class to to register authority in IBM store.
 *
 */
@Repository
@RequestScope
public class IbmAuthorityStore extends IbmDocumentStore implements IAuthorityStore {

	@PostConstruct
	public void init() throws MalformedURLException {
		initFactory(SchemaConstants.AUTHORITY);
	}
	
	@Override
	public Authority get(String authorityId) throws NotFoundException, ApplicationException {

		if (db.contains(authorityId)) {
			AuthorityDoc sd = db.find(AuthorityDoc.class, authorityId);
			return sd.getAuthority();
		} else {
			throw new NotFoundException(SchemaConstants.INVALID_INPUT);
		}
	}

	@Override
	public Authority create(Authority authority) throws ApplicationException, BadRequestException {

		if (db.contains(authority.getAuthorityId())) {
			logger.warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
            throw new BadRequestException(
                    MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_EXCEPTION, authority.getAuthorityId()));
		}
		AuthorityDoc sd = new AuthorityDoc(authority);
		try {
			db.save(sd);
		} catch (Exception ex) {
			logger.error(SchemaConstants.OBJECT_INVALID, ex);
			throw new ApplicationException(SchemaConstants.INVALID_INPUT);
		}
		logger.info(SchemaConstants.AUTHORITY_CREATED);
		return sd.getAuthority();
	}

}
