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

		Given("I hit schema service PUT API with {string}, data-partition-id as {string} and mark schema as osolete.",
				(String inputPayload, String tenant) -> {
					String body = this.context.getJsonPayloadForPostPUT();
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject().remove("status");
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject().addProperty("status",
							TestConstants.OBSOLETE);
					jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").getAsJsonObject().addProperty("scope",
							TestConstants.INTERNAL);
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

					String body = this.context.getFileUtils().read(inputPayload);
					JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
					int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
					int currentMajorVersion = Integer.parseInt(this.context.getSchemaVersionMajor());
					int nextMinorVersion = currentMinorVersion;
					int nextMajorVersion = currentMajorVersion;
					String id = "SchemaSanityTest:testSource:testEntity:" + nextMajorVersion + "." + nextMinorVersion
							+ ".0";
					updateVersionInJsonBody(jsonBody, nextMinorVersion, nextMajorVersion, id);
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
}