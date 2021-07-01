package org.opengroup.osdu.schema.validation;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.SchemaVersionException;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.util.JSONUtil;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.opengroup.osdu.schema.constants.SchemaConstants.SkipTags;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchemaPatchVersionValidator implements SchemaVersionValidator{

	private List<String> breakingChanges = new ArrayList<String>();
	
	private final ISchemaInfoStore schemaInfoStore;

	private final JaxRsDpsLog log;

	private final DpsHeaders headers;

	private final JSONUtil jsonUtil;
	
	private EnumSet<SkipTags> skipTags = EnumSet.allOf(SkipTags.class);
	
	@Override
	public SchemaVersionValidatorType getType() {
		return SchemaVersionValidatorType.PATCH;
	}

	@Override
	public void validate(String oldSchema, String newSchema) throws SchemaVersionException, ApplicationException {
		
		String cleanSource;
		String cleanTarget;
		try {
			cleanSource = jsonUtil.getCleanJSON(oldSchema);
			cleanTarget = jsonUtil.getCleanJSON(newSchema);
		} catch (JsonProcessingException e) {
			throw new ApplicationException("Failed to vaildate schemas.");
		}
		

		JsonValue source = Json.createReader(new StringReader(cleanSource)).readValue();
		JsonValue target = Json.createReader(new StringReader(cleanTarget)).readValue();

		JsonPatch diff = jsonUtil.findJSONDiff(source, target);

		for(JsonValue value: diff.toJsonArray()) {
			JsonObject jsonObject = value.asJsonObject();
			String path = jsonObject.getString("path");
			String attributeName = StringUtils.substringAfterLast(path, "/");

			if(skipTags.stream().anyMatch(tag -> tag.getValue().equals(attributeName))
					|| attributeName.startsWith(SchemaConstants.XOSDU_TAG)) 
				continue;

			log.error("Failed patch level validation for the attribute: "+attributeName);

			throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_PATCH);
		}
	}

}
