Feature: To verify functionality of POST schema Service

  ### Commons test steps are accomplished here
  Background: Common steps for all tests are executed
    Given I generate user token and set request headers
    Given I hit schema service GET List API with "authority" , "SchemaSanityTest" , "true"
    Given I hit schema service POST API with "/input_payloads/postInPrivateScope_positiveScenario.json" and data-partition-id as "opendes" only if status is not development

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API creates a empty private schema correctly.
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then post service should respond back with <ReponseStatusCode> and <ResponseMessage>
    And I GET udapted Schema with <ReponseStatusCodeForGET>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                         | tenant    | ReponseStatusCode | ResponseMessage                                                    | ReponseStatusCodeForGET | ResponseMessageforGET                        |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchemaService_EmptySchema.json" | "opendes" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "200"                   | "/output_payloads/ResolvedSchema_Empty.json" |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API responds as bad request for wrong value of $ref attribute in schema input
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then post service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                             | tenant    | ReponseStatusCode | ResponseMessage                                           |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchema_InvalidRefSchemaObject.json" | "opendes" | "400"             | "/output_payloads/PostSchema_InvalidRefSchemaObject.json" |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchema_RefNotResolvable.json"       | "opendes" | "400"             | "/output_payloads/PostSchema_RefNotResolvable.json"       |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API registers authority, source, entity and creates a private schema correctly with $ref attribute
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then post service should respond back with <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                                     | tenant    | ReponseStatusCode | ResponseMessage                                                    |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchemaServiceWithRef_positiveScenario.json" | "opendes" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" |

  @SchemaServiceTest
  Scenario Outline: Verify that Schema Service's POST API registers authority, source, entity and creates a private schema correctly.
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then post service should respond back with <ReponseStatusCode> and <ResponseMessage>
    Given I hit schema service PUT API with <InputPayload>, data-partition-id as <tenant> and mark schema as osolete.
    Then put schema service should respond back with <ReponseStatusCodeForPUT>
    Given I GET udapted Schema
    Then get schema service should respond back with <ReponseStatusCodeForGET> and <ResponseMessageforGET>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                               | tenant    | ReponseStatusCode | ResponseMessage                                                    | ReponseStatusCodeForPUT | ReponseStatusCodeForGET | ResponseMessageforGET                  |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postInPrivateScope_positiveScenario.json" | "opendes" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "204"                   | "200"                   | "/output_payloads/ResolvedSchema.json" |
