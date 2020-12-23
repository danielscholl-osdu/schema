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

import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.google.gson.Gson;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.azure.definitions.FlattenedSchemaInfo;
import org.opengroup.osdu.schema.azure.definitions.SchemaInfoDoc;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.util.VersionHierarchyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import lombok.extern.java.Log;

/**
 * Repository class to to register Schema in Azure store.
 *
 */
@Log
@Repository
public class AzureSchemaInfoStore implements ISchemaInfoStore {
    @Autowired
    private ITenantFactory tenantFactory;

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private String schemaInfoContainer;

    @Autowired
    private CosmosStore cosmosStore;

    @Autowired
    private String cosmosDBName;
    
    @Value("${shared.tenant.name:common}")
	private String sharedTenant;

    @Autowired
    JaxRsDpsLog log;

    /**
     * Method to get schemaInfo from azure store
     *
     * @param schemaId
     * @return schemaInfo object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    @Override
    public SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {

        String id = headers.getPartitionId() + ":" + schemaId;

        SchemaInfoDoc schemaInfoDoc = cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, headers.getPartitionId(), SchemaInfoDoc.class)
                .orElseThrow(() -> new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT));

        return getSchemaInfoObject(schemaInfoDoc.getFlattenedSchemaInfo());
    }

    /**
     * Method to Create schema in azure store
     *
     * @param schema
     * @return
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        String id = headers.getPartitionId() + ":" + schema.getSchemaInfo().getSchemaIdentity().getId();
        FlattenedSchemaInfo flattenedSchemaInfo = populateSchemaInfo(schema);
        SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc(id, headers.getPartitionId(), flattenedSchemaInfo);
        try {
            cosmosStore.createItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, headers.getPartitionId(), schemaInfoDoc);
        } catch (AppException ex) {
            if (ex.getError().getCode() == 409) {
                log.warning(SchemaConstants.SCHEMA_ID_EXISTS);
                throw new BadRequestException(SchemaConstants.SCHEMA_ID_EXISTS);
            } else {
                log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
                throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
            }
        }

        log.info(SchemaConstants.SCHEMA_INFO_CREATED);
        return getSchemaInfoObject(flattenedSchemaInfo);
    }

    /**
     * Method to update schema in azure store
     *
     * @param schema
     * @return
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        String id = headers.getPartitionId() + ":" + schema.getSchemaInfo().getSchemaIdentity().getId();
        FlattenedSchemaInfo flattenedSchemaInfo = populateSchemaInfo(schema);
        SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc(id, headers.getPartitionId(), flattenedSchemaInfo);
        try {
            cosmosStore.upsertItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, headers.getPartitionId(), schemaInfoDoc);
        } catch (Exception ex) {
            log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
            throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
        }

        log.info(SchemaConstants.SCHEMA_INFO_UPDATED);
        return getSchemaInfoObject(flattenedSchemaInfo);
    }

    /**
     * Method to clean schemaInfo in azure store
     *
     * @param schemaId
     * @return status
     * @throws ApplicationException
     */
    @Override
    public boolean cleanSchema(String schemaId) throws ApplicationException {
        String id = headers.getPartitionId() + ":" + schemaId;

        // Check whether SchemaInfo already exists
        Boolean exists = cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, headers.getPartitionId(), SchemaInfoDoc.class).isPresent();
        if (!exists) {
            return false;
        }

        // Delete the item.
        cosmosStore.deleteItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, headers.getPartitionId());
        return true;
    }

    @Override
    public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {

        SqlQuerySpec query = new SqlQuerySpec("SELECT * FROM c WHERE c.dataPartitionId = @partitionId" +
                " AND c.flattenedSchemaInfo.authority = @authority" +
                " AND c.flattenedSchemaInfo.source = @source" +
                " AND c.flattenedSchemaInfo.entityType = @entityType" +
                " AND c.flattenedSchemaInfo.majorVersion = @majorVersion");
        List<SqlParameter> sqlParameterList = query.getParameters();
        sqlParameterList.add(new SqlParameter("@partitionId", headers.getPartitionId()));
        sqlParameterList.add(new SqlParameter("@authority", schemaInfo.getSchemaIdentity().getAuthority()));
        sqlParameterList.add(new SqlParameter("@source", schemaInfo.getSchemaIdentity().getSource()));
        sqlParameterList.add(new SqlParameter("@entityType", schemaInfo.getSchemaIdentity().getEntityType()));
        sqlParameterList.add(new SqlParameter("@majorVersion", schemaInfo.getSchemaIdentity().getSchemaVersionMajor()));

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        List<SchemaInfoDoc> schemaInfoList = cosmosStore.queryItems(headers.getPartitionId(), cosmosDBName,schemaInfoContainer, query, options, SchemaInfoDoc.class);

        TreeMap<Long, String> sortedMap = new TreeMap<>(Collections.reverseOrder());
        for (SchemaInfoDoc info : schemaInfoList)
        {
            sortedMap.put(info.getFlattenedSchemaInfo().getMinorVersion(), info.getFlattenedSchemaInfo().getSchema());
        }

        if (sortedMap.size() != 0) {
            Entry<Long, String> entry = sortedMap.firstEntry();
            return entry.getValue();
        }
        return new String();
    }

    /**
     * Creates schemaInfo object and populates required properties.
     *
     * @param schema
     * @return
     */
    private FlattenedSchemaInfo populateSchemaInfo(SchemaRequest schemaRequest)
            throws BadRequestException {
        SchemaInfo schemaInfo = schemaRequest.getSchemaInfo();
        // check for super-seeding schemas
        String supersededById = "";
        if (schemaInfo.getSupersededBy() != null) {
            String id = headers.getPartitionId() + ":" + schemaInfo.getSupersededBy().getId();

            if (schemaInfo.getSupersededBy().getId() == null
                    || !cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, headers.getPartitionId(), FlattenedSchemaInfo.class).isPresent()) {
                log.error(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
                throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
            }
            supersededById = schemaInfo.getSupersededBy().getId();
        }

        Gson gson = new Gson();
        return FlattenedSchemaInfo.builder().id(schemaInfo.getSchemaIdentity().getId())
                .supersededBy(supersededById)
                .dateCreated(new Date())
                .createdBy(headers.getUserEmail())
                .authority(schemaInfo.getSchemaIdentity().getAuthority())
                .source(schemaInfo.getSchemaIdentity().getSource())
                .entityType(schemaInfo.getSchemaIdentity().getEntityType())
                .majorVersion(schemaInfo.getSchemaIdentity().getSchemaVersionMajor())
                .minorVersion(schemaInfo.getSchemaIdentity().getSchemaVersionMinor())
                .patchVersion(schemaInfo.getSchemaIdentity().getSchemaVersionPatch())
                .scope(schemaInfo.getScope().name())
                .status(schemaInfo.getStatus().name())
                .schema(gson.toJson(schemaRequest.getSchema()))
                .build();
    }

    private SchemaInfo getSchemaInfoObject(FlattenedSchemaInfo flattenedSchemaInfo) {
        SchemaIdentity superSededBy = null;
        if (!flattenedSchemaInfo.getSupersededBy().isEmpty()) {
            String id = headers.getPartitionId() + ":" + flattenedSchemaInfo.getSupersededBy();
            SchemaInfoDoc doc = cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, headers.getPartitionId(), SchemaInfoDoc.class).get();
            superSededBy = getSchemaIdentity(doc.getFlattenedSchemaInfo());
        }

        return SchemaInfo.builder().createdBy(flattenedSchemaInfo.getCreatedBy())
                .dateCreated(flattenedSchemaInfo.getDateCreated())
                .scope(SchemaScope.valueOf(flattenedSchemaInfo.getScope()))
                .status(SchemaStatus.valueOf(flattenedSchemaInfo.getStatus()))
                .schemaIdentity(getSchemaIdentity(flattenedSchemaInfo)).supersededBy(superSededBy).build();
    }

    private SchemaIdentity getSchemaIdentity(FlattenedSchemaInfo flattenedSchemaInfo) {

        return SchemaIdentity.builder().id(flattenedSchemaInfo.getId())
                .authority(flattenedSchemaInfo.getAuthority())
                .source(flattenedSchemaInfo.getSource())
                .entityType(flattenedSchemaInfo.getEntityType())
                .schemaVersionMajor(flattenedSchemaInfo.getMajorVersion())
                .schemaVersionMinor(flattenedSchemaInfo.getMinorVersion())
                .schemaVersionPatch(flattenedSchemaInfo.getPatchVersion()).build();
    }

    @Override
    public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException {
        String queryText = "SELECT * FROM c WHERE c.dataPartitionId = @partitionId";
        HashMap<String, Object> parameterMap = new HashMap<>();
        // Populate the implicit partitionId parameter
        parameterMap.put("@partitionId", tenantId);

        // Now update the query text according to the clauses present
        // Update the parameter hash accordingly.
        if (queryParams.getAuthority() != null) {
            queryText = queryText + " AND c.flattenedSchemaInfo.authority = @authority";
            parameterMap.put("@authority", queryParams.getAuthority());
        }
        if (queryParams.getSource() != null) {
            queryText = queryText + " AND c.flattenedSchemaInfo.source = @source";
            parameterMap.put("@source", queryParams.getSource());
        }
        if (queryParams.getEntityType() != null) {
            queryText = queryText + " AND c.flattenedSchemaInfo.entityType = @entityType";
            parameterMap.put("@entityType", queryParams.getEntityType());
        }
        if (queryParams.getSchemaVersionMajor() != null) {
            queryText = queryText + " AND c.flattenedSchemaInfo.majorVersion = @majorVersion";
            parameterMap.put("@majorVersion", queryParams.getSchemaVersionMajor());
        }
        if (queryParams.getSchemaVersionMinor() != null) {
            queryText = queryText + " AND c.flattenedSchemaInfo.minorVersion = @minorVersion";
            parameterMap.put("@minorVersion", queryParams.getSchemaVersionMinor());
        }
        if (queryParams.getSchemaVersionPatch() != null) {
            queryText = queryText + " AND c.flattenedSchemaInfo.patchVersion = @patchVersion";
            parameterMap.put("@patchVersion", queryParams.getSchemaVersionPatch());
        }
        if (queryParams.getStatus() != null) {
            queryText = queryText + " AND c.flattenedSchemaInfo.status = @status";
            parameterMap.put("@status", queryParams.getStatus());
        }

        SqlQuerySpec query = new SqlQuerySpec(queryText);
        for (String param : parameterMap.keySet())
        {
            query.getParameters().add(new SqlParameter(param, parameterMap.get(param)));
        }

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        List<SchemaInfoDoc> schemaInfoList = cosmosStore.queryItems(headers.getPartitionId(), cosmosDBName,schemaInfoContainer, query, options, SchemaInfoDoc.class);

        List<SchemaInfo> schemaList = new LinkedList<>();
        for (SchemaInfoDoc info: schemaInfoList)
        {
            schemaList.add(getSchemaInfoObject(info.getFlattenedSchemaInfo()));
        }

        if (queryParams.getLatestVersion() != null && queryParams.getLatestVersion()) {
            return getLatestVersionSchemaList(schemaList);
        }

        return schemaList;
    }

    @Override
    public boolean isUnique(String schemaId, String tenantId) throws ApplicationException {
        Set<String> tenantList = new HashSet<>();
        tenantList.add(sharedTenant);
        tenantList.add(tenantId);

        /* TODO : Below code enables uniqueness check across tenants and is redundant now. This will be handled/updated as part
            of data partition changes.
         */
        if (tenantId.equalsIgnoreCase(sharedTenant)) {
            List<String> privateTenantList = tenantFactory.listTenantInfo().stream().map(TenantInfo::getName)
                    .collect(Collectors.toList());
            tenantList.addAll(privateTenantList);
        }

        for (String tenant : tenantList) {
            String id = tenant + ":" + schemaId;
            Boolean exists = cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, tenant, SchemaInfoDoc.class).isPresent();
            if (exists) {
                return false;
            }
        }
        return true;
    }

    private List<SchemaInfo> getLatestVersionSchemaList(List<SchemaInfo> filteredSchemaList) {
        List<SchemaInfo> latestSchemaList = new LinkedList<>();
        SchemaInfo previousSchemaInfo = null;
        TreeMap<VersionHierarchyUtil, SchemaInfo> sortedMap = new TreeMap<>(
                new VersionHierarchyUtil.SortingVersionComparator());

        for (SchemaInfo schemaInfoObject : filteredSchemaList) {
            if ((previousSchemaInfo != null) && !(checkAuthorityMatch(previousSchemaInfo, schemaInfoObject)
                    && checkSourceMatch(previousSchemaInfo, schemaInfoObject)
                    && checkEntityMatch(previousSchemaInfo, schemaInfoObject))) {
                Entry<VersionHierarchyUtil, SchemaInfo> latestVersionEntry = sortedMap.firstEntry();
                latestSchemaList.add(latestVersionEntry.getValue());
                sortedMap.clear();
            }
            previousSchemaInfo = schemaInfoObject;
            SchemaIdentity schemaIdentity = schemaInfoObject.getSchemaIdentity();
            VersionHierarchyUtil version = new VersionHierarchyUtil(schemaIdentity.getSchemaVersionMajor(),
                    schemaIdentity.getSchemaVersionMinor(), schemaIdentity.getSchemaVersionPatch());
            sortedMap.put(version, schemaInfoObject);
        }
        if (sortedMap.size() != 0) {
            Entry<VersionHierarchyUtil, SchemaInfo> latestVersionEntry = sortedMap.firstEntry();
            latestSchemaList.add(latestVersionEntry.getValue());
        }

        return latestSchemaList;
    }

    private boolean checkEntityMatch(SchemaInfo previousSchemaInfo, SchemaInfo schemaInfoObject) {
        return schemaInfoObject.getSchemaIdentity().getEntityType()
                .equalsIgnoreCase(previousSchemaInfo.getSchemaIdentity().getEntityType());
    }

    private boolean checkSourceMatch(SchemaInfo previousSchemaInfo, SchemaInfo schemaInfoObject) {
        return schemaInfoObject.getSchemaIdentity().getSource()
                .equalsIgnoreCase(previousSchemaInfo.getSchemaIdentity().getSource());
    }

    private boolean checkAuthorityMatch(SchemaInfo previousSchemaInfo, SchemaInfo schemaInfoObject) {
        return schemaInfoObject.getSchemaIdentity().getAuthority()
                .equalsIgnoreCase(previousSchemaInfo.getSchemaIdentity().getAuthority());
    }
}

