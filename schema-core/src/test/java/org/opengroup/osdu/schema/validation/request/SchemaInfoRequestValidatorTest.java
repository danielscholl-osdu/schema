package org.opengroup.osdu.schema.validation.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaInfoRequestValidatorTest {

    @InjectMocks
    SchemaInfoRequestValidator schemaInfoRequestValidator;

    @Test
    public void validateRequestWithValidParametersShouldNotThrowException() {
        Set<String> validParams = new HashSet<>(Arrays.asList("authority", "source", "entityType", "schemaVersionMajor", "schemaVersionMinor", "schemaVersionPatch", "status", "scope", "latestVersion", "limit", "offset"));

        assertDoesNotThrow(() -> {
            schemaInfoRequestValidator.validateRequest(validParams);
        });	}

    @Test
    public void validateRequestWithInvalidParametersShouldThrowBadRequestException() {
        Set<String> invalidParams = new HashSet<>(Arrays.asList("invalidParam1", "invalidParam2"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            schemaInfoRequestValidator.validateRequest(invalidParams);
        });

        String expectedErrorForInvalidParam = "invalidParam1 is not a valid input param!";
        assertEquals(expectedErrorForInvalidParam,exception.getMessage());

        Set<String> typoInRequestParam = new HashSet<>(Arrays.asList("limi"));

        exception = assertThrows(BadRequestException.class, () -> {
            schemaInfoRequestValidator.validateRequest(typoInRequestParam);
        });

        String expectedErrorForTypo = "limi is not a valid input param!";
        assertEquals(expectedErrorForTypo, exception.getMessage());
    }
    @Test
    public void extractQueryParamsFromRequest() {
        // Create a mock HttpServletRequest
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addParameter("param1", "value1");
        mockRequest.addParameter("param2", "value2");

        // Set the mock request attributes
        RequestAttributes requestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        // Act
        Set<String> queryParams = SchemaInfoRequestValidator.extractQueryParamsFromRequest();

        // Assert
        assertEquals(2, queryParams.size());
        assertEquals(Set.of("param1", "param2"), queryParams);
    }
}
