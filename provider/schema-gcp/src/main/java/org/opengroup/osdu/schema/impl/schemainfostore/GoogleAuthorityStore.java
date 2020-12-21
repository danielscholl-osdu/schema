/*
  Copyright 2020 Google LLC
  Copyright 2020 EPAM Systems, Inc

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package org.opengroup.osdu.schema.impl.schemainfostore;

import java.text.MessageFormat;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.gcp.multitenancy.DatastoreFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
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

/**
 * Repository class to to register authority in Google store.
 *
 */

@Repository
public class GoogleAuthorityStore implements IAuthorityStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private DatastoreFactory dataStoreFactory;

    @Autowired
    JaxRsDpsLog log;

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
        Datastore datastore = dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE);
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
        Datastore datastore = dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE);
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.AUTHORITY_KIND).newKey(authority.getAuthorityId());
        Entity entity = getEntityObject(key);
        try {
            datastore.add(entity);
        } catch (DatastoreException ex) {
            if ("ALREADY_EXISTS".equals(ex.getReason())) {
                log.warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
                throw new BadRequestException(
                        MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_EXCEPTION, authority.getAuthorityId()));
            } else {
                log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
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

    private Entity getEntityObject(Key key) {
        return Entity.newBuilder(key).set(SchemaConstants.DATE_CREATED, Timestamp.now()).build();
    }

}
