/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.schema.impl.schemainfostore;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import javax.annotation.PostConstruct;

import org.opengroup.osdu.schema.constants.SchemaConstants;
//import org.opengroup.osdu.schema.credentials.DatastoreFactory;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.impl.schemainfostore.util.IbmDocumentStore;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.ibm.SourceDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Repository class to to register Source in Google store.
 *
 *
 */
@Repository
@RequestScope
public class IbmSourceStore extends IbmDocumentStore implements ISourceStore {

    @PostConstruct
	public void init() throws MalformedURLException {
		initFactory(SchemaConstants.SOURCE_KIND);
	}

	@Override
	public Source get(String sourceId) throws NotFoundException, ApplicationException {
		if (db.contains(sourceId)) {
			SourceDoc sd = db.find(SourceDoc.class, sourceId);
			return sd.getSource();
		} else {
			throw new NotFoundException(SchemaConstants.INVALID_INPUT);
		}
	}

	@Override
	public Source getSystemSource(String sourceId) throws NotFoundException, ApplicationException {
		updateDataPartitionId();
    	return this.get(sourceId);
	}

	@Override
	public Source create(Source source) throws BadRequestException, ApplicationException {

		if (db.contains(source.getSourceId())) {
			logger.warning(SchemaConstants.SOURCE_EXISTS);
			throw new BadRequestException(
					MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION, source.getSourceId()));
		}
		SourceDoc sd = new SourceDoc(source);

		try {
			db.save(sd);
		} catch (Exception ex) {
			logger.error(SchemaConstants.OBJECT_INVALID, ex);
			throw new ApplicationException(SchemaConstants.INVALID_INPUT);
		}
		logger.info(SchemaConstants.SOURCE_CREATED);
		return sd.getSource();
	}

	@Override
	public Source createSystemSource(Source source) throws BadRequestException, ApplicationException {
		updateDataPartitionId();
    	return this.create(source);
	}
}
