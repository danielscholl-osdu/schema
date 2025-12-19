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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.opengroup.osdu.core.aws.v2.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.aws.v2.dynamodb.interfaces.IDynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.GsiQueryRequest;
import org.opengroup.osdu.core.aws.v2.dynamodb.model.QueryPageResult;
import org.opengroup.osdu.core.aws.v2.dynamodb.util.RequestBuilderUtil;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@ConditionalOnProperty(prefix = "repository", name = "implementation", havingValue = "dynamodb", matchIfMissing = true)
@Repository
public class AwsSchemaInfoStore implements ISchemaInfoStore {

  private final DpsHeaders headers;
  private final ITenantFactory tenantFactory;
  private final JaxRsDpsLog log;
  private final ISchemaStore schemaStore;
  private final IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory;
  private final String schemaInfoTableParameterRelativePath;
  private final String sharedTenant;

  public AwsSchemaInfoStore(
      DpsHeaders headers,
      ITenantFactory tenantFactory,
      JaxRsDpsLog log,
      ISchemaStore schemaStore,
      IDynamoDBQueryHelperFactory dynamoDBQueryHelperFactory,
      @Value("${aws.dynamodb.schemaInfoTable.ssm.relativePath}") String schemaInfoTableParameterRelativePath,
      @Value("${shared.tenant.name:common}") String sharedTenant) {
    this.headers = headers;
    this.tenantFactory = tenantFactory;
    this.log = log;
    this.schemaStore = schemaStore;
    this.dynamoDBQueryHelperFactory = dynamoDBQueryHelperFactory;
    this.schemaInfoTableParameterRelativePath = schemaInfoTableParameterRelativePath;
    this.sharedTenant = sharedTenant;
  }

  private DynamoDBQueryHelper<SchemaInfoDoc> getSchemaInfoTableQueryHelper() {
    return dynamoDBQueryHelperFactory.createQueryHelper(headers, schemaInfoTableParameterRelativePath,
        SchemaInfoDoc.class);
  }

  private DynamoDBQueryHelper<SchemaInfoDoc> getSchemaInfoTableQueryHelper(String dataPartitionId) {
    return dynamoDBQueryHelperFactory.createQueryHelper(dataPartitionId, schemaInfoTableParameterRelativePath,
        SchemaInfoDoc.class);
  }

  @Override
  public SchemaInfo getSchemaInfo(String schemaId) throws NotFoundException {
    DynamoDBQueryHelper<SchemaInfoDoc> queryHelper = getSchemaInfoTableQueryHelper();

    String id = String.format("%s:%s", headers.getPartitionId(), schemaId);
    Optional<SchemaInfoDoc> result = queryHelper.getItem(id);
    if (result.isEmpty()) {
      throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
    }
    return result.get().getSchemaInfo();
  }

  @Override
  public SchemaInfo getSystemSchemaInfo(String schemaId) throws NotFoundException {
    this.updateDataPartitionId();
    return this.getSchemaInfo(schemaId);
  }

  private void upsertSchemaRecord(SchemaInfo schemaInfo, SchemaInfoDoc schemaInfoDoc, String partitionId)
      throws ApplicationException, BadRequestException {
    DynamoDBQueryHelper<SchemaInfoDoc> queryHelper = getSchemaInfoTableQueryHelper();
    SchemaIdentity supersedingSchema = schemaInfo.getSupersededBy();
    if (supersedingSchema != null) {
      String id = partitionId + ":" + supersedingSchema.getId();
      // Check if superseding schema exists
      Optional<SchemaInfoDoc> supersedingDoc = queryHelper.getItem(id);
      if (supersedingDoc.isEmpty()) {
        throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
      }
    }
    try {
      queryHelper.putItem(schemaInfoDoc);
    } catch (Exception ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
    }
  }

  @Override
  public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    String partitionId = headers.getPartitionId();
    String schemaId = schema.getSchemaInfo().getSchemaIdentity().getId();
    String id = partitionId + ":" + schemaId;

    // Retrieve the existing schema to preserve creation metadata
    SchemaInfo existingSchema;
    try {
      existingSchema = getSchemaInfo(schemaId);
    } catch (NotFoundException e) {
      throw new BadRequestException("Cannot update schema that doesn't exist: " + schemaId);
    }

    // Update the schema while preserving creation metadata
    SchemaInfo schemaInfo = schema.getSchemaInfo();
    schemaInfo.setCreatedBy(existingSchema.getCreatedBy());
    schemaInfo.setDateCreated(existingSchema.getDateCreated());

    SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.mapFrom(schemaInfo, partitionId);
    schemaInfoDoc.setId(id);
    upsertSchemaRecord(schemaInfo, schemaInfoDoc, partitionId);
    return schemaInfoDoc.getSchemaInfo();
  }

  @Override
  public SchemaInfo updateSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    this.updateDataPartitionId();
    return this.updateSchemaInfo(schema);
  }

  @Override
  public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    DynamoDBQueryHelper<SchemaInfoDoc> queryHelper = getSchemaInfoTableQueryHelper();

    String partitionId = headers.getPartitionId();
    String userEmail = headers.getUserEmail();
    String id = partitionId + ":" + schema.getSchemaInfo().getSchemaIdentity().getId();

    // Set Audit properties
    SchemaInfo schemaInfo = schema.getSchemaInfo();
    schemaInfo.setCreatedBy(userEmail);
    schemaInfo.setDateCreated(DateTime.now().toDate());

    SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.mapFrom(schema.getSchemaInfo(), partitionId);
    schemaInfoDoc.setId(id);

    try {
      // Use conditional expression to ensure the item doesn't already exist
      queryHelper.putItem(
          PutItemEnhancedRequest.builder(SchemaInfoDoc.class)
              .item(schemaInfoDoc)
              .conditionExpression(Expression.builder()
                  .expression("attribute_not_exists(Id)")
                  .build())
              .build());
    } catch (ConditionalCheckFailedException e) {
      throw new BadRequestException("Schema " + id + " already exist. Can't create again.");
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
    DynamoDBQueryHelper<SchemaInfoDoc> queryHelper = getSchemaInfoTableQueryHelper();
    String dataPartitionId = headers.getPartitionId();
    SchemaInfoDoc fullSchemaInfoDoc = SchemaInfoDoc.mapFrom(schemaInfo, dataPartitionId);
    String gsiPartitionKey = fullSchemaInfoDoc.getGsiPartitionKey();
    
    // Create a query document with the GSI partition key and major version
    SchemaInfoDoc queryDoc = SchemaInfoDoc.builder()
        .gsiPartitionKey(gsiPartitionKey)
        .majorVersion(fullSchemaInfoDoc.getMajorVersion())
        .build();

    // Use buildGsiRequest() to create a GsiQueryRequest object with the correct index name
    GsiQueryRequest<SchemaInfoDoc> request = RequestBuilderUtil.QueryRequestBuilder
        .forQuery(queryDoc, "major-version-index", SchemaInfoDoc.class)
        .buildGsiRequest();

    // Call queryByGSI with just the GsiQueryRequest
    QueryPageResult<SchemaInfoDoc> results = queryHelper.queryByGSI(request);

    // Use Java streams to find the maximum minor version
    Optional<SchemaInfoDoc> latestSchema = results.getItems().stream()
        .max((a, b) -> Long.compare(
            a.getSchemaInfo().getSchemaIdentity().getSchemaVersionMinor(),
            b.getSchemaInfo().getSchemaIdentity().getSchemaVersionMinor()));

    if (latestSchema.isPresent()) {
        String schemaId = latestSchema.get().getSchemaInfo().getSchemaIdentity().getId();
        try {
            return schemaStore.getSchema(dataPartitionId, schemaId);
        } catch (NotFoundException ex) {
            log.error("Schema not found for ID: " + schemaId, ex);
        }
    }
    return "";
  }

  @Override
  public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) {
    DynamoDBQueryHelper<SchemaInfoDoc> queryHelper = getSchemaInfoTableQueryHelper(tenantId);

    // Build filter expressions
    Map<String, AttributeValue> expressionValues = new HashMap<>();
    List<String> filterConditions = new ArrayList<>();

    // Add data partition filter - this is required
    filterConditions.add("DataPartitionId = :dataPartitionId");
    expressionValues.put(":dataPartitionId", AttributeValue.builder().s(tenantId).build());

    // Add all query parameters using helper method
    addFilterIfNotNull(filterConditions, expressionValues, "SchemaAuthority", queryParams.getAuthority(), false);
    addFilterIfNotNull(filterConditions, expressionValues, "SchemaSource", queryParams.getSource(), false);
    addFilterIfNotNull(filterConditions, expressionValues, "SchemaEntityType", queryParams.getEntityType(), false);
    addFilterIfNotNull(filterConditions, expressionValues, "SchemaScope", queryParams.getScope(), false);
    addFilterIfNotNull(filterConditions, expressionValues, "SchemaStatus", queryParams.getStatus(), false);
    addFilterIfNotNull(filterConditions, expressionValues, "MajorVersion", queryParams.getSchemaVersionMajor(), true);
    addFilterIfNotNull(filterConditions, expressionValues, "MinorVersion", queryParams.getSchemaVersionMinor(), true);
    addFilterIfNotNull(filterConditions, expressionValues, "PatchVersion", queryParams.getSchemaVersionPatch(), true);

    // Combine filter conditions
    String filterExpression = String.join(" AND ", filterConditions);
    log.info("SchemaInfo query filter expression: {}", filterExpression);

    // Use RequestBuilderUtil to build the scan request
    ScanEnhancedRequest scanRequest = RequestBuilderUtil.ScanRequestBuilder
        .forScan(SchemaInfoDoc.class)
        .filterExpression(filterExpression, expressionValues)
        .build();

    // Execute the scan
    List<SchemaInfoDoc> results = queryHelper.scanTable(scanRequest);

    List<SchemaInfo> toReturn = results.stream()
        .map(SchemaInfoDoc::getSchemaInfo)
        .collect(Collectors.toCollection(ArrayList::new));

    if (queryParams.getLatestVersion() != null && queryParams.getLatestVersion()) {
      toReturn = getLatestVersionSchemaList(toReturn);
    }

    return toReturn;
  }

  /**
   * Helper method to add a filter condition if the value is not null
   */
  private void addFilterIfNotNull(List<String> conditions, Map<String, AttributeValue> values,
      String attributeName, Object value, boolean isNumber) {
    if (value != null) {
      String valueStr = value.toString();
      
      // Special handling for entityType with wildcards
      if (attributeName.equals("SchemaEntityType") && valueStr.contains("*")) {
        addPatternFilter(conditions, values, attributeName, valueStr);
      } else {
        // Existing exact match logic
        String placeholder = String.format(":%s", attributeName.toLowerCase());
        conditions.add(String.format("%s = %s", attributeName, placeholder));

        if (isNumber) {
          values.put(placeholder, AttributeValue.builder().n(valueStr).build());
        } else {
          values.put(placeholder, AttributeValue.builder().s(valueStr).build());
        }
      }
    }
  }

  /**
   * Helper method to add pattern-based filter conditions for wildcard searches
   */
  private void addPatternFilter(List<String> conditions, Map<String, AttributeValue> values,
      String attributeName, String pattern) {
    String placeholder = String.format(":%s_pattern", attributeName.toLowerCase());
    
    if (pattern.equals("*")) {
      // Match all - don't add any filter
      return;
    } else if (pattern.endsWith("*") && !pattern.startsWith("*")) {
      // Starts with pattern (e.g., "well*")
      String prefix = pattern.substring(0, pattern.length() - 1);
      // Validate that the prefix doesn't contain wildcards
      if (prefix.contains("*")) {
        throw new BadRequestException("Unsupported wildcard pattern: " + pattern + 
            ". Only patterns like 'prefix*', '*substring*', and '*' are supported.");
      }
      conditions.add(String.format("begins_with(%s, %s)", attributeName, placeholder));
      values.put(placeholder, AttributeValue.builder().s(prefix).build());
    } else if (pattern.startsWith("*") && pattern.endsWith("*")) {
      // Contains pattern (e.g., "*well*")
      String substring = pattern.substring(1, pattern.length() - 1);
      // Validate that the substring doesn't contain wildcards
      if (substring.contains("*")) {
        throw new BadRequestException("Unsupported wildcard pattern: " + pattern + 
            ". Only patterns like 'prefix*', '*substring*', and '*' are supported.");
      }
      conditions.add(String.format("contains(%s, %s)", attributeName, placeholder));
      values.put(placeholder, AttributeValue.builder().s(substring).build());
    } else if (pattern.startsWith("*") && !pattern.endsWith("*")) {
      // Ends with pattern (e.g., "*well") - DynamoDB doesn't support ends_with natively
      throw new BadRequestException("Unsupported wildcard pattern: " + pattern + 
          ". 'ends with' patterns (*suffix) are not supported by DynamoDB. Only patterns like 'prefix*', '*substring*', and '*' are supported.");
    } else {
      // Any other pattern with wildcards in the middle (e.g., "w*ll", "w*l*l") is unsupported
      if (pattern.contains("*")) {
        throw new BadRequestException("Unsupported wildcard pattern: " + pattern + 
            ". Only patterns like 'prefix*', '*substring*', and '*' are supported.");
      } else {
        // No wildcards - treat as exact match
        String placeholderExact = String.format(":%s", attributeName.toLowerCase());
        conditions.add(String.format("%s = %s", attributeName, placeholderExact));
        values.put(placeholderExact, AttributeValue.builder().s(pattern).build());
      }
    }
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

    // Add all tenants if checking from shared tenant
    if (tenantId.equalsIgnoreCase(sharedTenant)) {
      List<String> privateTenantList = tenantFactory.listTenantInfo().stream()
          .map(TenantInfo::getDataPartitionId)
          .toList();
      tenantList.addAll(privateTenantList);
    }

    // Check uniqueness across all relevant tenants
    for (String tenant : tenantList) {
      DynamoDBQueryHelper<SchemaInfoDoc> queryHelper = getSchemaInfoTableQueryHelper(tenant);
      String id = tenant + ":" + schemaId;
      Optional<SchemaInfoDoc> item = queryHelper.getItem(id);
      if (item.isPresent()) {
        return false; // Schema ID exists, not unique
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
    DynamoDBQueryHelper<SchemaInfoDoc> queryHelper = getSchemaInfoTableQueryHelper();
    String id = headers.getPartitionId() + ":" + schemaId;

    try {
      queryHelper.deleteItem(id);
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
    if (!sortedMap.isEmpty()) {
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
