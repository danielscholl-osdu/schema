package org.opengroup.osdu.schema.service.serviceimpl;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NoSchemaFoundException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaInfoResponse;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.opengroup.osdu.schema.service.IAuthorityService;
import org.opengroup.osdu.schema.service.IEntityTypeService;
import org.opengroup.osdu.schema.service.ISchemaService;
import org.opengroup.osdu.schema.service.ISourceService;
import org.opengroup.osdu.schema.util.SchemaResolver;
import org.opengroup.osdu.schema.util.SchemaUtil;
import org.opengroup.osdu.schema.util.VersionHierarchyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;

/**
 * Schema Service to register, get and update schema.
 *
 */
@Service
public class SchemaService implements ISchemaService {

    @Autowired
    private ISchemaInfoStore schemaInfoStore;

    @Autowired
    private ISchemaStore schemaStore;

    @Autowired
    private IAuthorityService authorityService;

    @Autowired
    private ISourceService sourceService;

    @Autowired
    private IEntityTypeService entityTypeService;

    @Autowired
    private SchemaUtil schemaUtil;

    @Autowired
    private SchemaResolver schemaResolver;

    @Autowired
    JaxRsDpsLog log;

    @Autowired
    DpsHeaders headers;

    /**
     * Method to get schema
     *
     * @param schemaId
     * @return schema object.
     * @throws ApplicationException
     */
    @Override
    public Object getSchema(String schemaId) throws BadRequestException, NotFoundException, ApplicationException {
        Object schema = "";
        String dataPartitionId = headers.getPartitionId();
        validateSchemaId(schemaId);
        log.info(SchemaConstants.SCHEMA_GET_STARTED);
        try {
            schema = schemaStore.getSchema(dataPartitionId, schemaId);
        } catch (NotFoundException e) {
                schema = schemaStore.getSchema(SchemaConstants.ACCOUNT_ID_COMMON_PROJECT, schemaId);
        }

        return schema;
    }

    private void validateSchemaId(String schemaId) throws BadRequestException {
        if (StringUtils.isEmpty(schemaId)) {
            log.error(SchemaConstants.EMPTY_ID);
            throw new BadRequestException(SchemaConstants.EMPTY_ID);
        }
    }

    /**
     * Method to create schema
     *
     * @param schema request
     * @return schemaInfo.
     * @throws JSONException
     * @throws JsonProcessingException
     */
    @Override
    public SchemaInfo createSchema(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException {
        String dataPartitionId = headers.getPartitionId();
        String schemaId = createAndSetSchemaId(schemaRequest);
        if (schemaInfoStore.isUnique(schemaId, dataPartitionId)) {
            setScope(schemaRequest, dataPartitionId);

            String latestMinorSchema = schemaInfoStore.getLatestMinorVerSchema(schemaRequest.getSchemaInfo());

            Gson gson = new Gson();
            if (StringUtils.isNotEmpty(latestMinorSchema)) {
                schemaUtil.checkBreakingChange(gson.toJson(schemaRequest.getSchema()), latestMinorSchema);
            }
            String schema = schemaResolver.resolveSchema(gson.toJson(schemaRequest.getSchema()));

            Boolean authority = authorityService.checkAndRegisterAuthorityIfNotPresent(
                    schemaRequest.getSchemaInfo().getSchemaIdentity().getAuthority());
            Boolean source = sourceService
                    .checkAndRegisterSourceIfNotPresent(schemaRequest.getSchemaInfo().getSchemaIdentity().getSource());
            Boolean entity = entityTypeService.checkAndRegisterEntityTypeIfNotPresent(
                    schemaRequest.getSchemaInfo().getSchemaIdentity().getEntityType());

            if (authority && source && entity) {
                log.info(SchemaConstants.SCHEMA_CREATION_STARTED);
                try {
                    SchemaInfo schemaInfo = schemaInfoStore.createSchemaInfo(schemaRequest);
                    schemaStore.createSchema(schemaId, schema);
                    return schemaInfo;
                } catch (ApplicationException ex) {
                    log.warning(SchemaConstants.SCHEMA_CREATION_FAILED);
                    schemaInfoStore.cleanSchema(schemaId);
                    schemaStore.cleanSchemaProject(schemaId);
                    log.info(SchemaConstants.SCHEMA_CREATE_CLEAN);
                    throw ex;
                }
            } else {
                log.error("The schema could not be created due invalid authority,source or entityType");
                throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
            }
        } else {
            throw new BadRequestException(SchemaConstants.SCHEMA_ID_EXISTS);
        }
    }

    /**
     * Method to update schema. Schemas that are in Development state can be
     * updated. This method checks for the presence of schema based on the schema Id
     * provided in input Payload, if found updates both the schemaInfo and schema.
     *
     * @param schemarequest
     * @return schemaInfo.
     * @throws IOException
     * @throws JsonProcessingException
     * @throws JSONException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Override
    public SchemaInfo updateSchema(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException {
        String dataPartitionId = headers.getPartitionId();
        String createdSchemaId = createAndSetSchemaId(schemaRequest);
        SchemaInfo schemaInfo = null;
        try {
            schemaInfo = schemaInfoStore.getSchemaInfo(createdSchemaId);
        } catch (NotFoundException e) {

            log.error(SchemaConstants.INVALID_SCHEMA_UPDATE);

            if (!SchemaStatus.DEVELOPMENT.equals(schemaRequest.getSchemaInfo().getStatus()))
                throw new BadRequestException(SchemaConstants.SCHEMA_PUT_CREATE_EXCEPTION);

            throw new NoSchemaFoundException(SchemaConstants.INVALID_SCHEMA_UPDATE);
        }

        if (SchemaStatus.DEVELOPMENT.equals(schemaInfo.getStatus())) {
            log.info(MessageFormat.format(SchemaConstants.SCHEMA_UPDATION_STARTED, createdSchemaId));
            setScope(schemaRequest, dataPartitionId);
            Gson gson = new Gson();
            String schema = schemaResolver.resolveSchema(gson.toJson(schemaRequest.getSchema()));
            SchemaInfo schInfo = schemaInfoStore.updateSchemaInfo(schemaRequest);
            schemaStore.createSchema(schemaRequest.getSchemaInfo().getSchemaIdentity().getId(), schema);
            log.info(SchemaConstants.SCHEMA_UPDATED);
            return schInfo;
        } else {
            log.error(SchemaConstants.SCHEMA_UPDATE_ERROR);
            throw new BadRequestException(SchemaConstants.SCHEMA_UPDATE_EXCEPTION);
        }

    }

    private String createSchemaId(SchemaRequest schemaRequest) {
        SchemaIdentity schemaIdentity = schemaRequest.getSchemaInfo().getSchemaIdentity();
        return new StringBuilder().append(schemaIdentity.getAuthority()).append(":").append(schemaIdentity.getSource())
                .append(":").append(schemaIdentity.getEntityType()).append(":")
                .append(schemaIdentity.getSchemaVersionMajor()).append(".")
                .append(schemaIdentity.getSchemaVersionMinor()).append(".")
                .append(schemaIdentity.getSchemaVersionPatch()).toString();
    }

    private String createAndSetSchemaId(SchemaRequest schemaRequest) {
        String schemaId = createSchemaId(schemaRequest);
        schemaRequest.getSchemaInfo().getSchemaIdentity().setId(schemaId);
        return schemaId;
    }

    @Override
    public SchemaInfoResponse getSchemaInfoList(QueryParams queryParams)
            throws BadRequestException, ApplicationException {

        String tenantId = headers.getPartitionId();
        List<SchemaInfo> schemaList = new LinkedList<>();

        latestVersionMajorMinorFiltersCheck(queryParams);

        if (queryParams.getScope() != null) {

            if (queryParams.getScope().equalsIgnoreCase(SchemaScope.SHARED.toString())) {
                getSchemaInfos(queryParams, schemaList, SchemaConstants.ACCOUNT_ID_COMMON_PROJECT);
            }

            else if (queryParams.getScope().equalsIgnoreCase(SchemaScope.INTERNAL.toString())) {
                getSchemaInfos(queryParams, schemaList, tenantId);
            }
        } else {
            getSchemaInfos(queryParams, schemaList, SchemaConstants.ACCOUNT_ID_COMMON_PROJECT);

            if (!SchemaConstants.ACCOUNT_ID_COMMON_PROJECT.equalsIgnoreCase(tenantId)) {
                getSchemaInfos(queryParams, schemaList, tenantId);
            }
        }
        
        if (queryParams.getLatestVersion() != null && queryParams.getLatestVersion()) {
        	schemaList = getLatestVersionSchemaList(schemaList);
        }

        List<SchemaInfo> schemaFinalList = schemaList.stream().skip(queryParams.getOffset())
                .limit(queryParams.getLimit()).collect(Collectors.toList());

        return SchemaInfoResponse.builder().schemaInfos(schemaFinalList).count(schemaFinalList.size())
                .offset(queryParams.getOffset()).totalCount(schemaList.size()).build();
    }

    private void getSchemaInfos(QueryParams queryParams, List<SchemaInfo> schemaList, String tenant)
            throws ApplicationException {
        schemaInfoStore.getSchemaInfoList(queryParams, tenant).forEach(schemaList::add);
    }

    private void latestVersionMajorMinorFiltersCheck(QueryParams queryParams) throws BadRequestException {
        if (queryParams.getLatestVersion() != null && queryParams.getLatestVersion()) {

            if (queryParams.getSchemaVersionMajor() == null && queryParams.getSchemaVersionMinor() != null)
                throw new BadRequestException(SchemaConstants.LATESTVERSION_MINORFILTER_WITHOUT_MAJOR);

            if (queryParams.getSchemaVersionMinor() == null && queryParams.getSchemaVersionPatch() != null)
                throw new BadRequestException(SchemaConstants.LATESTVERSION_PATCHFILTER_WITHOUT_MINOR);

        }
    }

    /**
     * Method to set the scope of the schema according to the tenant
     * 
     * @param schemaRequest
     * @param dataPartitionId
     */
    private void setScope(SchemaRequest schemaRequest, String dataPartitionId) {
        if (dataPartitionId.equalsIgnoreCase(SchemaConstants.ACCOUNT_ID_COMMON_PROJECT)) {
            schemaRequest.getSchemaInfo().setScope(SchemaScope.SHARED);
        } else {
            schemaRequest.getSchemaInfo().setScope(SchemaScope.INTERNAL);
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
