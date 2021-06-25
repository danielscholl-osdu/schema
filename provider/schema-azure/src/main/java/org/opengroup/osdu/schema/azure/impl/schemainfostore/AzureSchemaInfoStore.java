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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.google.gson.Gson;

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

		SchemaIdentity  schemaIdentity = schemaKindToSchemaIdentity(schemaId);
		String partitioningKey =  createSchemaInfoPartitionKey(schemaIdentity);

		SchemaInfoDoc schemaInfoDoc = cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, partitioningKey, SchemaInfoDoc.class)
                .orElseThrow(() -> new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT));
		
		return getSchemaInfoObject(schemaInfoDoc.getFlattenedSchemaInfo(), headers.getPartitionId());
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
		String partitionKey = createSchemaInfoPartitionKey(schema.getSchemaInfo().getSchemaIdentity());
		SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc(id, partitionKey, flattenedSchemaInfo);
		try {
			cosmosStore.createItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, partitionKey, schemaInfoDoc);
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
		return getSchemaInfoObject(flattenedSchemaInfo, headers.getPartitionId());
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
		String partitionKey = createSchemaInfoPartitionKey(schema.getSchemaInfo().getSchemaIdentity());
		SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc(id, partitionKey, flattenedSchemaInfo);
		try {
			cosmosStore.upsertItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, partitionKey,schemaInfoDoc);
		} catch (Exception ex) {
			log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
			throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
		}

		log.info(SchemaConstants.SCHEMA_INFO_UPDATED);
		return getSchemaInfoObject(flattenedSchemaInfo, headers.getPartitionId());
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

		SchemaIdentity  schemaIdentity = schemaKindToSchemaIdentity(schemaId);
		String partitionKey = createSchemaInfoPartitionKey(schemaIdentity);

		// Check whether SchemaInfo already exists
		Boolean exists = cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, partitionKey, SchemaInfoDoc.class).isPresent();
		if (!exists) {
			return false;
		}

		// Delete the item.
		cosmosStore.deleteItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, headers.getPartitionId());
		return true;
	}

	@Override
	public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {
		
		String partitionKey = createSchemaInfoPartitionKey(schemaInfo.getSchemaIdentity());
		StringBuffer queryBuffer = new StringBuffer("SELECT * FROM c WHERE" +
				" c.flattenedSchemaInfo.authority = @authority" +
				" AND c.flattenedSchemaInfo.source = @source" +
				" AND c.flattenedSchemaInfo.entityType = @entityType" +
				" AND c.flattenedSchemaInfo.majorVersion = @majorVersion");
		
		CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
		
		if(false == StringUtils.isBlank(partitionKey)) {
			options.setPartitionKey(new PartitionKey(partitionKey));
		}
		
		SqlQuerySpec query = new SqlQuerySpec(queryBuffer.toString());
		
		List<SqlParameter>  pars = query.getParameters();
		pars.add(new SqlParameter("@authority", schemaInfo.getSchemaIdentity().getAuthority()));
		pars.add(new SqlParameter("@source", schemaInfo.getSchemaIdentity().getSource()));
		pars.add(new SqlParameter("@entityType", schemaInfo.getSchemaIdentity().getEntityType()));
		pars.add(new SqlParameter("@majorVersion", schemaInfo.getSchemaIdentity().getSchemaVersionMajor()));
		
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

			if (schemaInfo.getSupersededBy().getId() == null)
				throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);

			SchemaIdentity schemaIdentity = schemaKindToSchemaIdentity(schemaInfo.getSupersededBy().getId());
			String partitionKey = createSchemaInfoPartitionKey(schemaIdentity);

			if ( !cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, schemaInfoContainer, id, partitionKey, FlattenedSchemaInfo.class).isPresent()) {
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

	private SchemaInfo getSchemaInfoObject(FlattenedSchemaInfo flattenedSchemaInfo, String dataPartitionId) {
		SchemaIdentity superSededBy = null;
		if (!flattenedSchemaInfo.getSupersededBy().isEmpty()) {
			String id = dataPartitionId + ":" + flattenedSchemaInfo.getSupersededBy();
			SchemaIdentity schemaIdentity = schemaKindToSchemaIdentity(flattenedSchemaInfo.getSupersededBy());
			String partitionKey = createSchemaInfoPartitionKey(schemaIdentity);
			SchemaInfoDoc doc = cosmosStore.findItem(dataPartitionId, cosmosDBName, schemaInfoContainer, id, partitionKey, SchemaInfoDoc.class).get();
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
		String queryText = "SELECT * FROM c WHERE 1=1 ";
		HashMap<String, Object> parameterMap = new HashMap<>();

		CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();

		SchemaIdentity schemaIdentity =  SchemaIdentity.builder()
				.authority(queryParams.getAuthority())
				.source(queryParams.getSource())
				.entityType(queryParams.getEntityType())
				.schemaVersionMajor(queryParams.getSchemaVersionMajor()).build();
		String partitionKeyStr = createSchemaInfoPartitionKey(schemaIdentity);

		if(false == StringUtils.isBlank(partitionKeyStr)) {
			PartitionKey partitionKey = new PartitionKey(partitionKeyStr);
			options = options.setPartitionKey(partitionKey);
		}

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

		List<SchemaInfoDoc> schemaInfoList = cosmosStore.queryItems(tenantId, cosmosDBName,schemaInfoContainer, query, options, SchemaInfoDoc.class);

		List<SchemaInfo> schemaList = new LinkedList<>();
		for (SchemaInfoDoc info: schemaInfoList)
		{
			schemaList.add(getSchemaInfoObject(info.getFlattenedSchemaInfo(), tenantId));
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
			String partitionKey = createSchemaInfoPartitionKey(schemaKindToSchemaIdentity(schemaId));
			try {
				Boolean exists = cosmosStore.findItem(tenant, cosmosDBName, schemaInfoContainer, id, partitionKey, SchemaInfoDoc.class).isPresent();
				if (exists) {
					return false;
				}
			} catch (AppException ex) {
				log.warning(String.format("Error occurred while performing uniqueness check in tenant '%s'", tenant), ex);
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

	private SchemaIdentity schemaKindToSchemaIdentity(String kind) {
		String[] schemaAttr = kind.split(":");
		String[] version = schemaAttr[3].split("\\.");
		SchemaIdentity schemaIdentity =  SchemaIdentity.builder()
				.authority(schemaAttr[0])
				.source(schemaAttr[1])
				.entityType(schemaAttr[2])
				.schemaVersionMajor(new Long(version[0]))
				.schemaVersionMinor(new Long(version[1]))
				.schemaVersionPatch(new Long(version[2])).build();

		return schemaIdentity;
	}

	private String createSchemaInfoPartitionKey(SchemaIdentity schemaIdentity) {
		
		if(StringUtils.isBlank(schemaIdentity.getAuthority())
				|| StringUtils.isBlank(schemaIdentity.getSource())
				|| StringUtils.isBlank(schemaIdentity.getEntityType())
				|| null == schemaIdentity.getSchemaVersionMajor())
			return null;


		return schemaIdentity.getAuthority()+
				":"+schemaIdentity.getSource()+
				":"+schemaIdentity.getEntityType()+
				":"+schemaIdentity.getSchemaVersionMajor().toString();
	}
}

