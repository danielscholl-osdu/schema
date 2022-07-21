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

import org.joda.time.DateTime;
import org.opengroup.osdu.core.aws.mongodb.MongoDBMultiClusterFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.SchemaDBIndexFields;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models.SchemaInfoDto;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * The type Aws schema info store mongo db.
 */
@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "mongodb")
@Repository
public class MongoDBSchemaInfoStore implements ISchemaInfoStore {

    /**
     * The constant SCHEMA_INFO_PREFIX.
     */
    public static final String SCHEMA_INFO_PREFIX = "SchemaInfo-";
    @Inject
    private DpsHeaders headers;

    @Inject
    private JaxRsDpsLog log;

    @Inject
    private ISchemaStore schemaStore;

    @Inject
    private MongoDBMultiClusterFactory mongoDBMultiClusterFactory;

    @Inject
    private IndexUpdater indexUpdater;

    @Value("${shared.tenant.name:common}")
    private String sharedTenant;

    private String getDataPartitionId() {
        this.log.warning("TenantInfo found to be null, defaulting to partition id from headers");
        return this.headers.getPartitionId();
    }


    /**
     * Update schemaInfo.
     *
     * @param schema the SchemaRequest
     * @return the SchemaInfo
     */
    @Override
    public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        SchemaInfo schemaInfo = schema.getSchemaInfo();
        schemaInfo.setCreatedBy(headers.getUserEmail());
        schemaInfo.setDateCreated(DateTime.now().toDate());
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        String id = schemaInfo.getSchemaIdentity().getId();
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(schemaInfo);

        validateSupersededById(schemaInfo.getSupersededBy());
        String dataPartitionId = getDataPartitionId();
        Query query = Query.query(Criteria.where(SchemaDBIndexFields.ID).is(id));
        try {
            SchemaInfoDto replacedSchemaInfoDto = mongoDBMultiClusterFactory.getHelper(dataPartitionId).findAndReplace(query, schemaInfoDto, getCollection(dataPartitionId));
            if (replacedSchemaInfoDto == null) {
                throw new BadRequestException();
            }
        } catch (BadRequestException e) {
            log.error(MessageFormat.format(SchemaConstants.SCHEMA_NOT_PRESENT, e.getMessage()));
            throw new BadRequestException(SchemaConstants.SCHEMA_NOT_PRESENT);
        } catch (Exception ex) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
            throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
        }
        return schemaInfoDto.getData();
    }

    /**
     * Update system schemaInfo.
     *
     * @param schema the SchemaRequest
     * @return the SchemaInfo
     */
    @Override
    public SchemaInfo updateSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        updateDataPartitionId();
        return this.updateSchemaInfo(schema);
    }

    /**
     * Create schemaInfo.
     *
     * @param schema the SchemaRequest
     * @return the SchemaInfo
     */
    @Override
    public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        SchemaInfo schemaInfo = schema.getSchemaInfo();
        schemaInfo.setCreatedBy(headers.getUserEmail());
        schemaInfo.setDateCreated(DateTime.now().toDate());
        SchemaInfoDto schemaInfoDto = new SchemaInfoDto();
        String id = schemaInfo.getSchemaIdentity().getId();
        if (!createSchemaId(schemaInfo.getSchemaIdentity()).equals(id)) {
            throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
        }
        schemaInfoDto.setId(id);
        schemaInfoDto.setData(schemaInfo);

        validateSupersededById(schemaInfo.getSupersededBy());
        try {
            String dataPartitionId = getDataPartitionId();
            mongoDBMultiClusterFactory.getHelper(dataPartitionId).insert(schemaInfoDto, getCollection(dataPartitionId));
        } catch (DuplicateKeyException exception) {
            throw new BadRequestException(
                    MessageFormat.format(SchemaConstants.SCHEMA_ID_EXISTS, schemaInfoDto.getId()));
        } catch (Exception ex) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
            throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
        }
        return schemaInfo;
    }

    /**
     * Create system schemaInfo.
     *
     * @param schema the SchemaRequest
     * @return the SchemaInfo
     */
    @Override
    public SchemaInfo createSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        this.updateDataPartitionId();
        return this.createSchemaInfo(schema);
    }

    /**
     * Get schemaInfo.
     *
     * @param schemaId the id of schemaInfo
     * @return the SchemaInfo
     */
    @Override
    public SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {

        String dataPartitionId = getDataPartitionId();
        SchemaInfoDto schemaInfoDto;
        try {
            schemaInfoDto = mongoDBMultiClusterFactory.getHelper(dataPartitionId).getById(schemaId, SchemaInfoDto.class, getCollection(dataPartitionId));

        } catch (Exception ex) {
            log.error("Unable to delete schema info", ex);
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
        if (schemaInfoDto == null) {
            throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
        }
        return schemaInfoDto.getData();
    }

    /**
     * Get system schemaInfo.
     *
     * @param schemaId the id of schemaInfo
     * @return the SchemaInfo
     */
    @Override
    public SchemaInfo getSystemSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
        this.updateDataPartitionId();
        return this.getSchemaInfo(schemaId);
    }

    /**
     * Get last minor version schema.
     *
     * @param schemaInfo the schemaInfo
     * @return the string
     */
    @Override
    public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {

        // todo not used for now
        SchemaIdentity schemaIdentity = schemaInfo.getSchemaIdentity();

        AggregationOperation match = Aggregation.match(
                Criteria.where(SchemaDBIndexFields.AUTHORITY).is(schemaIdentity.getAuthority())
                        .and(SchemaDBIndexFields.ENTITY_TYPE).is(schemaIdentity.getEntityType())
                        .and(SchemaDBIndexFields.SOURCE).is(schemaIdentity.getSource())
                        .and(SchemaDBIndexFields.MAJOR_VERSION).is(schemaIdentity.getSchemaVersionMajor())
        );

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, SchemaDBIndexFields.MINOR_VERSION);
        LimitOperation limit = Aggregation.limit(1);
        Aggregation aggregation = Aggregation.newAggregation(match, sort, limit);
        String dataPartitionId = getDataPartitionId();
        AggregationResults<SchemaInfoDto> results = mongoDBMultiClusterFactory.getHelper(dataPartitionId).pipeline(aggregation,
                getCollection(dataPartitionId),
                SchemaInfoDto.class);
        Optional<SchemaInfoDto> first = results.getMappedResults().stream().findFirst();
        String schemaString = null;
        if (first.isPresent()) {
            SchemaInfoDto schemaInfoDto = first.get();
            try {
                schemaString = schemaStore.getSchema(dataPartitionId, schemaInfoDto.getId());
            } catch (NotFoundException e) {
                return schemaString;
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
            }
        }
        return schemaString;

    }

    /**
     * Get collection.
     *
     * @param queryParams search params
     * @param tenantId    the dataPartition
     * @return the collection
     */
    @Override
    public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException {
        Criteria criteria = getCriteria(queryParams);
        AggregationOperation match = Aggregation.match(criteria);
        Aggregation aggregation;
        if (Boolean.TRUE.equals(queryParams.getLatestVersion())) {
            SortOperation sortPatch = Aggregation.sort(Sort.Direction.DESC, SchemaDBIndexFields.PATCH_VERSION);
            SortOperation sortMinor = Aggregation.sort(Sort.Direction.DESC, SchemaDBIndexFields.MINOR_VERSION);
            SortOperation sortMajor = Aggregation.sort(Sort.Direction.DESC, SchemaDBIndexFields.MAJOR_VERSION);
            LimitOperation limit = Aggregation.limit(1);
            aggregation = Aggregation.newAggregation(match, sortPatch, sortMinor, sortMajor, limit);
        } else {
            aggregation = Aggregation.newAggregation(match);
        }
        try {
            AggregationResults<SchemaInfoDto> results = mongoDBMultiClusterFactory.getHelper(tenantId).pipeline(aggregation,
                    getCollection(tenantId),
                    SchemaInfoDto.class);

            return results.getMappedResults().stream()
                    .map(SchemaInfoDto::getData)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get collection.
     *
     * @param queryParams search params
     * @return the collection
     */
    @Override
    public List<SchemaInfo> getSystemSchemaInfoList(QueryParams queryParams) throws ApplicationException {
        return this.getSchemaInfoList(queryParams, sharedTenant);
    }

    /**
     * check schemaInfo uniqueness by id.
     *
     * @param schemaId schemaInfoId
     * @param tenantId the dataPartition
     * @return the boolean
     */
    @Override
    public boolean isUnique(String schemaId, String tenantId) throws ApplicationException {
        try {
            return !mongoDBMultiClusterFactory.getHelper(tenantId).existsByField(SchemaDBIndexFields.ID, schemaId, SchemaInfoDto.class, getCollection(tenantId));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * check system schemaInfo uniqueness by id.
     *
     * @param schemaId schemaInfoId
     * @return the boolean
     */
    @Override
    public boolean isUniqueSystemSchema(String schemaId) throws ApplicationException {
        return this.isUnique(schemaId, sharedTenant);
    }


    /**
     * remove schemaInfo by id.
     *
     * @param schemaId schemaInfoId
     * @return the boolean
     */
    @Override
    public boolean cleanSchema(String schemaId) throws ApplicationException {
        String dataPartitionId = getDataPartitionId();
        try {
            return mongoDBMultiClusterFactory.getHelper(dataPartitionId).delete(SchemaDBIndexFields.ID, schemaId, getCollection(dataPartitionId));
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * remove system schemaInfo by id.
     *
     * @param schemaId schemaInfoId
     * @return the boolean
     */
    @Override
    public boolean cleanSystemSchema(String schemaId) throws ApplicationException {
        updateDataPartitionId();
        return this.cleanSchema(schemaId);
    }

    private Criteria getCriteria(QueryParams queryParams) {
        Criteria criteria = new Criteria();
        if (Objects.nonNull(queryParams.getAuthority())) {
            criteria.and(SchemaDBIndexFields.AUTHORITY).is(queryParams.getAuthority());
        }
        if (Objects.nonNull(queryParams.getSource())) {
            criteria.and(SchemaDBIndexFields.SOURCE).is(queryParams.getSource());
        }
        if (Objects.nonNull(queryParams.getEntityType())) {
            criteria.and(SchemaDBIndexFields.ENTITY_TYPE).is(queryParams.getEntityType());
        }
        if (Objects.nonNull(queryParams.getSchemaVersionMajor())) {
            criteria.and(SchemaDBIndexFields.MAJOR_VERSION).is(queryParams.getSchemaVersionMajor());
        }
        if (Objects.nonNull(queryParams.getSchemaVersionMinor())) {
            criteria.and(SchemaDBIndexFields.MINOR_VERSION).is(queryParams.getSchemaVersionMinor());
        }
        if (Objects.nonNull(queryParams.getSchemaVersionPatch())) {
            criteria.and(SchemaDBIndexFields.PATCH_VERSION).is(queryParams.getSchemaVersionPatch());
        }
        if (Objects.nonNull(queryParams.getScope())) {
            criteria.and(SchemaDBIndexFields.SCOPE).is(queryParams.getScope());
        }
        if (Objects.nonNull(queryParams.getStatus())) {
            criteria.and(SchemaDBIndexFields.STATUS).is(queryParams.getStatus());
        }
        return criteria;
    }

    private void validateSupersededById(SchemaIdentity superseding_schema) throws BadRequestException {
        if (superseding_schema != null) {
            String id = superseding_schema.getId();
            String dataPartitionId = getDataPartitionId();
            if (!mongoDBMultiClusterFactory.getHelper(dataPartitionId).existsByField(SchemaDBIndexFields.ID, id, SchemaInfoDto.class, getCollection(dataPartitionId))) {
                throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
            }
        }
    }

    public static String createSchemaId(SchemaIdentity schemaIdentity) {
        return new StringBuilder().append(schemaIdentity.getAuthority())
                .append(":").append(schemaIdentity.getSource())
                .append(":").append(schemaIdentity.getEntityType()).append(":")
                .append(schemaIdentity.getSchemaVersionMajor()).append(".")
                .append(schemaIdentity.getSchemaVersionMinor()).append(".")
                .append(schemaIdentity.getSchemaVersionPatch()).toString();
    }

    private void updateDataPartitionId() {
        headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
    }

    private String getCollection(String dataPartitionId) {
        indexUpdater.checkIndex(dataPartitionId);
        return SCHEMA_INFO_PREFIX + dataPartitionId;
    }
}
