package org.opengroup.osdu.schema.stepdefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;
import io.cucumber.java8.En;
import org.opengroup.osdu.schema.constants.TestConstants;
import org.opengroup.osdu.schema.stepdefs.model.HttpRequest;
import org.opengroup.osdu.schema.stepdefs.model.HttpResponse;
import org.opengroup.osdu.schema.stepdefs.model.SchemaServiceScope;
import org.opengroup.osdu.schema.util.HttpClientFactory;
import org.opengroup.osdu.schema.util.VersionInfoUtils;

public class InfoStepDef_GET implements En {

  @Inject
  private SchemaServiceScope context;

  private VersionInfoUtils versionInfoUtil = new VersionInfoUtils();

  public InfoStepDef_GET() {

    Given("I send get request to version info endpoint", () -> {
      HttpRequest httpRequest = HttpRequest.builder()
          .url(TestConstants.HOST + TestConstants.GET_INFO_ENDPOINT)
          .httpMethod(HttpRequest.GET)
          .build();
      HttpResponse response = HttpClientFactory.getInstance().send(httpRequest);
      this.context.setHttpResponse(response);
    });

    Then("service should respond back with version info in response", () -> {
      assertEquals(200, this.context.getHttpResponse().getCode());

      VersionInfoUtils.VersionInfo responseObject = this.versionInfoUtil
          .getVersionInfoFromResponse(this.context.getHttpResponse());

      assertNotNull(responseObject.groupId);
      assertNotNull(responseObject.artifactId);
      assertNotNull(responseObject.version);
      assertNotNull(responseObject.buildTime);
      assertNotNull(responseObject.branch);
      assertNotNull(responseObject.commitId);
      assertNotNull(responseObject.commitMessage);
    });
  }
}
