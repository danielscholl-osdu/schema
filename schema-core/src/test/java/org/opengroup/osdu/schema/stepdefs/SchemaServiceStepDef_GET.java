package org.opengroup.osdu.schema.stepdefs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opengroup.osdu.schema.constants.TestConstants;
import org.opengroup.osdu.schema.stepdefs.model.HttpRequest;
import org.opengroup.osdu.schema.stepdefs.model.HttpResponse;
import org.opengroup.osdu.schema.stepdefs.model.SchemaServiceScope;
import org.opengroup.osdu.schema.util.AuthUtil;
import org.opengroup.osdu.schema.util.HttpClientFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

import io.cucumber.java8.En;

public class SchemaServiceStepDef_GET implements En {

	@Inject
	private SchemaServiceScope context;

	static String[] GetListBaseFilterArray;
	static String[] GetListVersionFilterArray;
	String queryParameter;

	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	List<HashMap<String, String>> list_schemaParameterMap = new ArrayList<HashMap<String, String>>();

	public SchemaServiceStepDef_GET() {

		Given("I generate user token and set request headers", () -> {
			if (this.context.getToken() == null) {
				String token = new AuthUtil().getToken();
				this.context.setToken(token);
			}

			if (this.context.getAuthHeaders() == null) {
				Map<String, String> authHeaders = new HashMap<String, String>();
				authHeaders.put(TestConstants.AUTHORIZATION, this.context.getToken());
				authHeaders.put(TestConstants.DATA_PARTITION_ID, TestConstants.TENANT);
				authHeaders.put(TestConstants.CONTENT_TYPE, TestConstants.JSON_CONTENT);
				this.context.setAuthHeaders(authHeaders);
			}
		});

		Given("I hit schema service GET List API with {string} , {string} , {string}",
				(String parameter, String parameterVal, String latestVersion) -> {
					Map<String, String> queryParams = new HashMap<String, String>();
					queryParams.put(parameter, parameterVal);
					queryParams.put("latestVersion", latestVersion);
					HttpRequest httpRequest = HttpRequest.builder()
							.url(TestConstants.HOST + TestConstants.GET_LIST_ENDPOINT).queryParams(queryParams)
							.httpMethod(HttpRequest.GET).requestHeaders(this.context.getAuthHeaders()).build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
					assertEquals("200", String.valueOf(response.getCode()));
					LOGGER.log(Level.INFO, "resp - " + response.toString());
					verifyGetListResponse(parameter, parameterVal);
				});

		Given("I hit schema service GET List API with {string} , {string} , {string} , {string} , {string}",
				(String parameter, String parameterVal, String latestVersion, String parameter2,
						String statusValue) -> {
					Map<String, String> queryParams = new HashMap<String, String>();
					queryParams.put(parameter, parameterVal);
					queryParams.put("latestVersion", latestVersion);
					queryParams.put(parameter2, statusValue);
					HttpRequest httpRequest = HttpRequest.builder()
							.url(TestConstants.HOST + TestConstants.GET_LIST_ENDPOINT).queryParams(queryParams)
							.httpMethod(HttpRequest.GET).requestHeaders(this.context.getAuthHeaders()).build();
					HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
					this.context.setHttpResponse(response);
					LOGGER.log(Level.INFO, "resp - " + response.toString());
				});

		Then("get service should respond back with schemaInfo list from private as well as shared scope matching {string} and {string}",
				(String parameter, String parameterVal) -> {
					verifyGetListResponse(parameter, parameterVal);
				});

		Given("I hit schema service GET API with {string}", (String schemaId) -> {
			HttpRequest httpRequest = HttpRequest.builder()
					.url(TestConstants.HOST + TestConstants.GET_ENDPOINT + schemaId).httpMethod(HttpRequest.GET)
					.requestHeaders(this.context.getAuthHeaders()).build();
			HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
			this.context.setHttpResponse(response);
		});

		Then("service should respond back with success {string} and response {string}",
				(String ReponseStatusCode, String SchemaToBeVerified) -> {
					HttpResponse response = this.context.getHttpResponse();
					assertEquals(ReponseStatusCode, String.valueOf(response.getCode()));
					String body = this.context.getFileUtils().read(SchemaToBeVerified);
					Gson gsn = new Gson();
					JsonObject expectedData = gsn.fromJson(body, JsonObject.class);
					JsonObject responseMsg = gsn.fromJson(response.getBody().toString(), JsonObject.class);
					assertEquals(expectedData.get("schema").toString(), responseMsg.get("schema").toString());
				});

		Then("I GET udapted Schema", () -> {
			HttpRequest httpRequest = HttpRequest.builder()
					.url(TestConstants.HOST + TestConstants.GET_ENDPOINT + this.context.getSchemaIdFromInputPayload())
					.httpMethod(HttpRequest.GET).requestHeaders(this.context.getAuthHeaders()).build();
			HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
			this.context.setHttpResponse(response);
		});

		Then("I GET udapted Schema with {string}", (String ReponseStatusCode) -> {
			HttpRequest httpRequest = HttpRequest.builder()
					.url(TestConstants.HOST + TestConstants.GET_ENDPOINT + this.context.getSchemaIdFromInputPayload())
					.httpMethod(HttpRequest.GET).requestHeaders(this.context.getAuthHeaders()).build();
			HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
			this.context.setHttpResponse(response);
			assertEquals(ReponseStatusCode, String.valueOf(response.getCode()));
		});

		Then("get schema service should respond back with {string} and {string}",
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

	private void verifyGetListResponse(String parameter, String parameterVal) throws IOException {
		prepareSchemaParameterMapList();
		verifySchemaInfoResponse(parameter, parameterVal);
	}

	public void prepareSchemaParameterMapList() throws IOException {
		String response = this.context.getHttpResponse().getBody();
		Gson gsn = new Gson();
		JsonObject schemaInfosList = gsn.fromJson(response, JsonObject.class);
		JsonArray root = (JsonArray) schemaInfosList.get("schemaInfos");
		for (JsonElement eachSchemaInfo : root) {
			Map<String, String> schemaIdentityMap = new HashMap<String, String>();

			JsonObject schemaIdentity_ForEachSchemaStatus = (JsonObject) (eachSchemaInfo.getAsJsonObject());

			this.context.setStatus(schemaIdentity_ForEachSchemaStatus.get("status").getAsString());

			JsonObject schemaIdentity_ForEachSchemaInfo = (JsonObject) eachSchemaInfo.getAsJsonObject()
					.get("schemaIdentity");
			schemaIdentityMap.put("authority", schemaIdentity_ForEachSchemaInfo.get("authority").getAsString());
			schemaIdentityMap.put("source", schemaIdentity_ForEachSchemaInfo.get("source").getAsString());
			schemaIdentityMap.put("entityType", schemaIdentity_ForEachSchemaInfo.get("entityType").getAsString());
			schemaIdentityMap.put("schemaVersionMajor",
					schemaIdentity_ForEachSchemaInfo.get("schemaVersionMajor").getAsString());

			this.context
					.setSchemaVersionMajor(schemaIdentity_ForEachSchemaInfo.get("schemaVersionMajor").getAsString());
			schemaIdentityMap.put("schemaVersionMinor",
					schemaIdentity_ForEachSchemaInfo.get("schemaVersionMinor").getAsString());
			this.context
					.setSchemaVersionMinor(schemaIdentity_ForEachSchemaInfo.get("schemaVersionMinor").getAsString());
			schemaIdentityMap.put("schemaVersionPatch",
					schemaIdentity_ForEachSchemaInfo.get("schemaVersionPatch").getAsString());
			schemaIdentityMap.put("scope", eachSchemaInfo.getAsJsonObject().get("scope").getAsString());
			schemaIdentityMap.put("status", eachSchemaInfo.getAsJsonObject().get("status").getAsString());
			this.list_schemaParameterMap.add((HashMap<String, String>) schemaIdentityMap);
		}
		LOGGER.log(Level.INFO, "SchemaParameterMapList - " + this.list_schemaParameterMap.toString());
	}

	private void verifySchemaInfoResponse(String parameterName, String parameterVal) {
		for (HashMap<String, String> schemaInfoMap : this.list_schemaParameterMap) {
			assertEquals(
					"Response schemaInfoList contains schemaInfo not matching parameter criteria - " + parameterName,
					parameterVal.toString(), schemaInfoMap.get(parameterName).toString());
		}
	}
}