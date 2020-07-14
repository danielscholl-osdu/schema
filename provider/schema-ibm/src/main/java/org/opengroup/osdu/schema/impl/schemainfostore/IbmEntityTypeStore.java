package org.opengroup.osdu.schema.impl.schemainfostore;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import javax.annotation.PostConstruct;

import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.impl.schemainfostore.util.IbmDocumentStore;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.ibm.EntityTypeDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Repository class to register Entity type in IBM store.
 *
 *
 */
@Repository
@RequestScope
public class IbmEntityTypeStore extends IbmDocumentStore implements IEntityTypeStore {
	
    @PostConstruct
	public void init() throws MalformedURLException {
    	initFactory(SchemaConstants.ENTITYTYPE_KIND);
	}
	
	@Override
	public EntityType get(String entityTypeId) throws NotFoundException, ApplicationException {
		if (db.contains(entityTypeId)) {
			EntityTypeDoc sd = db.find(EntityTypeDoc.class, entityTypeId);
			return sd.getEntityType();
		} else {
			throw new NotFoundException(SchemaConstants.INVALID_INPUT);
		}
	}

	@Override
	public EntityType create(EntityType entityType) throws BadRequestException, ApplicationException {

		if (db.contains(entityType.getEntityTypeId())) {
			logger.warning(SchemaConstants.ENTITY_TYPE_EXISTS);
			throw new BadRequestException(
					MessageFormat.format(SchemaConstants.ENTITY_TYPE_EXISTS_EXCEPTION, entityType.getEntityTypeId()));
		}
		EntityTypeDoc sd = new EntityTypeDoc(entityType);

		try {
			db.save(sd);
		} catch (Exception ex) {
			logger.error(SchemaConstants.OBJECT_INVALID, ex);
			throw new ApplicationException(SchemaConstants.INVALID_INPUT);
		}
		logger.info(SchemaConstants.ENTITY_TYPE_CREATED);
		return sd.getEntityType();
	}


}
