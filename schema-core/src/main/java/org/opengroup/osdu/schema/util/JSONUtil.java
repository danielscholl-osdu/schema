package org.opengroup.osdu.schema.util;

import java.io.StringReader;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonPatch;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.constants.SchemaConstants.CompositionTags;
import org.opengroup.osdu.schema.constants.SchemaConstants.SkipTags;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JSONUtil {

	private EnumSet<SkipTags> skipTags = EnumSet.allOf(SkipTags.class);
	private EnumSet<CompositionTags> compositionTags = EnumSet.allOf(CompositionTags.class);
	
	public String getCleanJSON(String inputJSON) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(inputJSON);
		clean(root, "");
		
		return root.toString();
	}
	private boolean checkIfParentIsCompositionTag(String path) {
		String [] tags = path.split("/");
		if(tags.length < 3)
			return false;
		boolean isCompositionTag = compositionTags.stream().anyMatch(tag -> tag.getValue().equals(tags[tags.length-2]));
		return isCompositionTag;
	}
	
	public  JsonPatch findJSONDiff(JsonValue source, JsonValue target) {
		JsonPatch diff = Json.createDiff(source.asJsonObject(), target.asJsonObject());
		return diff;

	}
	
	public  JsonArray getJsonArrayFromGivenPath(String jsonString, String rootPath) {
		JsonReader reader = Json.createReader(new StringReader(jsonString));
		JsonStructure jsonStruct = reader.read();
		JsonPointer jp = Json.createPointer(rootPath); 
		JsonValue oldJsonVal = jp.getValue(jsonStruct);
		return oldJsonVal.asJsonArray();
	}
	
	private void clean(JsonNode node, String path) {
		if (node.isObject()) {
			ObjectNode object = (ObjectNode) node;
			Iterator<Entry<String, JsonNode>> itr = object.fields();
			while(itr.hasNext()) {
				
				Entry<String, JsonNode> entry = itr.next();
				String key = entry.getKey();
				if(skipTags.stream().anyMatch(tag -> tag.getValue().equals(key))
						|| entry.getKey().startsWith(SchemaConstants.XOSDU_TAG)) {
					//Don't delete the title tag of elements inside composition tag
					//This will be utilized later for comparision
					if(SchemaConstants.TITLE_TAG.equals(key)
							&& checkIfParentIsCompositionTag(path)){
						continue;
					}
					itr.remove();
				}
				clean(entry.getValue(), path + "/" + entry.getKey());
			}
		} else if (node.isArray()) {
			ArrayNode array = (ArrayNode) node;
			Iterator<JsonNode> itr = array.elements();
			AtomicInteger counter = new AtomicInteger();
			while(itr.hasNext()) {
				JsonNode entry = itr.next();
				if(skipTags.stream().anyMatch(tag -> tag.getValue().equals(entry))
						|| entry.asText().startsWith(SchemaConstants.XOSDU_TAG)) {
					itr.remove();
				}
				clean(entry, path + "/" + counter.getAndIncrement());
			}
		}
	}
}
