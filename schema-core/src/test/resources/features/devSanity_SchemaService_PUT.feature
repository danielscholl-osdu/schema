Feature: To verify functionality of PUT schema Service

  ### Commons test steps are accomplished here
  Background: Common steps for all tests are executed
    Given I generate user token and set request headers
    Given I hit schema service GET List API with "authority" , "SchemaSanityTest" , "true"
    Given I hit schema service POST API with "/input_payloads/postInPrivateScope_positiveScenario.json" and data-partition-id as "opendes" only if status is not development

  @SchemaService
  Scenario Outline: Verify that Schema Service's PUT API works correctly without scope field
    Given I hit schema service PUT API with <InputPayload>, data-partition-id as <tenant>
    Then put schema service should respond back with <ReponseStatusCodeForPUT>
    And I GET udapted Schema with <ReponseStatusCodeForGET>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                               | tenant    | ReponseStatusCode | ResponseMessage                                                    | ReponseStatusCodeForPUT | ReponseStatusCodeForGET | ResponseMessageforGET                  |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postInPrivateScope_positiveScenario.json" | "opendes" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "204"                   | "200"                   | "/output_payloads/ResolvedSchema.json" |

  @SchemaService
  Scenario Outline: Verify that Schema Service's PUT API works correctly and update schema properly
    Given I hit schema service PUT API with <InputPayload>, data-partition-id as <tenant>
    Then put schema service should respond back with <ReponseStatusCodeForPUT>
    When I hit schema service PUT API with <UpdatedInputPayload>, data-partition-id as <tenant>
    Then put schema service should respond back with <ReponseStatusCodeForPUT>
    When I GET udapted Schema
    Then get schema service should respond back with <ReponseStatusCodeForGET> and <ResponseMessageforGET>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                               | tenant    | ReponseStatusCode | ResponseMessage                                                    | ReponseStatusCodeForPUT | ReponseStatusCodeForGET | ResponseMessageforGET                         | UpdatedInputPayload                                      |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postInPrivateScope_positiveScenario.json" | "opendes" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "204"                   | "200"                   | "/output_payloads/UpdatedResolvedSchema.json" | "/input_payloads/putUpdatedSchema_positiveScenario.json" |
      