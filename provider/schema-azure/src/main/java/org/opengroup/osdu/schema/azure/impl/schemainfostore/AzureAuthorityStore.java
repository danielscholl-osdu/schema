// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.schema.azure.impl.schemainfostore;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.azure.CosmosStore;
import org.opengroup.osdu.schema.azure.definitions.AuthorityDoc;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;

/**
 * Repository class to to register authority in Azure store.
 *
 */

@Repository
public class AzureAuthorityStore implements IAuthorityStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private String authorityContainer;

    @Autowired
    private CosmosStore cosmosStore;

    @Autowired
    private String cosmosDBName;

    @Autowired
    JaxRsDpsLog log;

    /**
     * Method to get Authority from Azure store
     * @param authorityId
     * @return
     * @throws NotFoundException
     * @throws ApplicationException
     */
    @Override
    public Authority get(String authorityId) throws NotFoundException, ApplicationException {

        String id = headers.getPartitionId() + ":" + authorityId;

        AuthorityDoc authorityDoc = cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, authorityContainer, id, headers.getPartitionId(), AuthorityDoc.class)
                .orElseThrow(() -> new NotFoundException("bad input parameter"));

        return authorityDoc.getAuthority();
    }

    /**
     * Method to register an authority in azure store.
     * @param authority
     * @return
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public Authority create(Authority authority) throws ApplicationException, BadRequestException {
        String id = headers.getPartitionId() + ":" + authority.getAuthorityId();

        try {
            AuthorityDoc authorityDoc = new AuthorityDoc(id, headers.getPartitionId(), authority);
            cosmosStore.createItem(headers.getPartitionId(), cosmosDBName, authorityContainer, authorityDoc);
        } catch (AppException ex) {
            if (ex.getError().getCode() == 409) {
                log.warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
                throw new BadRequestException(MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_EXCEPTION,
                        authority.getAuthorityId()));
            } else {
                log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
                throw new ApplicationException(SchemaConstants.INVALID_INPUT);
            }
        }

        log.info(SchemaConstants.AUTHORITY_CREATED);
        return authority;
    }
}
