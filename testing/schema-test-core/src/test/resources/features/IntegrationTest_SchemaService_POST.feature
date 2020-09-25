Feature: To verify functionality of POST schema Service

  ### Commons test steps are accomplished here
  Background: Common steps for all tests are executed
    Given I generate user token and set request headers for "TENANT1"
    Given I get latest schema with authority, source, entityType as "SchemaSanityTest", "testSource", "testEntity" respectively
    Given I hit schema service POST API with "/input_payloads/postInPrivateScope_positiveScenario.json" and data-partition-id as "TENANT1" only if status is not development

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API creates a empty private schema correctly.
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then service should respond back with <ReponseStatusCode> and <ResponseMessage>
    And schema service should respond back with <ReponseStatusCodeForGET> and <ResponseMessageforGET>

    Examples: 
      | InputPayload                                         | tenant    | ReponseStatusCode | ResponseMessage                                                    | ReponseStatusCodeForGET | ResponseMessageforGET                        |
      | "/input_payloads/postSchemaService_EmptySchema.json" | "TENANT1" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "200"                   | "/output_payloads/ResolvedSchema_Empty.json" |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API responds as bad request for wrong value of $ref attribute in schema input
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | InputPayload                                             | tenant    | ReponseStatusCode | ResponseMessage                                           |
      | "/input_payloads/postSchema_InvalidRefSchemaObject.json" | "TENANT1" | "400"             | "/output_payloads/PostSchema_InvalidRefSchemaObject.json" |
      | "/input_payloads/postSchema_RefNotResolvable.json"       | "TENANT1" | "400"             | "/output_payloads/PostSchema_RefNotResolvable.json"       |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API registers unique scehma only.
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant> without increasing any version
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | InputPayload                                                                                        | tenant    | ReponseStatusCode | ResponseMessage                                         |
      | "/input_payloads/PostTenant1ExistingSchema_duplicateSchemaService--Schema1--entity1--10-1-100.json" | "TENANT1" | "400"             | "/output_payloads/SchemaPost_DuplicateSchemaError.json" |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API validates input payload for JSON correctness
    Given I hit schema service POST API with <InputPayload> for input json validation
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | InputPayload                                         | ReponseStatusCode | ResponseMessage                                       |
      | "/input_payloads/inputPayloadWithIncorrectJSON.json" | "400"             | "/output_payloads/SchemaPost_IncorrectJsonError.json" |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API registers authority, source, entity and creates a private schema correctly with $ref attribute
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then service should respond back with <ReponseStatusCode> and <ResponseMessage> and scope whould be <responseScope>

    Examples: 
      | InputPayload                                                     | tenant    | ReponseStatusCode | ResponseMessage                                                    | responseScope |
      | "/input_payloads/postSchemaServiceWithRef_positiveScenario.json" | "TENANT1" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "INTERNAL"    |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API throws correct error if input payload is not valid
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | InputPayload                                                   | tenant    | ReponseStatusCode | ResponseMessage                                            |
      | "/input_payloads/postSchema_withEntityAttributeInPayload.json" | "TENANT1" | "400"             | "/output_payloads/PostSchema_EntityNotAllowedError.json"   |
      | "/input_payloads/postSchema_flattenedSchemaAsInput.json"       | "TENANT1" | "400"             | "/output_payloads/PostSchema_InvalidInputSchemaError.json" |

  @SchemaService
  Scenario Outline: Verify that Schema Service supersededBy functionality work correctly
    Given I hit schema service POST API for supersededBy with <InputPayload> and data-partition-id as <tenant>
    Then the post service for supersededBy should respond back with <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | InputPayload                                           | tenant    | ReponseStatusCode | ResponseMessage                                                    |
      | "/input_payloads/supercededInputPayload_positive.json" | "TENANT1" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" |

  @SchemaService
  Scenario Outline: Verify whether schema can not be registered with already existing major, but increased minor version
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Given I hit schema service POST API with <EmptyInputPayload> and data-partition-id as <tenant> with increased minor version only
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | EmptyInputPayload                                    | InputPayload                                           | ReponseStatusCode | ResponseMessage                                        | tenant    |
      | "/input_payloads/postSchemaService_EmptySchema.json" | "/input_payloads/inputPayloadWithExistingVersion.json" | "400"             | "/output_payloads/SchemaPost_BreakingChangeError.json" | "TENANT1" |
