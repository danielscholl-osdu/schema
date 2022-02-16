/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.schema.impl.schemainfostore;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.osm.model.Destination;
import org.opengroup.osdu.core.gcp.osm.model.query.GetQuery;
import org.opengroup.osdu.core.gcp.osm.model.where.Where;
import org.opengroup.osdu.core.gcp.osm.model.where.predicate.Eq;
import org.opengroup.osdu.core.gcp.osm.service.Context;
import org.opengroup.osdu.core.gcp.osm.translate.TranslatorException;
import org.opengroup.osdu.core.gcp.osm.translate.TranslatorRuntimeException;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.opengroup.osdu.core.gcp.osm.model.where.condition.And.and;
import static org.opengroup.osdu.core.gcp.osm.model.where.predicate.Eq.eq;
/**
 * Repository class to register Schema in KV store.
 *
 */

@Repository
public class OsmSchemaInfoStore extends AbstractOsmRepository<SchemaRequest> implements ISchemaInfoStore {

    private static final String SCHEMA_INFO_PREFIX = "schemaInfo.%s";
    private static final String SCHEMA_IDENTITY_PREFIX = String.format(SCHEMA_INFO_PREFIX, "schemaIdentity.%s");
    private static final String SCHEMA_ID = String.format(SCHEMA_IDENTITY_PREFIX, "id");
    private static final String SCHEMA_OSM_KIND = String.format("%s-osm", SchemaConstants.SCHEMA_KIND);

    static {
        ENTITY_CREATED = SchemaConstants.SCHEMA_INFO_CREATED;
    }

    private final ITenantFactory tenantFactory;

    @Autowired
    public OsmSchemaInfoStore(DpsHeaders headers, DestinationProvider<Destination> destinationProvider, JaxRsDpsLog log, Context context, ITenantFactory tenantFactory) {
        super(headers, destinationProvider, log, context);
        this.tenantFactory = tenantFactory;
    }

    public String getSharedTenant(){
        return sharedTenant;
    }

    public void setSharedTenant(String sharedTenant){
        this.sharedTenant = sharedTenant;
    }

    /**
     * Method to get schemaInfo from KV store
     *
     * @param schemaId
     * @return schemaInfo object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    @Override
    public SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
            SchemaRequest schemaRequest = context.findOne(buildQueryFor(getDestination(), eq(SCHEMA_ID, schemaId)))
                    .orElseThrow(() ->
                            new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT));
            return schemaRequest.getSchemaInfo();
    }

    /**
     * Method to get System schemaInfo from KV store
     * @param schemaId
     * @return schemaInfo object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    @Override
    public SchemaInfo getSystemSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
        try {
            return this.getSystemEntity(schemaId).getSchemaInfo();
        } catch (NotFoundException e){
            throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
        }
    }

    /**
     * Method to Create schema in KV store of tenantId GCP
     *
     * @param schema
     * @return schemaInfo object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        try {
            enrichSchemaInfo(schema.getSchemaInfo());
            return this.create(schema).getSchemaInfo();
        } catch (ApplicationException e){
            throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
        }
    }

    /**
     * Method to Create System schema in google store
     * @param schema
     * @return SchemaInfo object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public SchemaInfo createSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        this.updateDataPartitionId();
        return this.createSchemaInfo(schema);
    }

    /**
     * Method to update schema in KV store of tenantId GCP
     *
     * @param schema
     * @return schemaInfo object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        enrichSchemaInfo(schema.getSchemaInfo());

        SchemaRequest entityFromDb = null;
        try {
            entityFromDb = context.upsertAndGet(schema, getDestination());
        } catch (TranslatorRuntimeException ex) {
            log.error(SchemaConstants.OBJECT_INVALID);
            throw new ApplicationException("Invalid object, update failed");
        }
        log.info(SchemaConstants.SCHEMA_INFO_UPDATED);
        return entityFromDb.getSchemaInfo();
    }

    /**
     * Method to update System schema in KV store
     * @param schema
     * @return SchemaInfo object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public SchemaInfo updateSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        this.updateDataPartitionId();
        return this.updateSchemaInfo(schema);
    }

    /**
     * Method to clean schemaInfo in google datastore of tenantId GCP
     *
     * @param schemaId
     * @return status
     * @throws ApplicationException
     */
    @Override
    public boolean cleanSchema(String schemaId) throws ApplicationException {
        try {
            context.delete(SchemaRequest.class, getDestination(), eq(SCHEMA_ID, schemaId));
            return true;
        } catch (TranslatorException ex) {
            return false;
        }
    }

    /**
     * Method to clean System schemaInfo in google datastore
     * @param schemaId
     * @return status
     * @throws ApplicationException
     */
    @Override
    public boolean cleanSystemSchema(String schemaId) throws ApplicationException {
        this.updateDataPartitionId();
        return this.cleanSchema(schemaId);
    }

    @Override
    public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {
        GetQuery<SchemaRequest> getQuery = new GetQuery<>(SchemaRequest.class, getDestination(),
                and(
                        eq(String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.AUTHORITY), schemaInfo.getSchemaIdentity().getAuthority()),
                        eq(String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.ENTITY_TYPE), schemaInfo.getSchemaIdentity().getEntityType()),
                        eq(String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.MAJOR_VERSION), schemaInfo.getSchemaIdentity().getSchemaVersionMajor()),
                        eq(String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.SOURCE), schemaInfo.getSchemaIdentity().getSource())
                ));

        List<SchemaRequest> results = context.getResultsAsList(getQuery);

        Iterator<SchemaRequest> result = results.iterator();

        TreeMap<Long, Object> sortedMap = new TreeMap<>(Collections.reverseOrder());
        while (result.hasNext()) {
            SchemaRequest entity = result.next();
            sortedMap.put(
                    entity.getSchemaInfo().getSchemaIdentity().getSchemaVersionMinor(),
                    entity.getSchema());
        }
        if (sortedMap.size() != 0) {
            Entry<Long, Object> entry = sortedMap.firstEntry();
            return String.valueOf(entry.getValue());
        }
        return "";
    }

    @Override
    public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException {
        List<SchemaInfo> schemaList = new LinkedList<>();
        List<Eq> filterList = getFilters(queryParams);

        GetQuery<SchemaRequest>.GetQueryBuilder<SchemaRequest> queryBuilder = new GetQuery<>(SchemaRequest.class, getDestination()).builder() ;
        if (!filterList.isEmpty()) {
            queryBuilder.where(buildFiltersFromList(filterList));
        }
        for (SchemaRequest entity : context.getResultsAsList(queryBuilder.build())) {
            schemaList.add(entity.getSchemaInfo());
        }

        return schemaList;
    }

    /**
     * Get schema info list for system schemas
     * @param queryParams
     * @return
     * @throws ApplicationException
     */
    @Override
    public List<SchemaInfo> getSystemSchemaInfoList(QueryParams queryParams) throws ApplicationException {
        return this.getSchemaInfoList(queryParams, sharedTenant);
    }

    private Where buildFiltersFromList(List<Eq> filters){
        return filters.size() > 1
                ? and(filters.get(0), filters.get(1), filters.toArray(filters.toArray(new Where[0])))
                : filters.get(0);

    }

    private void enrichSchemaInfo(SchemaInfo schema) throws BadRequestException {
        if (schema.getSupersededBy() != null) {
            Optional<SchemaRequest> superseded =
                    context.findOne(buildQueryFor(getDestination(), eq(SCHEMA_ID, schema.getSupersededBy().getId())));

            if (schema.getSupersededBy().getId() == null
                    || !superseded.isPresent()) {
                log.error(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
                throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
            }
        }

        schema.setDateCreated(Date.from(Instant.now()));
        schema.setCreatedBy(headers.getUserEmail());
    }

    private List<Eq> getFilters(QueryParams queryParams) {
        List<Eq> filterList = new LinkedList<>();
        if (queryParams.getAuthority() != null) {
            filterList.add(eq(
                    String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.AUTHORITY),
                    queryParams.getAuthority()));
        }
        if (queryParams.getSource() != null) {
            filterList.add(eq(
                    String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.SOURCE),
                    queryParams.getSource()));
        }
        if (queryParams.getEntityType() != null) {
            filterList.add(eq(
                    String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.ENTITY_TYPE),
                    queryParams.getEntityType()));
        }
        if (queryParams.getSchemaVersionMajor() != null) {
            filterList.add(eq(
                    String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.MAJOR_VERSION),
                    queryParams.getSchemaVersionMajor()));
        }
        if (queryParams.getSchemaVersionMinor() != null) {
            filterList.add(eq(
                    String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.MINOR_VERSION),
                    queryParams.getSchemaVersionMinor()));
        }
        if (queryParams.getSchemaVersionPatch() != null) {
            filterList.add(eq(
                    String.format(SCHEMA_IDENTITY_PREFIX, SchemaConstants.PATCH_VERSION),
                    queryParams.getSchemaVersionPatch()));
        }
        if (queryParams.getStatus() != null) {
            filterList.add(eq(
                    String.format(SCHEMA_INFO_PREFIX, SchemaConstants.STATUS),
                    queryParams.getStatus().toUpperCase()));
        }
        return filterList;
    }

    @Override
    public boolean isUnique(String schemaId, String tenantId) {

        Set<String> tenantList = new HashSet<>();
        tenantList.add(sharedTenant);
        tenantList.add(tenantId);

        // code to call check uniqueness
        if (tenantId.equalsIgnoreCase(sharedTenant)) {
            List<String> privateTenantList = tenantFactory.listTenantInfo().stream().map(TenantInfo::getDataPartitionId)
                    .collect(Collectors.toList());
            tenantList.addAll(privateTenantList);

        }
        for (String tenant : tenantList) {
            GetQuery<SchemaRequest> query = buildQueryFor(getDestination(), eq(SCHEMA_ID, schemaId));

            try {
                List<SchemaRequest> schemas = context.getResultsAsList(query);
                if (schemas.size() > 1) {
                    return false;
                }
            } catch (TranslatorRuntimeException e) {
                throw new AppException(HttpStatus.SC_BAD_REQUEST, "Schema uniqueness check failed",
                    String.format("Misconfigured tenant-info for %s, not possible to check schema uniqueness", tenant));
            }
        }
        return true;
    }

    /**
     * Method to check whether given system schema id is unique or not
     * @param schemaId
     * @return
     * @throws ApplicationException
     */
    @Override
    public boolean isUniqueSystemSchema(String schemaId) {
        return this.isUnique(schemaId, sharedTenant);
    }

    @Override
    protected Destination getDestination() {
        return destinationProvider.getDestination(
                headers.getPartitionId(),
                SchemaConstants.NAMESPACE,
                SCHEMA_OSM_KIND
        );
    }

    @Override
    protected GetQuery<SchemaRequest> buildQueryFor(Destination destination, Where where) {
        return new GetQuery<>(SchemaRequest.class, destination, where);
    }

    @Override
    protected void checkEntityExistence(SchemaRequest entity) throws BadRequestException {
        SchemaRequest entityFromDb =  context.getOne(buildQueryFor(getDestination(),
                eq(SCHEMA_ID, entity.getSchemaInfo().getSchemaIdentity().getId())));
        if (ObjectUtils.isNotEmpty(entityFromDb)) {
            log.warning(SchemaConstants.SCHEMA_CREATION_FAILED);
            throw new BadRequestException(MessageFormat.format(SchemaConstants.SCHEMA_ID_EXISTS,
                    entity.getSchemaInfo().getSchemaIdentity().getId()));
        }
    }

}