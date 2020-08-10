package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.joda.time.DateTime;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelper;
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
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;
import org.opengroup.osdu.schema.provider.aws.models.SchemaInfoDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.opengroup.osdu.schema.util.VersionHierarchyUtil;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class AwsSchemaInfoStore implements ISchemaInfoStore {

  @Inject
  private DpsHeaders headers;

  @Inject
  private JaxRsDpsLog log;

  @Inject
  private AwsServiceConfig serviceConfig;

  @Inject
  private ISchemaStore schemaStore;

  private DynamoDBQueryHelper queryHelper;

  @PostConstruct
  public void init() {
    queryHelper = new DynamoDBQueryHelper(serviceConfig.getDynamoDbEndpoint(),
            serviceConfig.getAmazonRegion(),
            serviceConfig.getDynamoDbTablePrefix());
  }

  @Override
  public SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
    String id = headers.getPartitionId() + ":" + schemaId;
    SchemaInfoDoc result = queryHelper.loadByPrimaryKey(SchemaInfoDoc.class, id);
    if (result == null) {
      throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
    }
    return result.getSchemaInfo();
  }

  @Override
  public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    // The SchemaService calls the getSchemaInfo method and verifies the entity is updateable, however,
    // it doesn't pass that entity into this method or update properties in the request that shouldn't change, like
    // createdBy and createdOn.  This causes the need to query the entity twice which is inefficient.
    // This should be fixed

    String partitionId = headers.getPartitionId();
    String userEmail = headers.getUserEmail();
    String id = partitionId + ":" + schema.getSchemaInfo().getSchemaIdentity().getId();

    // Set Audit properties
    SchemaInfo schemaInfo = schema.getSchemaInfo();

    SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.mapFrom(schema.getSchemaInfo(), partitionId);
    schemaInfoDoc.setDataPartitionId(partitionId);
    schemaInfoDoc.setId(id);

    throwExceptionIfSupersedingSchemaIsNotFoundInDb(schemaInfo.getSupersededBy(), partitionId);

    try {
      queryHelper.save(schemaInfoDoc);
    } catch (Exception ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
    }

    return schemaInfoDoc.getSchemaInfo();
  }

  @Override
  public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
    String partitionId = headers.getPartitionId();
    String userEmail = headers.getUserEmail();
    String id = partitionId + ":" + schema.getSchemaInfo().getSchemaIdentity().getId();

    // Set Audit properties
    SchemaInfo schemaInfo = schema.getSchemaInfo();
    schemaInfo.setCreatedBy(userEmail);
    schemaInfo.setDateCreated(DateTime.now().toDate());

    SchemaInfoDoc schemaInfoDoc = SchemaInfoDoc.mapFrom(schema.getSchemaInfo(), partitionId);
    schemaInfoDoc.setDataPartitionId(partitionId);
    schemaInfoDoc.setId(id);

    throwExceptionIfSupersedingSchemaIsNotFoundInDb(schemaInfo.getSupersededBy(), partitionId);

    try {
      queryHelper.save(schemaInfoDoc);
    } catch (Exception ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
    }

    return schemaInfoDoc.getSchemaInfo();
  }

  @Override
  public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {
    String dataPartitionId = headers.getPartitionId();
    SchemaInfoDoc fullSchemaInfoDoc = SchemaInfoDoc.mapFrom(schemaInfo, headers.getPartitionId());
    // remap to another object only containing the partial key
    SchemaInfoDoc query = SchemaInfoDoc.builder()
            .dataPartitionId(fullSchemaInfoDoc.getDataPartitionId())
            .authority(fullSchemaInfoDoc.getAuthority())
            .scope(fullSchemaInfoDoc.getScope())
            .entityType(fullSchemaInfoDoc.getEntityType())
            .build();

    PaginatedQueryList<SchemaInfoDoc> results = queryHelper.queryByGSI(SchemaInfoDoc.class,
            query,
            "MajorVersion",
            fullSchemaInfoDoc.getMajorVersion());

    TreeMap<Long, SchemaInfoDoc> sortedMap = new TreeMap<>(Collections.reverseOrder());

    results.forEach(item -> sortedMap.put(item.getSchemaInfo().getSchemaIdentity().getSchemaVersionMinor(), item));

    if (sortedMap.size() != 0) {
      SchemaInfoDoc item = sortedMap.firstEntry().getValue();
      String schemaId = item.getSchemaInfo().getSchemaIdentity().getId();
      try {
        return schemaStore.getSchema(dataPartitionId, schemaId);
      } catch (NotFoundException ex) {
        // probably should log something here.  Maybe the getSchema method logs, not sure.
        // and not sure if returning empty string (allow process to continue
        return new String();
      }
    }
    return new String();

  }

  @Override
  public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException {
    // This function is called twice.  Once for tenant `common` and once for the requested tenant.

    // TODO: how should the system handle empty query params i.e. &scope=
    // is it equal to empty string or should the qualifier be removed?

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
    List<SchemaInfo> toReturn = results.stream().map(item -> item.getSchemaInfo()).collect(Collectors.toList());

    if (queryParams.getLatestVersion() != null && queryParams.getLatestVersion()) {
      toReturn = getLatestVersionSchemaList(toReturn);
    }

    return toReturn;
  }

  @Override
  public boolean isUnique(String schemaId, String tenantId) throws ApplicationException {
    String id = tenantId + ":" + schemaId;
    SchemaInfoDoc schemaInfoDoc = new SchemaInfoDoc();
    schemaInfoDoc.setId(id);
    return !queryHelper.keyExistsInTable(SchemaInfoDoc.class, schemaInfoDoc);
  }

  @Override
  public boolean cleanSchema(String schemaId) throws ApplicationException {

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

  private void throwExceptionIfSupersedingSchemaIsNotFoundInDb(SchemaIdentity schema, String tenantId) throws ApplicationException, BadRequestException {
    if (schema != null) {
      if (!isUnique(schema.getId(), tenantId)) {
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
}
