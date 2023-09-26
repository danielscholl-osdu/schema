/* Copyright © 2020 Amazon Web Services

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
package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperV2;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.aws.models.SchemaInfoDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.opengroup.osdu.schema.util.VersionHierarchyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "dynamodb",
        matchIfMissing = true)
@Repository
public class AwsSchemaInfoStore implements ISchemaInfoStore {

  @Inject
  private DpsHeaders headers;

  @Autowired
  private ITenantFactory tenantFactory;

  @Inject
  private JaxRsDpsLog log;

  @Inject
  private ISchemaStore schemaStore;

  @Inject
  private DynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;

  @Value("${aws.dynamodb.schemaInfoTable.ssm.relativePath}")
  String schemaInfoTableParameterRelativePath;

  @Value("${shared.tenant.name:common}")
  private String sharedTenant;

  private DynamoDBQueryHelperV2 getSchemaInfoTableQueryHelper() {
    return dynamoDBQueryHelperFactory.getQueryHelperForPartition(headers, schemaInfoTableParameterRelativePath);
  }

  private DynamoDBQueryHelperV2 getSchemaInfoTableQueryHelper(String dataPartitionId) {
    return dynamoDBQueryHelperFactory.getQueryHelperForPartition(dataPartitionId, schemaInfoTableParameterRelativePath);
  }


  @Override
  public SchemaInfo getSchemaInfo(String schemaId) throws NotFoundException {

    DynamoDBQueryHelperV2 queryHelper = getSchemaInfoTableQueryHelper();

    String id = headers.getPartitionId() + ":" + schemaId;
    SchemaInfoDoc result = queryHelper.loadByPrimaryKey(SchemaInfoDoc.class, id);
    if (result == null) {
      throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
    }
    return result.getSchemaInfo();
  }

  @Override
  public SchemaInfo getSystemSchemaInfo(String schemaId) throws NotFoundException {
    this.updateDataPartitionId();
    return this.getSchemaInfo(schemaId);
  }

  @Override
  public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    // The SchemaService calls the getSchemaInfo method and verifies the entity is updatable, however,
    // it doesn't pass that entity into this method or update properties in the request that shouldn't change, like
    // createdBy and createdOn.  This causes the need to query the entity twice which is inefficient.
    // This should be fixed

    DynamoDBQueryHelperV2 queryHelper = getSchemaInfoTableQueryHelper();

    String partitionId = headers.getPartitionId();
    String userEmail = headers.getUserEmail();
    String id = partitionId + ":" + schema.getSchemaInfo().getSchemaIdentity().getId();

    // Set Audit properties
    SchemaInfo schemaInfo = schema.getSchemaInfo();
    schemaInfo.setCreatedBy(userEmail);
    schemaInfo.setDateCreated(DateTime.now().toDate());
    SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.mapFrom(schemaInfo, partitionId);
    schemaInfoDoc.setId(id);

    try {
      validateSupersededById(schemaInfo.getSupersededBy(), partitionId);
      queryHelper.save(schemaInfoDoc);
    } catch (BadRequestException ex) {
      throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
    } catch (Exception ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
    }

    return schemaInfoDoc.getSchemaInfo();
  }

  @Override
  public SchemaInfo updateSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    this.updateDataPartitionId();
    return this.updateSchemaInfo(schema);
  }

  @Override
  public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {

    DynamoDBQueryHelperV2 queryHelper = getSchemaInfoTableQueryHelper();

    String partitionId = headers.getPartitionId();
    String userEmail = headers.getUserEmail();
    String id = partitionId + ":" + schema.getSchemaInfo().getSchemaIdentity().getId();

    // Set Audit properties
    SchemaInfo schemaInfo = schema.getSchemaInfo();
    schemaInfo.setCreatedBy(userEmail);
    schemaInfo.setDateCreated(DateTime.now().toDate());

    SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.mapFrom(schema.getSchemaInfo(), partitionId);
    schemaInfoDoc.setId(id);

    if (queryHelper.keyExistsInTable(SchemaInfoDoc.class, schemaInfoDoc) ){
      throw new BadRequestException("Schema " + id + " already exist. Can't create again.");
    }

    try {
      validateSupersededById(schemaInfo.getSupersededBy(), partitionId);
      queryHelper.save(schemaInfoDoc);
    } catch (BadRequestException ex) {
      throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
    } catch (Exception ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
    }

    return schemaInfoDoc.getSchemaInfo();
  }

  @Override
  public SchemaInfo createSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    this.updateDataPartitionId();
    return this.createSchemaInfo(schema);
  }

  @Override
  public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {

    DynamoDBQueryHelperV2 queryHelper = getSchemaInfoTableQueryHelper();

    String dataPartitionId = headers.getPartitionId();
    SchemaInfoDoc fullSchemaInfoDoc = SchemaInfoDoc.mapFrom(schemaInfo, headers.getPartitionId());

    SchemaInfoDoc gsiQuery = new SchemaInfoDoc();
    gsiQuery.setGsiPartitionKey(fullSchemaInfoDoc.getGsiPartitionKey());

    PaginatedQueryList<SchemaInfoDoc> results = queryHelper.queryByGSI(SchemaInfoDoc.class,gsiQuery,"MajorVersion",fullSchemaInfoDoc.getMajorVersion());


    TreeMap<Long, SchemaInfoDoc> sortedMap = new TreeMap<>(Collections.reverseOrder());

    if(results != null && !results.isEmpty())
    	results.forEach(item -> sortedMap.put(item.getSchemaInfo().getSchemaIdentity().getSchemaVersionMinor(), item));

    if (sortedMap.size() != 0) {
      SchemaInfoDoc item = sortedMap.firstEntry().getValue();
      String schemaId = item.getSchemaInfo().getSchemaIdentity().getId();
      try {
        return schemaStore.getSchema(dataPartitionId, schemaId);
      } catch (NotFoundException ex) {
        // probably should log something here.  Maybe the getSchema method logs, not sure.
        // and not sure if returning empty string (allow process to continue
      }
    }
    return "";

  }

  @Override
  public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId)  {
    // This function is called twice.  Once for tenant `common` and once for the requested tenant.

    // Undefined behavior-- how should the system handle empty query params i.e. &scope=
    // is it equal to empty string or should the qualifier be removed?


    DynamoDBQueryHelperV2 queryHelper = getSchemaInfoTableQueryHelper(tenantId);

    List<String> filters = new ArrayList<>();
    Map<String, AttributeValue> valueMap = new HashMap<>();

    filters.add("DataPartitionId = :DataPartitionId");
    valueMap.put(":DataPartitionId", new AttributeValue().withS(tenantId));

    if (queryParams.getAuthority() != null) {
      filters.add("SchemaAuthority = :SchemaAuthority");
      valueMap.put(":SchemaAuthority", new AttributeValue().withS(queryParams.getAuthority()));
    }

    if (queryParams.getSource() != null) {
      filters.add("SchemaSource = :SchemaSource");
      valueMap.put(":SchemaSource", new AttributeValue().withS(queryParams.getSource()));
    }

    if (queryParams.getEntityType() != null) {
      filters.add("SchemaEntityType = :SchemaEntityType");
      valueMap.put(":SchemaEntityType", new AttributeValue().withS(queryParams.getEntityType()));
    }

    if (queryParams.getSchemaVersionMajor() != null) {
      filters.add("MajorVersion = :MajorVersion");
      valueMap.put(":MajorVersion", new AttributeValue().withN(queryParams.getSchemaVersionMajor().toString()));
    }

    if (queryParams.getSchemaVersionMinor() != null) {
      filters.add("MinorVersion = :MinorVersion");
      valueMap.put(":MinorVersion", new AttributeValue().withN(queryParams.getSchemaVersionMinor().toString()));
    }

    if (queryParams.getSchemaVersionPatch() != null) {
      filters.add("PatchVersion = :PatchVersion");
      valueMap.put(":PatchVersion", new AttributeValue().withN(queryParams.getSchemaVersionPatch().toString()));
    }

    if (queryParams.getScope() != null) {
      filters.add("SchemaScope = :SchemaScope");
      valueMap.put(":SchemaScope", new AttributeValue().withS(queryParams.getScope()));
    }

    if (queryParams.getStatus() != null) {
      filters.add("SchemaStatus = :SchemaStatus");
      valueMap.put(":SchemaStatus", new AttributeValue().withS(queryParams.getStatus()));
    }

    String filterExpression = String.join(" and ", filters);
    log.info(String.format("SchemaInfo query filter expression: %s", filterExpression));

    List<SchemaInfoDoc> results = queryHelper.scanTable(SchemaInfoDoc.class, filterExpression, valueMap);
    List<SchemaInfo> toReturn = results.stream().map(SchemaInfoDoc::getSchemaInfo).collect(Collectors.toList());

    if (queryParams.getLatestVersion() != null && queryParams.getLatestVersion()) {
      toReturn = getLatestVersionSchemaList(toReturn);
    }

    return toReturn;
  }

  @Override
  public List<SchemaInfo> getSystemSchemaInfoList(QueryParams queryParams) {
    return this.getSchemaInfoList(queryParams, sharedTenant);
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
    DynamoDBQueryHelperV2 queryHelper = getSchemaInfoTableQueryHelper(tenant);

     String id = tenant + ":" + schemaId;
     SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc();
     schemaInfoDoc.setId(id);
     if(queryHelper.keyExistsInTable(SchemaInfoDoc.class, schemaInfoDoc)){ // the schemaId exists and hence is not unique
       return false;
     }
   }
   return true;
  }

  @Override
  public boolean isUniqueSystemSchema(String schemaId) {
    return this.isUnique(schemaId, sharedTenant);
  }

  @Override
  public boolean cleanSchema(String schemaId) {

    DynamoDBQueryHelperV2 queryHelper = getSchemaInfoTableQueryHelper();

    String id = headers.getPartitionId() + ":" + schemaId;
    SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc();
    schemaInfoDoc.setId(id);
    try {
      queryHelper.deleteByPrimaryKey(SchemaInfoDoc.class, schemaInfoDoc);
      return true;
    } catch (Exception ex) {
      log.error("Unable to delete schema info", ex);
      return false;
    }
  }

  @Override
  public boolean cleanSystemSchema(String schemaId) {
    this.updateDataPartitionId();
    return this.cleanSchema(schemaId);
  }

  private void validateSupersededById(SchemaIdentity supersedingSchema, String tenantId) throws BadRequestException {

    DynamoDBQueryHelperV2 queryHelper = getSchemaInfoTableQueryHelper(tenantId);

    if (supersedingSchema != null) {
      String id = tenantId + ":" + supersedingSchema.getId();
      SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc();
      schemaInfoDoc.setId(id);
      if(queryHelper.keyExistsInTable(SchemaInfoDoc.class, schemaInfoDoc)) // superseding schema does ot exist in the db
      {
         throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
      }

    }
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
        Map.Entry<VersionHierarchyUtil, SchemaInfo> latestVersionEntry = sortedMap.firstEntry();
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
      Map.Entry<VersionHierarchyUtil, SchemaInfo> latestVersionEntry = sortedMap.firstEntry();
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

  private void updateDataPartitionId() {
    headers.put(SchemaConstants.DATA_PARTITION_ID, sharedTenant);
  }
}
