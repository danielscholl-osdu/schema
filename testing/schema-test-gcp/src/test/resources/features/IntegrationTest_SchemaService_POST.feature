Feature: To verify functionality of POST schema Service

  ### Commons test steps are accomplished here
  Background: Common steps for all tests are executed
    Given I generate user token and set request headers
    Given I hit schema service GET List API with "authority" , "SchemaSanityTest" , "true"
    Given I hit schema service POST API with "/input_payloads/postInPrivateScope_positiveScenario.json" and data-partition-id as "opendes" only if status is not development

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API creates a empty private schema correctly.
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then service should respond back with <ReponseStatusCode> and <ResponseMessage>
    And schema service should respond back with <ReponseStatusCodeForGET> and <ResponseMessageforGET>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                         | tenant    | ReponseStatusCode | ResponseMessage                                                    | ReponseStatusCodeForGET | ResponseMessageforGET                        |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchemaService_EmptySchema.json" | "opendes" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "200"                   | "/output_payloads/ResolvedSchema_Empty.json" |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API responds as bad request for wrong value of $ref attribute in schema input
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                             | tenant    | ReponseStatusCode | ResponseMessage                                           |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchema_InvalidRefSchemaObject.json" | "opendes" | "400"             | "/output_payloads/PostSchema_InvalidRefSchemaObject.json" |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchema_RefNotResolvable.json"       | "opendes" | "400"             | "/output_payloads/PostSchema_RefNotResolvable.json"       |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API registers unique scehma only.
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant> without increasing any version
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | InputPayload                                                                                        | tenant    | ReponseStatusCode | ResponseMessage                                         |
      | "/input_payloads/PostTenant1ExistingSchema_duplicateSchemaService--Schema1--entity1--10-1-100.json" | "opendes" | "400"             | "/output_payloads/SchemaPost_DuplicateSchemaError.json" |

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
    Then service should respond back with <ReponseStatusCode> and <ResponseMessage> and scope whould be <responceScope>
    #    And I GET updated schema
    #    And I get response <resourceNotFoundResponseCode> when I try to get schema from <otherTenant> other than from where it was ingested
    
    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                                     | tenant    | otherTenant | ReponseStatusCode | resourceNotFoundResponseCode | ResponseMessage                                                    | responceScope |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchemaServiceWithRef_positiveScenario.json" | "common"  | "opendes"   | "201"             | "404"                        | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "SHARED"      |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchemaServiceWithRef_positiveScenario.json" | "opendes" | "common"    | "201"             | "404"                        | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" | "INTERNAL"    |

  @SchemaService
  Scenario Outline: Verify that Schema Service's POST API throws correct error if input payload is not valid
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                                   | tenant    | ReponseStatusCode | ResponseMessage                                            |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchema_withEntityAttributeInPayload.json" | "opendes" | "400"             | "/output_payloads/PostSchema_EntityNotAllowedError.json"   |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/postSchema_flattenedSchemaAsInput.json"       | "opendes" | "400"             | "/output_payloads/PostSchema_InvalidInputSchemaError.json" |

  @SchemaService
  Scenario Outline: Verify that Schema Service supersededBy functionality work correctly
    Given I hit schema service POST API for supersededBy with <InputPayload> and data-partition-id as <tenant>
    Then the post service for supersededBy should respond back with <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | parameter   | value              | latestVersion | InputPayload                                                     | tenant    | ReponseStatusCode | ResponseMessage                                                    |
      | "authority" | "SchemaSanityTest" | "true"        | "/input_payloads/supercededInputPayload_positive.json" | "opendes" | "201"             | "/output_payloads/SchemaPost_PrivateScope_SuccessfulCreation.json" |

      
  @SchemaService
  Scenario Outline: Verify whether schema can not be registered with already existing major, but increased minor version
    Given I hit schema service POST API with <InputPayload> and data-partition-id as <tenant>
    Given I hit schema service POST API with <EmptyInputPayload> and data-partition-id as <tenant> with increased minor version only
    Then service should respond back with error <ReponseStatusCode> and <ResponseMessage>

    Examples: 
      | EmptyInputPayload                                    | InputPayload                                           | ReponseStatusCode | ResponseMessage                                        | tenant    |
      | "/input_payloads/postSchemaService_EmptySchema.json" | "/input_payloads/inputPayloadWithExistingVersion.json" | "400"             | "/output_payloads/SchemaPost_BreakingChangeError.json" | "opendes" |
