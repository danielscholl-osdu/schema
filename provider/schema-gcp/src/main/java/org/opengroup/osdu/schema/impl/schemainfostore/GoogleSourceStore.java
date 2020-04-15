package org.opengroup.osdu.schema.impl.schemainfostore;

import java.text.MessageFormat;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.credentials.DatastoreFactory;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

import lombok.extern.java.Log;

/**
 * Repository class to to register Source in Google store.
 *
 *
 */
@Log
@Repository
public class GoogleSourceStore implements ISourceStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private DatastoreFactory dataStoreFactory;

    @Autowired
    private ITenantFactory tenantFactory;

    /**
     * Method to create Source in google store
     *
     * @param sourceId
     * @return Source object
     * @throws ApplicationException
     */
    @Override
    public Source get(String sourceId) throws NotFoundException, ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(tenantFactory.getTenantInfo(headers.getPartitionId()));
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE).setKind(SchemaConstants.SOURCE_KIND)
                .newKey(sourceId);
        Entity entity = datastore.get(key);
        if (entity == null) {
            throw new NotFoundException("bad input parameter");
        } else {
            return mapEntityToDto(entity);
        }

    }

    /**
     * Method to create Source in google store of dataPartitionId GCP
     *
     * @param Source
     * @param dataPartitionId
     * @return Source object
     */
    @Override
    public Source create(Source source) throws BadRequestException, ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(tenantFactory.getTenantInfo(headers.getPartitionId()));
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE).setKind(SchemaConstants.SOURCE_KIND)
                .newKey(source.getSourceId());
        Entity entity = getEntityObject(source, key);

        try {
            datastore.add(entity);
        } catch (DatastoreException ex) {
            if ("ALREADY_EXISTS".equals(ex.getReason())) {
                log.warning(SchemaConstants.SOURCE_EXISTS);
                throw new BadRequestException(
                        MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION, source.getSourceId()));
            } else {
                log.severe(SchemaConstants.OBJECT_INVALID);
                throw new ApplicationException(SchemaConstants.INVALID_INPUT);
            }
        }
        log.info(SchemaConstants.SOURCE_CREATED);
        return mapEntityToDto(entity);
    }

    private Source mapEntityToDto(Entity entity) {

        Source source = new Source();
        source.setSourceId(entity.getKey().getName());
        return source;

    }

    private Entity getEntityObject(Source source, Key key) {
        Entity.Builder entityBuilder = Entity.newBuilder(key);
        entityBuilder.set(SchemaConstants.DATE_CREATED, Timestamp.now());
        return entityBuilder.build();
    }

}
