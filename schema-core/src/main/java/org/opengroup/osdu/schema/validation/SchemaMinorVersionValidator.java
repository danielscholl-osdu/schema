package org.opengroup.osdu.schema.validation;


import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.json.JsonPointer;
import javax.json.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.SchemaVersionException;
import org.opengroup.osdu.schema.util.JSONUtil;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchemaMinorVersionValidator implements SchemaVersionValidator{

	private final JaxRsDpsLog log;

	private final JSONUtil jsonUtil;

	@Override
	public void validate(String oldSchema, String newSchema) throws SchemaVersionException, ApplicationException{

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
		Set<String> processedArrayPath = new HashSet<>();

		log.info("Difference between two schemas found :"+diff);

		for(JsonValue jsonValue : diff.toJsonArray()) {

			JsonObject jsonObject = jsonValue.asJsonObject();
			String operation = jsonObject.getString("op");
			String path = jsonObject.getString("path");
			String attributeName = StringUtils.substringAfterLast(path, "/");

			log.info("Processing the path :"+path);
			log.info("Processing the Operation :"+operation);

			//Changing state of "additionalProperties" is only permitted when it was not present and added as true
			//or it is present as true but removed now
			if("additionalProperties".equals(attributeName)) {
				if("add".equals(operation) && "true".equals(jsonObject.get("value").toString())) {
					continue;
				}else if("remove".equals(operation)) {
					JsonPointer pointer = Json.createPointer(path);
					JsonValue originalValue = pointer.getValue(source.asJsonObject());
					if("true".equals(originalValue.toString()))
						continue;
				}

				log.error("Changing state of \"additionalProperties\" is not permitted. Path :"+path);
				throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR);
			}
			//Removing anything is not permitted
			if(path.contains("/allOf") || path.contains("/oneOf") || path.contains("/anyOf")) {
				String [] attributes = path.split("/");

				Optional<String> xxxOfAttrOpt = Arrays.stream(attributes)
						.filter(attr -> ("allOf".equals(attr) || "oneOf".equals(attr) || "anyOf".equals(attr)))
						.findFirst();

				if(operation.equals("remove") && attributeName.equals(xxxOfAttrOpt.get()))
					throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR);
				
				int lastIdx = path.indexOf(xxxOfAttrOpt.get());
				String rootPath = path.substring(0,	lastIdx+5);

				//Array is already validated
				if (processedArrayPath.contains(rootPath)) 
					continue;

				validateCompositionAttributesForMinorVersion(cleanSource, cleanTarget, rootPath);
				processedArrayPath.add(rootPath);
			}

			//Changing list of "required" is not permitted
			else if(path.contains("/required")) {
				int lastIdx = path.indexOf("required");
				String rootPath = path.substring(0,	lastIdx+8);

				//Array is already validated
				if (processedArrayPath.contains(rootPath)) 
					continue;

				validateRequiredAttribute(cleanSource, cleanTarget, rootPath);
				processedArrayPath.add(rootPath);
			}
			else if("type".endsWith(attributeName)) {
				//Remove operation is not permitted
				if(operation.equals("remove") || operation.equals("replace")) {
					log.error("Removing or changing type attribute is not permitted. Path :"+path);
					throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR);
				}
			}

			//Remove operation is not permitted
			else if(operation.equals("remove")) {
				log.error("Removing elements from a schema at Minor level not permitted. Path :"+path);
				throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR);
			}
		}
	}


	@Override
	public SchemaVersionValidatorType getType() {
		return SchemaVersionValidatorType.MINOR;
	}

	private  void validateCompositionAttributesForMinorVersion(String oldSchema, String newSchema, String path) throws SchemaVersionException, ApplicationException {
		JsonArray oldJsonArray = jsonUtil.getJsonArrayFromGivenPath(oldSchema, path);
		JsonArray newJsonArray = jsonUtil.getJsonArrayFromGivenPath(newSchema, path);


		try {
			if(oldJsonArray.size() > newJsonArray.size()) {
				throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR);
			}

			for(int i=0;i<oldJsonArray.size();i++) {
				String oldTitle=null;
				String ref=null;
				JsonValue oldValue = oldJsonArray.get(i);

				//Check Ref and Title attribute is present inside the array
				if(oldValue.asJsonObject().containsKey("title")) {
					oldTitle = oldValue.asJsonObject().get("title").toString();
				}
				if(oldValue.asJsonObject().containsKey("$ref")) {
					ref = oldValue.asJsonObject().get("$ref").toString();
				}

				//If both the elements are missing then compare the value at respective index
				if(null == oldTitle && null == ref) {
					validate(oldValue.toString(), newJsonArray.get(i).toString());
				}else {
					ListIterator<JsonValue> newItr = newJsonArray.listIterator();
					int count=0;
					String newTitle=null;
					while(newItr.hasNext()) {
						JsonValue newValue = newItr.next();
						if(newValue.asJsonObject().containsKey("title")) {
							newTitle = newValue.asJsonObject().get("title").toString();
						}

						//Skip if title block is not present
						if(StringUtils.isNoneBlank(oldTitle) && oldTitle.equals(newTitle) == false) {
							count++;
							continue;
						}
						validate(oldValue.toString(), newValue.toString());
						break;
					}

					//Element is not found in the entire Array
					if(count == newJsonArray.size()) {
						throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR);
					}
				}
			}
		}catch (AssertionError exc) {
			throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR);
		}

	}

	private  void validateRequiredAttribute(String oldSchema, String newSchema, String path) throws SchemaVersionException {

		JsonArray oldJsonArray = jsonUtil.getJsonArrayFromGivenPath(oldSchema, path);
		JsonArray newJsonArray = jsonUtil.getJsonArrayFromGivenPath(newSchema, path);

		if(false == compareRequiredArrays(oldJsonArray, newJsonArray)) {
			log.error("Changing list of \"required\" is not permitted. Path :"+path);
			throw new SchemaVersionException(SchemaConstants.BREAKING_CHANGES_MINOR);
		}

	}

	private  boolean compareRequiredArrays(JsonArray arr1, JsonArray arr2) {

		if(arr1.size() != arr2.size()) {
			log.error("Array length is not same.");
			return false;
		}

		return arr1.containsAll(arr1);
	}
}
