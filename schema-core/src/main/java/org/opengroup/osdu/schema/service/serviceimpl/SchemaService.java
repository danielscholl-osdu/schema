package org.opengroup.osdu.schema.service.serviceimpl;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.opengroup.osdu.schema.exceptions.SchemaVersionException;
import org.opengroup.osdu.schema.logging.AuditLogger;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaInfoResponse;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.model.SchemaUpsertResponse;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.opengroup.osdu.schema.service.IAuthorityService;
import org.opengroup.osdu.schema.service.IEntityTypeService;
import org.opengroup.osdu.schema.service.ISchemaService;
import org.opengroup.osdu.schema.service.ISourceService;
import org.opengroup.osdu.schema.util.SchemaComparatorByVersion;
import org.opengroup.osdu.schema.util.SchemaResolver;
import org.opengroup.osdu.schema.util.SchemaUtil;
import org.opengroup.osdu.schema.validation.SchemaVersionValidatorFactory;
import org.opengroup.osdu.schema.validation.SchemaVersionValidatorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;

/**
 * Schema Service to register, get and update schema.
 *
 */
@Service
@RequiredArgsConstructor
public class SchemaService implements ISchemaService {

	private final AuditLogger auditLogger;

	private final ISchemaInfoStore schemaInfoStore;

	private final ISchemaStore schemaStore;

	private final IAuthorityService authorityService;

	private final ISourceService sourceService;

	private final IEntityTypeService entityTypeService;

	private final SchemaUtil schemaUtil;

	private SchemaResolver schemaResolver;

	private final SchemaVersionValidatorFactory versionValidatorFactory;

	@Value("${shared.tenant.name:common}")
	private String sharedTenant;

	final JaxRsDpsLog log;

	private final IMessageBus messageBus;

	@Autowired
	public void setSchemaResolver(SchemaResolver schemaResolver) {
		this.schemaResolver = schemaResolver;
	}

	final DpsHeaders headers;

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
			schema = schemaStore.getSchema(sharedTenant, schemaId);
		}

		auditLogger.schemaRetrievedSuccess(Collections.singletonList(schema.toString()));
		return schema;
	}

	private void validateSchemaId(String schemaId) throws BadRequestException {
		if (StringUtils.isEmpty(schemaId)) {
			auditLogger.schemaRetrievedFailure(Collections.singletonList(schemaId));
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

			String schema = resolveAndCheckBreakingChanges(schemaRequest);

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
					auditLogger.schemaRegisteredSuccess(Collections.singletonList(schemaRequest.toString()));
					messageBus.publishMessage(schemaId, SchemaConstants.SCHEMA_CREATE_EVENT_TYPE);
					return schemaInfo;
				} catch (ApplicationException ex) {
					auditLogger.schemaRegisteredFailure(
							Collections.singletonList(schemaRequest.toString()));
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
			String schema = resolveAndCheckBreakingChanges(schemaRequest);
			SchemaInfo schInfo = schemaInfoStore.updateSchemaInfo(schemaRequest);
			auditLogger.schemaUpdatedSuccess(Collections.singletonList(schemaRequest.toString()));
			schemaStore.createSchema(schemaRequest.getSchemaInfo().getSchemaIdentity().getId(), schema);
			messageBus.publishMessage(createdSchemaId, SchemaConstants.SCHEMA_UPDATE_EVENT_TYPE);
			log.info(SchemaConstants.SCHEMA_UPDATED);
			return schInfo;
		} else {
			auditLogger.schemaUpdatedFailure(Collections.singletonList(schemaRequest.toString()));
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

	private String resolveAndCheckBreakingChanges(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException {

		Gson gson = new Gson();
		String schemaInRequestPayload = gson.toJson(schemaRequest.getSchema());
		String fullyResolvedInputSchema = schemaResolver.resolveSchema(schemaInRequestPayload);
		compareFullyResolvedSchema(schemaRequest.getSchemaInfo(), fullyResolvedInputSchema);
		return fullyResolvedInputSchema;
	}

	@Override
	public SchemaInfoResponse getSchemaInfoList(QueryParams queryParams)
			throws BadRequestException, ApplicationException {

		String tenantId = headers.getPartitionId();
		List<SchemaInfo> schemaList = new LinkedList<>();

		latestVersionMajorMinorFiltersCheck(queryParams);

		if (queryParams.getScope() != null) {

			if (queryParams.getScope().equalsIgnoreCase(SchemaScope.SHARED.toString())) {
				getSchemaInfos(queryParams, schemaList, sharedTenant);
			}

			else if (queryParams.getScope().equalsIgnoreCase(SchemaScope.INTERNAL.toString())) {
				getSchemaInfos(queryParams, schemaList, tenantId);
			}
		} else {
			getSchemaInfos(queryParams, schemaList, sharedTenant);

			if (!sharedTenant.equalsIgnoreCase(tenantId)) {
				getSchemaInfos(queryParams, schemaList, tenantId);
			}
		}

		if (queryParams.getLatestVersion() != null && queryParams.getLatestVersion()) {
			schemaList = getLatestVersionSchemaList(schemaList);
		}

		Comparator<SchemaInfo> compareByCreatedDate = (s1,s2) -> s1.getDateCreated().compareTo(s2.getDateCreated());

		List<SchemaInfo> schemaFinalList = schemaList.stream().sorted(compareByCreatedDate)
											.skip(queryParams.getOffset())
											.limit(queryParams.getLimit()).collect(Collectors.toList());

		if (schemaFinalList.isEmpty()){
			auditLogger.searchSchemaFailure(Collections.singletonList(queryParams.toString()));
		} else {
			auditLogger.searchSchemaSuccess(schemaFinalList.stream()
					.map(SchemaInfo::toString)
					.collect(Collectors.toList()));
		}
		return SchemaInfoResponse.builder().schemaInfos(schemaFinalList).count(schemaFinalList.size())
				.offset(queryParams.getOffset()).totalCount(schemaList.size()).build();
	}

	@Override
	public SchemaUpsertResponse upsertSchema(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException {
		SchemaInfo response = null;
		HttpStatus httpCode = HttpStatus.BAD_REQUEST;
		SchemaUpsertResponse.SchemaUpsertResponseBuilder upsertBuilder = SchemaUpsertResponse.builder();
		try {
			response = updateSchema(schemaRequest);
			httpCode = HttpStatus.OK;
		} catch (NoSchemaFoundException noSchemaFound) {
			try {
				response = createSchema(schemaRequest);
				httpCode = HttpStatus.CREATED;
			}catch (BadRequestException badreqEx) {
				//If there is same schema-id for other tenant then throw different error message
				if(SchemaConstants.SCHEMA_ID_EXISTS.equals(badreqEx.getMessage()))
					throw new BadRequestException(SchemaConstants.INVALID_UPDATE_OPERATION);

				throw badreqEx;
			}
		}
		return upsertBuilder.schemaInfo(response).httpCode(httpCode).build();
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
		if (dataPartitionId.equalsIgnoreCase(sharedTenant)) {
			schemaRequest.getSchemaInfo().setScope(SchemaScope.SHARED);
		} else {
			schemaRequest.getSchemaInfo().setScope(SchemaScope.INTERNAL);
		}
	}


	private List<SchemaInfo> getLatestVersionSchemaList(List<SchemaInfo> filteredSchemaList) {

		List<SchemaInfo> latestSchemaList = new ArrayList<>();
		Map<String, SchemaInfo> latestSchemaMap = new HashMap<>();
		SchemaComparatorByVersion schemaComparatorByVersion = new SchemaComparatorByVersion();

		for(SchemaInfo schemaInfo :filteredSchemaList) {

			String key = getGroupingKey(schemaInfo);
			latestSchemaMap.computeIfAbsent(key, k -> schemaInfo);

			SchemaInfo value = latestSchemaMap.get(key);

			if(schemaComparatorByVersion.compare(schemaInfo, value) >= 0) 
				latestSchemaMap.put(key, schemaInfo);

		}

		latestSchemaList.addAll(latestSchemaMap.values());

		return latestSchemaList;
	}

	/***
	 * This method creates a key based on Athority:Source:EntityType
	 * 
	 * @param schemaInfo SchemaInfo whose key is to be formed
	 * @return String based key formed using Athority:Source:EntityType
	 */
	private String getGroupingKey(SchemaInfo schemaInfo){
		return String.join(":", schemaInfo.getSchemaIdentity().getAuthority(),
				schemaInfo.getSchemaIdentity().getSource(), 
				schemaInfo.getSchemaIdentity().getEntityType());
	}

	private void compareFullyResolvedSchema(SchemaInfo inputSchemaInfo, String resolvedInputSchema) throws BadRequestException, ApplicationException {
		try {
			SchemaInfo[] schemaInfoToCompareWith = schemaUtil.findSchemaToCompare(inputSchemaInfo);

			for(SchemaInfo existingSchemaInfo : schemaInfoToCompareWith) {
				if(null == existingSchemaInfo)
					continue;

				String existingSchemaInStore = getSchema(existingSchemaInfo.getSchemaIdentity().getId()).toString();

				try {
					//Compare Major version of the schemas are different
					if(inputSchemaInfo.getSchemaIdentity().getSchemaVersionMajor().compareTo(existingSchemaInfo.getSchemaIdentity().getSchemaVersionMajor()) != 0) {
						continue;
						//Compare Minor version is greater or smaller
					}else if(inputSchemaInfo.getSchemaIdentity().getSchemaVersionMinor().compareTo(existingSchemaInfo.getSchemaIdentity().getSchemaVersionMinor()) < 0){
						versionValidatorFactory.getSchemaVersionValidator(SchemaVersionValidatorType.MINOR).validate(resolvedInputSchema, existingSchemaInStore);
					}else if(inputSchemaInfo.getSchemaIdentity().getSchemaVersionMinor().compareTo(existingSchemaInfo.getSchemaIdentity().getSchemaVersionMinor()) > 0) {
						versionValidatorFactory.getSchemaVersionValidator(SchemaVersionValidatorType.MINOR).validate(existingSchemaInStore, resolvedInputSchema);
					}else {
						versionValidatorFactory.getSchemaVersionValidator(SchemaVersionValidatorType.PATCH).validate(existingSchemaInStore, resolvedInputSchema);
					}
				}catch (SchemaVersionException exc) {
					log.error("Failed to resolve the schema and find breaking changes. Reason :" + exc.getMessage());

					String message = MessageFormat.format(exc.getMessage(), StringUtils.substringAfterLast(inputSchemaInfo.getSchemaIdentity().getId(), SchemaConstants.SCHEMA_KIND_DELIMITER),
							StringUtils.substringAfterLast(existingSchemaInfo.getSchemaIdentity().getId(), SchemaConstants.SCHEMA_KIND_DELIMITER));
					throw new BadRequestException(message);
				}
			}


		}  catch ( NotFoundException e) {
			throw new ApplicationException("Schema not found to evaluate breaking changes.");
		}catch (JSONException exc) {
			log.error("Failed to resolve the schema and find breaking changes. Reason :"+exc.getMessage());
			throw new BadRequestException("Bad Input, invalid json");
		}

	}

}