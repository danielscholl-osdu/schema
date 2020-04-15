Feature: To verify functionality of schema Service

  ### Commons test steps are accomplished here
  Background: Common steps for all tests are executed
    Given I generate user token and set request headers

  ### Schema Service Postive test Scenarios covering fucntionalities of Get List and Post operation. To be added are Put and Get Schema functionalities
  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API registers authority, source, entity and creates a private schema correctly.
    Given I hit schema service GET List API with <parameter> , <value> , <latestVersion>
    Then service should respond back with schemaInfo list from private as well as shared scope matching <parameter> and <value>
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    And service should respond back with <ReponseStatusCode> and <ResponseMessage>
    And I hit schema service PUT API with <InputPayload>, data-partition-id as <tenant> and mark schema as absolete.
    Then schema service should respond back with <ReponseStatusCodeForPUT>
    And I GET udapted Schema
    Then schema service should respond back with <ReponseStatusCodeForGET> and <ResponseMessageforGET>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                               | tenant    | ReponseStatusCode | ResponseMessage                                                    | ReponseStatusCodeForPUT | ReponseStatusCodeForGET | ResponseMessageforGET                  |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postInPrivateScope_positiveScenario.json" | "opendes" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "204"                   | "200"                   | "/output_payloads/ResolvedSchema.json" |
