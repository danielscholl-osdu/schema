package org.opengroup.osdu.schema.util;


import java.util.Collections;
import java.util.List;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchemaUtil {

	private final ISchemaInfoStore schemaInfoStore;

	private final JaxRsDpsLog log;

	private final DpsHeaders headers;

	@Value("${shared.tenant.name:common}")
	private String sharedTenant;

	public SchemaInfo[] findSchemaToCompare(SchemaInfo schemaInfo) throws ApplicationException {

		SchemaInfo[] matchingSchemaInfo = findClosestSchemas(schemaInfo, true);

		if(null == matchingSchemaInfo[0] && null == matchingSchemaInfo[1]) {
			log.info("Latest patch version is not found so trying to find the nearest matching major version");
			matchingSchemaInfo =  findClosestSchemas(schemaInfo, false);
		}

		return matchingSchemaInfo;
	}

	public SchemaInfo[] findClosestSchemas(SchemaInfo schemaInfo, boolean atPatchlevel) throws ApplicationException {
		SchemaIdentity schemaIdentity = schemaInfo.getSchemaIdentity();
		SchemaInfo[] schemaInfoArr = new SchemaInfo [2];
		QueryParams.QueryParamsBuilder queryParamBuilder = QueryParams.builder().authority(schemaIdentity.getAuthority())
				.source(schemaIdentity.getSource())
				.entityType(schemaIdentity.getEntityType())
				.schemaVersionMajor(schemaIdentity.getSchemaVersionMajor());

		if(atPatchlevel)
			queryParamBuilder.schemaVersionMinor(schemaIdentity.getSchemaVersionMinor());

		QueryParams queryParams = queryParamBuilder.build();

		List<SchemaInfo> schemaInfoList = schemaInfoStore.getSchemaInfoList(queryParams, headers.getPartitionId());

		if(null == schemaInfoList || schemaInfoList.isEmpty())
			return schemaInfoArr;

		SchemaComparatorByVersion schemaComparatorByVersion = new SchemaComparatorByVersion();
		Collections.sort(schemaInfoList, schemaComparatorByVersion);

		SchemaInfo smaller = null,bigger =null; 

		for(SchemaInfo element : schemaInfoList) {

			if(schemaComparatorByVersion.compare(element, schemaInfo) < 0) {
				smaller = element;
			}else if (schemaComparatorByVersion.compare(element, schemaInfo) > 0) {
				bigger = element;
				break;
			}
		}

		schemaInfoArr[0]=smaller;
		schemaInfoArr[1]=bigger;

		return schemaInfoArr;
	}



}
