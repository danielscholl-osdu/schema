package org.opengroup.osdu.schema.stepdefs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.opengroup.osdu.schema.constants.TestConstants;
import org.opengroup.osdu.schema.stepdefs.model.HttpRequest;
import org.opengroup.osdu.schema.stepdefs.model.HttpResponse;
import org.opengroup.osdu.schema.stepdefs.model.SchemaServiceScope;
import org.opengroup.osdu.schema.util.AuthUtil;
import org.opengroup.osdu.schema.util.HttpClientFactory;
import org.opengroup.osdu.schema.util.JsonUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

import io.cucumber.java8.En;

public class SchemaServiceStepDef_POST implements En {

	@Inject
	private SchemaServiceScope context;

	static String[] GetListBaseFilterArray;
	static String[] GetListVersionFilterArray;
	String queryParameter;

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	List<HashMap<String, String>> list_schemaParameterMap = new ArrayList<HashMap<String, String>>();

	public SchemaServiceStepDef_POST() {

		Given("I hit schema service POST API with {string} and data-partition-id as {string} only if status is not development",
				(String inputPayload, String tenant) -> {
					if (!"DEVELOPMENT".equals(context.getStatus())) {

						String body = this.context.getFileUtils().read(inputPayload);
						JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
						int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
						int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
						int nextMinorVersion = currentMinorVersion + 1;
						int nextMajorVersion = currentMajorVersion + 1;
						String id = "SchemaSanityTest:testSource:testEntity:" + nextMajorVersion + "."
								+ nextMinorVersion + ".0";
						updateVersionInJsonBody(jsonBody, nextMinorVersion, nextMajorVersion, id);
						body = new Gson().toJson(jsonBody);
						this.context.setSchemaIdFromInputPayload(id);
						this.context.setSchemaFromInputPayload(
								jsonBody.getAsJsonObject().get(TestConstants.SCHEMA).toString());
						this.context.setJsonPayloadForPostPUT(jsonBody.toString());
						Map<String, String> headers = this.context.getAuthHeaders();
						headers.put(TestConstants.DATA_PARTITION_ID, tenant);
						this.context.setAuthHeaders(headers);

						HttpRequest httpRequest = HttpRequest.builder()
								.url(TestConstants.HOST + TestConstants.POST_ENDPOINT).body(jsonBody.toString())
								.httpMethod(HttpRequest.POST).requestHeaders(headers).build();

						HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
						assertEquals("201", String.valueOf(response.getCode()));
						this.context.setHttpResponse(response);
						prepareSchemaParameterMapList();
					}

				});

		Given("I hit schema service POST API with {string} and data-partition-id as {string}",
				(String inputPayload, String tenant) -> {
					int nextMinorVersion = 0;
					int nextMajorVersion = 0;
					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					String statusNew = context.getStatus();
					nextMinorVersion = currentMinorVersion + 1;
					nextMajorVersion = currentMajorVersion + 1;
					String id = "SchemaSanityTest:testSource:testEntity:" + nextMajorVersion + "." + nextMinorVersion
							+ ".0";
					updateVersionInJsonBody(jsonBody, nextMinorVersion, nextMajorVersion, id);
					body = new Gson().toJson(jsonBody);
					this.context.setSchemaIdFromInputPayload(id);
					this.context
							.setSchemaFromInputPayload(jsonBody.getAsJsonObject().get(TestConstants.SCHEMA).toString());
					this.context.setJsonPayloadForPostPUT(jsonBody.toString());
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					this.context.setAuthHeaders(headers);
					HttpRequest httpRequest = HttpRequest.builder()
							.url(TestConstants.HOST + TestConstants.POST_ENDPOINT).body(jsonBody.toString())
							.httpMethod(HttpRequest.POST).requestHeaders(headers).build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
				});
		Then("post service should respond back with {string} and {string}",
				(String ReponseStatusCode, String ResponseMessage) -> {
					String body = this.context.getFileUtils().read(ResponseMessage);
					JsonObject jsonBody = new Gson().fromJson(body, JsonObject.class);
					HttpResponse response = this.context.getHttpResponse();
					if (response != null) {
						assertEquals(ReponseStatusCode, String.valueOf(response.getCode()));

						assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.AUTHORITY),
								getResponseValue(
										TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.AUTHORITY));

						assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.SOURCE),
								getResponseValue(
										TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.SOURCE));

						assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.ENTITY),
								getResponseValue(
										TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.ENTITY));

						assertEquals(getExpectedValue(jsonBody, null, TestConstants.STATUS),
								getResponseValue(TestConstants.STATUS));

						assertEquals(getExpectedValue(jsonBody, null, TestConstants.SCOPE),
								getResponseValue(TestConstants.SCOPE));

						Assert.assertNotNull(jsonBody.get(TestConstants.DATE_CREATED));
						Assert.assertNotNull(jsonBody.get(TestConstants.CREATED_BY));
					}
				});

		Then("post service should respond back with error {string} and {string}",
				(String ReponseStatusCode, String ResponseToBeVerified) -> {
					HttpResponse response = this.context.getHttpResponse();
					assertEquals(ReponseStatusCode, String.valueOf(response.getCode()));
					String body = this.context.getFileUtils().read(ResponseToBeVerified);
					Gson gsn = new Gson();
					JsonObject expectedData = gsn.fromJson(body, JsonObject.class);
					JsonObject responseMsg = gsn.fromJson(response.getBody().toString(), JsonObject.class);
					assertEquals(expectedData.toString(), responseMsg.toString());
				});
	}

	private void updateVersionInJsonBody(JsonElement jsonBody, int nextMinorVersion, int nextMajorVersion, String id) {
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
				.remove("schemaVersionMinor");
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
				.addProperty("schemaVersionMinor", nextMinorVersion);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
				.remove("schemaVersionMajor");
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
				.addProperty("schemaVersionMajor", nextMajorVersion);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject().remove("id");
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
				.addProperty("id", id);
	}

	private String getExpectedValue(JsonObject jsonBody, String parentAttributeValue, String valueToBeRetrieved) {
		String value;
		if (parentAttributeValue == null) {
			value = jsonBody.get(valueToBeRetrieved).toString();
			return value.substring(1, value.length() - 1);
		} else {
			value = jsonBody.getAsJsonObject(parentAttributeValue).get(valueToBeRetrieved).toString();
			if (Character.isDigit(value.charAt(0))) {
				return value;
			} else {
				return value.substring(1, value.length() - 1);
			}
		}
	}

	private String getResponseValue(String responseAttribute) {
		JsonUtils.getAsJsonPath(this.context.getHttpResponse().getBody().toString()).get(responseAttribute);
		return JsonUtils.getAsJsonPath(this.context.getHttpResponse().getBody().toString()).get(responseAttribute)
				.toString();
	}

	public void prepareSchemaParameterMapList() throws IOException {
		String response = this.context.getHttpResponse().getBody();
		Gson gsn = new Gson();
		JsonObject root = gsn.fromJson(response, JsonObject.class);
		JsonObject schemaIdentity_ForEachSchemaInfo = (JsonObject) root.get("schemaIdentity");
		this.context.setSchemaVersionMajor(schemaIdentity_ForEachSchemaInfo.get("schemaVersionMajor").getAsString());
		this.context.setSchemaVersionMinor(schemaIdentity_ForEachSchemaInfo.get("schemaVersionMinor").getAsString());
	}
}