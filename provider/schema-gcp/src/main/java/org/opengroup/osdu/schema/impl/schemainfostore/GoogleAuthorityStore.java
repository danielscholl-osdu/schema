package org.opengroup.osdu.schema.impl.schemainfostore;

import java.text.MessageFormat;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.credentials.DatastoreFactory;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

import lombok.extern.java.Log;

/**
 * Repository class to to register authority in Google store.
 *
 */

@Log
@Repository
public class GoogleAuthorityStore implements IAuthorityStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private DatastoreFactory dataStoreFactory;

    @Autowired
    private ITenantFactory tenantFactory;

    /**
     * Method to get authority from google store
     *
     * @param authorityId
     * @return Authority object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    @Override
    public Authority get(String authorityId) throws NotFoundException, ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(tenantFactory.getTenantInfo(headers.getPartitionId()));
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.AUTHORITY_KIND).newKey(authorityId);

        Entity entity = datastore.get(key);
        if (entity == null) {
            throw new NotFoundException("bad input parameter");
        } else {
            return mapEntityToDto(entity);
        }

    }

    /**
     * Method to create authority in google store of dataPartitionId project
     *
     * @param authority
     * @param dataPartitionId
     * @return Authority object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public Authority create(Authority authority) throws ApplicationException, BadRequestException {
        Datastore datastore = dataStoreFactory.getDatastore(tenantFactory.getTenantInfo(headers.getPartitionId()));
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.AUTHORITY_KIND).newKey(authority.getAuthorityId());
        Entity entity = getEntityObject(authority, key);
        try {
            datastore.add(entity);
        } catch (DatastoreException ex) {
            if ("ALREADY_EXISTS".equals(ex.getReason())) {
                log.warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
                throw new BadRequestException(
                        MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_EXCEPTION, authority.getAuthorityId()));
            } else {
                log.severe(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
                throw new ApplicationException(SchemaConstants.INVALID_INPUT);
            }
        }
        log.info(SchemaConstants.AUTHORITY_CREATED);
        return mapEntityToDto(entity);
    }

    private Authority mapEntityToDto(Entity entity) {

        Authority authority = new Authority();
        authority.setAuthorityId(entity.getKey().getName());
        return authority;

    }

    private Entity getEntityObject(Authority authority, Key key) {
        return Entity.newBuilder(key).set(SchemaConstants.DATE_CREATED, Timestamp.now()).build();
    }

}
