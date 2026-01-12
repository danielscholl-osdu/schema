/*
 * Copyright © Amazon Web Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opengroup.osdu.schema.provider.aws.utils.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;

import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class SchemaInfoConverterTest {

    private SchemaInfoConverter converter;
    private SchemaInfo testSchemaInfo;

    @BeforeEach
    void setUp() {
        converter = new SchemaInfoConverter();

        // Create a test SchemaIdentity
        SchemaIdentity schemaIdentity = new SchemaIdentity();
        schemaIdentity.setId("test-schema-id");
        schemaIdentity.setAuthority("test-authority");
        schemaIdentity.setSource("test-source");
        schemaIdentity.setEntityType("test-entity-type");
        schemaIdentity.setSchemaVersionMajor(1L);
        schemaIdentity.setSchemaVersionMinor(2L);
        schemaIdentity.setSchemaVersionPatch(3L);

        // Create a test SchemaInfo
        testSchemaInfo = new SchemaInfo();
        testSchemaInfo.setSchemaIdentity(schemaIdentity);
        testSchemaInfo.setCreatedBy("test-user@example.com");
        testSchemaInfo.setDateCreated(new Date());
        testSchemaInfo.setStatus(SchemaStatus.DEVELOPMENT);
        testSchemaInfo.setScope(SchemaScope.INTERNAL);
    }

    @Test
    void transformFrom_shouldConvertSchemaInfoToAttributeValue() {
        // Act
        AttributeValue result = converter.transformFrom(testSchemaInfo);

        // Assert
        String jsonString = result.s();
        assertTrue(jsonString.contains("test-schema-id"));
        assertTrue(jsonString.contains("test-authority"));
        assertTrue(jsonString.contains("test-source"));
        assertTrue(jsonString.contains("test-entity-type"));
        assertTrue(jsonString.contains("test-user@example.com"));
        assertTrue(jsonString.contains("DEVELOPMENT"));
        assertTrue(jsonString.contains("INTERNAL"));
    }

    @Test
    void transformFrom_shouldHandleNullInput() {
        // Act
        AttributeValue result = converter.transformFrom(null);

        // Assert
        assertTrue(result.nul());
    }

    @Test
    void transformTo_shouldConvertAttributeValueToSchemaInfo() {
        // Arrange
        AttributeValue attributeValue = converter.transformFrom(testSchemaInfo);

        // Act
        SchemaInfo result = converter.transformTo(attributeValue);

        // Assert
        assertEquals(testSchemaInfo.getSchemaIdentity().getId(), result.getSchemaIdentity().getId());
        assertEquals(testSchemaInfo.getSchemaIdentity().getAuthority(), result.getSchemaIdentity().getAuthority());
        assertEquals(testSchemaInfo.getSchemaIdentity().getSource(), result.getSchemaIdentity().getSource());
        assertEquals(testSchemaInfo.getSchemaIdentity().getEntityType(), result.getSchemaIdentity().getEntityType());
        assertEquals(testSchemaInfo.getSchemaIdentity().getSchemaVersionMajor(),
                result.getSchemaIdentity().getSchemaVersionMajor());
        assertEquals(testSchemaInfo.getSchemaIdentity().getSchemaVersionMinor(),
                result.getSchemaIdentity().getSchemaVersionMinor());
        assertEquals(testSchemaInfo.getSchemaIdentity().getSchemaVersionPatch(),
                result.getSchemaIdentity().getSchemaVersionPatch());
        assertEquals(testSchemaInfo.getCreatedBy(), result.getCreatedBy());
        assertEquals(testSchemaInfo.getStatus(), result.getStatus());
        assertEquals(testSchemaInfo.getScope(), result.getScope());
    }

    @Test
    void transformTo_shouldHandleInvalidJson() {
        // Arrange
        AttributeValue invalidJson = AttributeValue.builder().s("{invalid-json}").build();

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            converter.transformTo(invalidJson);
        });

        assertTrue(exception.getMessage().contains("Error converting JSON to SchemaInfo"));
    }

    @Test
    void transformTo_shouldHandleNullAttributeValue() {
        // Act
        SchemaInfo result = converter.transformTo(null);

        // Assert
        assertNull(result);
    }

    @Test
    void transformTo_shouldHandleNullFlaggedAttributeValue() {
        // Arrange
        AttributeValue nullValue = AttributeValue.builder().nul(true).build();

        // Act
        SchemaInfo result = converter.transformTo(nullValue);

        // Assert
        assertNull(result);
    }

    @Test
    void type_shouldReturnCorrectEnhancedType() {
        // Act
        EnhancedType<SchemaInfo> type = converter.type();

        // Assert
        assertEquals(EnhancedType.of(SchemaInfo.class), type);
    }

    @Test
    void attributeValueType_shouldReturnStringType() {
        // Act
        AttributeValueType valueType = converter.attributeValueType();

        // Assert
        assertEquals(AttributeValueType.S, valueType);
    }
}
