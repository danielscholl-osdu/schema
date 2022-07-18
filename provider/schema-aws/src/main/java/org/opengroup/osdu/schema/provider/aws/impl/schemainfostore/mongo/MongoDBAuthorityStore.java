// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo;

import org.opengroup.osdu.core.aws.mongodb.MongoDBMultiClusterFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.AuthorityDto;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.SchemaDBIndexFields;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.text.MessageFormat;

/**
 * The type Aws authority store mongo db.
 */
@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "mongodb")
@Repository
public class MongoDBAuthorityStore implements IAuthorityStore {
    /**
     * The constant AUTHORITY_PREFIX.
     */
    public static final String AUTHORITY_PREFIX = "Authority-";

    @Inject
    private DpsHeaders headers;

    @Inject
    private JaxRsDpsLog log;

    @Inject
    private MongoDBMultiClusterFactory mongoDBMultiClusterFactory;

    @Value("${shared.tenant.name:common}")
    private String sharedTenant;

    private String getDataPartitionId() {
        this.log.warning("TenantInfo found to be null, defaulting to partition id from headers");
        return this.headers.getPartitionId();
    }

    /**
     * Get system schemaInfo.
     *
     * @param authorityId the authority id
     * @return the Authority
     */
    @Override
    public Authority get(String authorityId) throws NotFoundException, ApplicationException {
        String dataPartitionId = getDataPartitionId();
        boolean exists = mongoDBMultiClusterFactory.getHelper(dataPartitionId).existsByField(SchemaDBIndexFields.ID, authorityId, AuthorityDto.class, getCollection(dataPartitionId));
        if (!exists) {
            throw new NotFoundException(SchemaConstants.INVALID_INPUT);
        }
        Authority authority = new Authority();
        authority.setAuthorityId(authorityId);
        return authority;
    }

    /**
     * Get system Authority.
     *
     * @param authorityId the authority id
     * @return the Authority
     */
    @Override
    public Authority getSystemAuthority(String authorityId) throws NotFoundException, ApplicationException {
        this.updateDataPartitionId();
        return this.get(authorityId);
    }

    /**
     * Create Authority.
     *
     * @param authority the Authority
     * @return the Authority
     */
    @Override
    public Authority create(Authority authority) throws ApplicationException, BadRequestException {
        try {
            AuthorityDto authorityDto = new AuthorityDto(authority.getAuthorityId());
            String dataPartitionId = getDataPartitionId();
            mongoDBMultiClusterFactory.getHelper(dataPartitionId).insert(authorityDto, getCollection(dataPartitionId));
        } catch (DuplicateKeyException duplicateKeyException) {
            log.warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
            throw new BadRequestException(
                    MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_EXCEPTION, authority.getAuthorityId()));
        } catch (Exception exception) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, exception.getMessage()));
            throw new ApplicationException(SchemaConstants.INVALID_INPUT);
        }
        return authority;
    }

    /**
     * Create system Authority.
     *
     * @param authority the Authority
     * @return the Authority
     */
    @Override
    public Authority createSystemAuthority(Authority authority) throws ApplicationException, BadRequestException {
        updateDataPartitionId();
        return this.create(authority);
    }

    private void updateDataPartitionId() {
        headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
    }

    private String getCollection(String dataPartitionId) {
        return AUTHORITY_PREFIX + dataPartitionId;
    }
}
