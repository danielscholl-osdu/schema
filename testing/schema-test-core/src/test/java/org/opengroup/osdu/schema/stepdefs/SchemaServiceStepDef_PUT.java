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

public class SchemaServiceStepDef_PUT implements En {

	@Inject
	private SchemaServiceScope context;

	static String[] GetListBaseFilterArray;
	static String[] GetListVersionFilterArray;
	String queryParameter;

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	List<HashMap<String, String>> list_schemaParameterMap = new ArrayList<HashMap<String, String>>();

	public SchemaServiceStepDef_PUT() {

		Given("I hit schema service PUT API with {string}, data-partition-id as {string} and mark schema as {string}.",
				(String inputPayload, String tenant, String status) -> {
					tenant = selectTenant(tenant);
					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					String id = "SchemaSanityTest:testSource:testEntity:" + currentMajorVersion + "."
							+ currentMinorVersion + ".0";
					updateVersionInJsonBody(jsonBody, currentMinorVersion, currentMajorVersion, id);
					this.context.setSchemaIdFromInputPayload(id);
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject().remove("status");
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject().addProperty("status",
							status);
					body = new Gson().toJson(jsonBody);
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					HttpRequest httpRequest = HttpRequest.builder().url(TestConstants.HOST + TestConstants.PUT_ENDPOINT)
							.body(body).httpMethod(HttpRequest.PUT).requestHeaders(this.context.getAuthHeaders())
							.build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
				});

		Given("I hit schema service PUT API with {string}, data-partition-id as {string} and mark schema as {string} for next major version",
				(String inputPayload, String tenant, String status) -> {
					tenant = selectTenant(tenant);
					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					currentMajorVersion = currentMajorVersion + 1;
					String id = "SchemaSanityTest:testSource:testEntity:" + currentMajorVersion + "."
							+ currentMinorVersion + ".0";
					updateVersionInJsonBody(jsonBody, currentMinorVersion, currentMajorVersion, id);
					this.context.setSchemaIdFromInputPayload(id);
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject().remove("status");
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject().addProperty("status",
							status);
					body = new Gson().toJson(jsonBody);
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					HttpRequest httpRequest = HttpRequest.builder().url(TestConstants.HOST + TestConstants.PUT_ENDPOINT)
							.body(body).httpMethod(HttpRequest.PUT).requestHeaders(this.context.getAuthHeaders())
							.build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
				});

		Given("I hit schema service PUT API with {string}, data-partition-id as {string}",
				(String inputPayload, String tenant) -> {
					tenant = selectTenant(tenant);
					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					String id = "SchemaSanityTest:testSource:testEntity:" + currentMajorVersion + "."
							+ currentMinorVersion + ".0";
					updateVersionInJsonBody(jsonBody, currentMinorVersion, currentMajorVersion, id);
					this.context.setSchemaIdFromInputPayload(id);
					body = new Gson().toJson(jsonBody);
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					HttpRequest httpRequest = HttpRequest.builder().url(TestConstants.HOST + TestConstants.PUT_ENDPOINT)
							.body(body).httpMethod(HttpRequest.PUT).requestHeaders(this.context.getAuthHeaders())
							.build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
				});

		Given("I hit schema service PUT API with {string}, data-partition-id as {string} for superceded input",
				(String inputPayload, String tenant) -> {
					tenant = selectTenant(tenant);
					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					String id = "SchemaSanityTest:testSource:testEntity:" + currentMajorVersion + "."
							+ currentMinorVersion + ".0";
					String supersededById = jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("supersededBy")
							.getAsJsonObject().get("id").getAsString();
					updateVersionInJsonBody(jsonBody, currentMinorVersion, currentMajorVersion, id);
					this.context.setSchemaIdFromInputPayload(id);
					this.context.setSupersededById(supersededById);
					body = new Gson().toJson(jsonBody);
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					HttpRequest httpRequest = HttpRequest.builder().url(TestConstants.HOST + TestConstants.PUT_ENDPOINT)
							.body(body).httpMethod(HttpRequest.PUT).requestHeaders(this.context.getAuthHeaders())
							.build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
				});

		Given("I hit schema service PUT API with {string}, data-partition-id as {string} with increased minor version only",
				(String inputPayload, String tenant) -> {
					tenant = selectTenant(tenant);
					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					currentMinorVersion = currentMinorVersion + 1;
					String id = "SchemaSanityTest:testSource:testEntity:" + currentMajorVersion + "."
							+ currentMinorVersion + ".0";
					updateVersionInJsonBody(jsonBody, currentMinorVersion, currentMajorVersion, id);
					this.context.setSchemaIdFromInputPayload(id);
					body = new Gson().toJson(jsonBody);
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					HttpRequest httpRequest = HttpRequest.builder().url(TestConstants.HOST + TestConstants.PUT_ENDPOINT)
							.body(body).httpMethod(HttpRequest.PUT).requestHeaders(this.context.getAuthHeaders())
							.build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
				});

		Given("I hit schema service PUT API with {string}, data-partition-id as {string} with different entityType",
				(String inputPayload, String tenant) -> {
					tenant = selectTenant(tenant);
					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					String id = "SchemaSanityTest:testSource:testEntity:" + currentMajorVersion + "."
							+ currentMinorVersion + ".0";
					updateVersionInJsonBody(jsonBody, currentMinorVersion, currentMajorVersion, id);
					this.context.setSchemaIdFromInputPayload(id);
					int randomNum = (int) (Math.random() * 10000);
					String entityVal = "testEntity" + randomNum;
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
							.remove("entityType");
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
							.addProperty("entityType", entityVal);
					body = new Gson().toJson(jsonBody);
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					HttpRequest httpRequest = HttpRequest.builder().url(TestConstants.HOST + TestConstants.PUT_ENDPOINT)
							.body(body).httpMethod(HttpRequest.PUT).requestHeaders(this.context.getAuthHeaders())
							.build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
				});

		Given("I hit schema service PUT API with {string}, data-partition-id as {string} with next major version",
				(String inputPayload, String tenant) -> {
					tenant = selectTenant(tenant);
					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					currentMajorVersion = currentMajorVersion + 1;
					String id = "SchemaSanityTest:testSource:testEntity:" + currentMajorVersion + "."
							+ currentMinorVersion + ".0";
					updateVersionInJsonBody(jsonBody, currentMinorVersion, currentMajorVersion, id);
					this.context.setSchemaIdFromInputPayload(id);
					body = new Gson().toJson(jsonBody);
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					HttpRequest httpRequest = HttpRequest.builder().url(TestConstants.HOST + TestConstants.PUT_ENDPOINT)
							.body(body).httpMethod(HttpRequest.PUT).requestHeaders(this.context.getAuthHeaders())
							.build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
				});

		Then("put schema service should respond back with {string}", (String ReponseStatusCode) -> {
			HttpResponse response = this.context.getHttpResponse();
			assertEquals(ReponseStatusCode, String.valueOf(response.getCode()));
		});

		Then("the put service for supersededBy should respond back with {string} and {string}",
				(String ReponseStatusCode, String ResponseMessage) -> {
					String body = this.context.getFileUtils().read(ResponseMessage);
					JsonObject jsonBody = new Gson().fromJson(body, JsonObject.class);
					HttpResponse response = this.context.getHttpResponse();
					if (response != null) {
						assertEquals(ReponseStatusCode, String.valueOf(response.getCode()));
						commonAssertion(response, jsonBody);
						Assert.assertNotNull(getResponseValue(TestConstants.SUPERSEDED_BY));
						assertEquals(
								getResponseValue(TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.ID),
								this.context.getSchemaIdFromInputPayload());
						assertEquals(
								getResponseValue(TestConstants.SUPERSEDED_BY + TestConstants.DOT + TestConstants.ID),
								this.context.getSupersededById());
					}
				});

		Given("I hit schema service PUT API for supersededBy with {string} and data-partition-id as {string}",
				(String inputPayload, String tenant) -> {
					tenant = selectTenant(tenant);
					String body = this.context.getFileUtils().read(inputPayload);

					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					int currentPatchVersion = Integer.parseInt(this.context.getSchemaVersionPatch());
					String supersededById = "SchemaSanityTest:testSource:testEntity:" + currentMajorVersion + "."
							+ currentMinorVersion + "." + currentPatchVersion;
					updateSupersededByInJsonBody(jsonBody, supersededById, currentMajorVersion, currentMinorVersion,
							currentPatchVersion);
					this.context.setSupersededById(supersededById);
					int nextMinorVersion = currentMinorVersion + 1;
					int nextMajorVersion = currentMajorVersion + 1;
					String schemaId = "SchemaSanityTest:testSource:testEntity:" + nextMajorVersion + "."
							+ nextMinorVersion + ".0";
					this.context.setSchemaIdFromInputPayload(schemaId);
					updateVersionInJsonBody(jsonBody, nextMinorVersion, nextMajorVersion, schemaId);
					body = new Gson().toJson(jsonBody);
					Map<String, String> headers = this.context.getAuthHeaders();
					headers.put(TestConstants.DATA_PARTITION_ID, tenant);
					HttpRequest httpRequest = HttpRequest.builder().url(TestConstants.HOST + TestConstants.PUT_ENDPOINT)
							.body(body).httpMethod(HttpRequest.PUT).requestHeaders(this.context.getAuthHeaders())
							.build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
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

	public void prepareSchemaParameterMapList() throws IOException {
		String response = this.context.getHttpResponse().getBody();
		Gson gsn = new Gson();
		JsonObject root = gsn.fromJson(response, JsonObject.class);
		JsonObject schemaIdentity_ForEachSchemaInfo = (JsonObject) root.get("schemaIdentity");
		this.context.setSchemaVersionMajor(schemaIdentity_ForEachSchemaInfo.get("schemaVersionMajor").getAsString());
		this.context.setSchemaVersionMinor(schemaIdentity_ForEachSchemaInfo.get("schemaVersionMinor").getAsString());
	}

	private void commonAssertion(HttpResponse response, JsonObject jsonBody) {

		assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.AUTHORITY),
				getResponseValue(TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.AUTHORITY));

		assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.SOURCE),
				getResponseValue(TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.SOURCE));

		assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.ENTITY),
				getResponseValue(TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.ENTITY));

		Assert.assertNotNull(jsonBody.get(TestConstants.DATE_CREATED));
		Assert.assertNotNull(jsonBody.get(TestConstants.CREATED_BY));
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
		return JsonUtils.getAsJsonPath(this.context.getHttpResponse().getBody().toString()).get(responseAttribute)
				.toString();
	}

	private void updateSupersededByInJsonBody(JsonElement jsonBody, String id, int majorVersion, int minorVersion,
			int patchVersion) {

		JsonElement supersededByBody = new Gson().fromJson(jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo")
				.getAsJsonObject(TestConstants.SCHEMA_IDENTITY).toString(), JsonElement.class);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").add(TestConstants.SUPERSEDED_BY, supersededByBody);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject(TestConstants.SUPERSEDED_BY)
				.remove(TestConstants.ID);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject(TestConstants.SUPERSEDED_BY)
				.addProperty(TestConstants.ID, id);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject(TestConstants.SUPERSEDED_BY)
				.remove(TestConstants.SCHEMA_MAJOR_VERSION);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject(TestConstants.SUPERSEDED_BY)
				.addProperty(TestConstants.SCHEMA_MAJOR_VERSION, majorVersion);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject(TestConstants.SUPERSEDED_BY)
				.remove(TestConstants.SCHEMA_MINOR_VERSION);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject(TestConstants.SUPERSEDED_BY)
				.addProperty(TestConstants.SCHEMA_MINOR_VERSION, minorVersion);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject(TestConstants.SUPERSEDED_BY)
				.remove(TestConstants.SCHEMA_PATCH_VERSION);
		jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject(TestConstants.SUPERSEDED_BY)
				.addProperty(TestConstants.SCHEMA_PATCH_VERSION, patchVersion);
	}

	private String selectTenant(String tenant) {

		switch (tenant) {
		case "TENANT1":
			tenant = TestConstants.PRIVATE_TENANT1;
			break;
		case "TENANT2":
			tenant = TestConstants.PRIVATE_TENANT2;
			break;
		case "COMMON":
			tenant = TestConstants.SHARED_TENANT;
			break;
		default:
			System.out.println("Invalid tenant");
		}
		return tenant;
	}
}