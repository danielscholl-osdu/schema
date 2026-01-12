Feature: To verify version info endpoint content

  @SchemaService
  Scenario: Verify version info endpoint content
    Given I send get request without a token to version info endpoint
    Then service should respond back with version info in response
