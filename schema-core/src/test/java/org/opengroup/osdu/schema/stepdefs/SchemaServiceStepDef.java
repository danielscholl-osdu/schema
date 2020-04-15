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

public class SchemaServiceStepDef implements En {

    @Inject
    private SchemaServiceScope context;

    static String[] GetListBaseFilterArray;
    static String[] GetListVersionFilterArray;
    String queryParameter;

    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    List<HashMap<String, String>> list_schemaParameterMap = new ArrayList<HashMap<String, String>>();

    public SchemaServiceStepDef() {

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
                    LOGGER.log(Level.INFO, "resp - " + response.toString());
                });

        Then("service should respond back with schemaInfo list from private as well as shared scope matching {string} and {string}",
                (String parameter, String parameterVal) -> {
                    verifyGetListResponse(parameter, parameterVal);
                });

        Given("I hit schema service POST API with {string} and data-partition-id as {string}",
                (String inputPayload, String tenant) -> {
                    String body = this.context.getFileUtils().read(inputPayload);
                    JsonElement jsonBody = new Gson().fromJson(body, JsonElement.class);
                    int currentMinorVersion = Integer.parseInt(this.context.getSchemaVersionMinor());
                    int nextVersion = currentMinorVersion + 1;
                    String id = "SchemaSanityTest:testSource:testEntity:1." + nextVersion + ".0";

                    jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
                            .remove("schemaVersionMinor");
                    jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
                            .addProperty("schemaVersionMinor", nextVersion);

                    jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
                            .remove("id");
                    jsonBody.getAsJsonObject().getAsJsonObject("schemaInfo").get("schemaIdentity").getAsJsonObject()
                            .addProperty("id", id);
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

        Then("service should respond back with {string} and {string}",
                (String ReponseStatusCode, String ResponseMessage) -> {
                    String body = this.context.getFileUtils().read(ResponseMessage);
                    JsonObject jsonBody = new Gson().fromJson(body, JsonObject.class);
                    HttpResponse response = this.context.getHttpResponse();
                    assertEquals(ReponseStatusCode, String.valueOf(response.getCode()));

                    assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.AUTHORITY),
                            getResponseValue(
                                    TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.AUTHORITY));

                    assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.SOURCE),
                            getResponseValue(TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.SOURCE));

                    assertEquals(getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY, TestConstants.ENTITY),
                            getResponseValue(TestConstants.SCHEMA_IDENTITY + TestConstants.DOT + TestConstants.ENTITY));

                    assertEquals(
                            getExpectedValue(jsonBody, TestConstants.SCHEMA_IDENTITY,
                                    TestConstants.SCHEMA_MAJOR_VERSION),
                            getResponseValue(TestConstants.SCHEMA_IDENTITY + TestConstants.DOT
                                    + TestConstants.SCHEMA_MAJOR_VERSION));

                    assertEquals(getExpectedValue(jsonBody, null, TestConstants.STATUS),
                            getResponseValue(TestConstants.STATUS));

                    assertEquals(getExpectedValue(jsonBody, null, TestConstants.SCOPE),
                            getResponseValue(TestConstants.SCOPE));

                    Assert.assertNotNull(jsonBody.get(TestConstants.DATE_CREATED));
                    Assert.assertNotNull(jsonBody.get(TestConstants.CREATED_BY));
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

        Given("I hit schema service PUT API with {string}, data-partition-id as {string} and mark schema as absolete.",
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

        Then("schema service should respond back with {string}", (String ReponseStatusCode) -> {
            HttpResponse response = this.context.getHttpResponse();
            assertEquals(ReponseStatusCode, String.valueOf(response.getCode()));
        });

        Then("I GET udapted Schema", () -> {
            HttpRequest httpRequest = HttpRequest.builder()
                    .url(TestConstants.HOST + TestConstants.GET_ENDPOINT + this.context.getSchemaIdFromInputPayload())
                    .httpMethod(HttpRequest.GET).requestHeaders(this.context.getAuthHeaders()).build();
            HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
            this.context.setHttpResponse(response);
        });

        Then("schema service should respond back with {string} and {string}",
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

    private void verifyGetListResponse(String parameter, String parameterVal) throws IOException {
        prepareSchemaParameterMapList();
        verifySchemaInfoResponse(parameter, parameterVal);
    }

    private void prepareSchemaParameterMapList() throws IOException {
        String response = this.context.getHttpResponse().getBody();
        Gson gsn = new Gson();
        JsonObject schemaInfosList = gsn.fromJson(response, JsonObject.class);
        JsonArray root = (JsonArray) schemaInfosList.get("schemaInfos");
        for (JsonElement eachSchemaInfo : root) {
            Map<String, String> schemaIdentityMap = new HashMap<String, String>();
            JsonObject schemaIdentity_ForEachSchemaInfo = (JsonObject) eachSchemaInfo.getAsJsonObject()
                    .get("schemaIdentity");
            schemaIdentityMap.put("authority", schemaIdentity_ForEachSchemaInfo.get("authority").getAsString());
            schemaIdentityMap.put("source", schemaIdentity_ForEachSchemaInfo.get("source").getAsString());
            schemaIdentityMap.put("entityType", schemaIdentity_ForEachSchemaInfo.get("entity").getAsString());
            schemaIdentityMap.put("schemaVersionMajor",
                    schemaIdentity_ForEachSchemaInfo.get("schemaVersionMajor").getAsString());
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