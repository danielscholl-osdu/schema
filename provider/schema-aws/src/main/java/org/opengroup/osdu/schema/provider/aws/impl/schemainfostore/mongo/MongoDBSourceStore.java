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
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.SchemaDBIndexFields;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.SourceDto;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import jakarta.inject.Inject;
import java.text.MessageFormat;

/**
 * The type Aws source store mongo db.
 */
@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "mongodb")
@Repository
public class MongoDBSourceStore implements ISourceStore {

    /**
     * The constant SOURCE_NOT_FOUND.
     */
    public static final String SOURCE_NOT_FOUND = "source not found";
    /**
     * The constant SOURCE_PREFIX.
     */
    public static final String SOURCE_PREFIX = "Source-";

    @Inject
    private DpsHeaders headers;

    @Inject
    private JaxRsDpsLog log;

    @Value("${shared.tenant.name:common}")
    private String sharedTenant;

    @Inject
    private MongoDBMultiClusterFactory mongoDBMultiClusterFactory;

    private String getDataPartitionId() {
        this.log.warning("TenantInfo found to be null, defaulting to partition id from headers");
        return this.headers.getPartitionId();
    }

    /**
     * Get Source.
     *
     * @param sourceId the source Id
     * @return the Source
     */
    @Override
    public Source get(String sourceId) throws NotFoundException, ApplicationException {
        String dataPartitionId = getDataPartitionId();
        boolean exists;
        try {
            exists = mongoDBMultiClusterFactory.getHelper(dataPartitionId).existsByField(SchemaDBIndexFields.ID, sourceId, SourceDto.class, getCollection(dataPartitionId));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
        if (!exists) {
            throw new NotFoundException(SOURCE_NOT_FOUND);
        }
        Source source = new Source();
        source.setSourceId(sourceId);
        return source;
    }

    /**
     * Get system Source.
     *
     * @param sourceId the source Id
     * @return the Source
     */
    @Override
    public Source getSystemSource(String sourceId) throws NotFoundException, ApplicationException {
        updateDataPartitionId();
        return this.get(sourceId);
    }

    /**
     * Create Source.
     *
     * @param source the Source
     * @return the Source
     */
    @Override
    public Source create(Source source) throws BadRequestException, ApplicationException {
        try {
            SourceDto sourceDto = new SourceDto(source.getSourceId());
            String dataPartitionId = getDataPartitionId();
            mongoDBMultiClusterFactory.getHelper(dataPartitionId).insert(sourceDto, getCollection(dataPartitionId));
            log.info(SchemaConstants.SOURCE_CREATED);
        } catch (DuplicateKeyException exception) {
            throw new BadRequestException(MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION,
                    source.getSourceId()));
        } catch (Exception exception) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, exception.getMessage()));
            throw new ApplicationException(SchemaConstants.INVALID_INPUT);
        }
        return source;
    }

    /**
     * Create system Source.
     *
     * @param source the Source
     * @return the Source
     */
    @Override
    public Source createSystemSource(Source source) throws BadRequestException, ApplicationException {
        updateDataPartitionId();
        return create(source);
    }

    private void updateDataPartitionId() {
        headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
    }

    private String getCollection(String dataPartitionId) {
        return SOURCE_PREFIX + dataPartitionId;
    }
}
