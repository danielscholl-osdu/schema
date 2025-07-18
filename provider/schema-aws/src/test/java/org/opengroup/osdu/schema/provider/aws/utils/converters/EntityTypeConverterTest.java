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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.schema.model.EntityType;

import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class EntityTypeConverterTest {

    private EntityTypeConverter converter;
    private EntityType testEntityType;

    @BeforeEach
    void setUp() {
        converter = new EntityTypeConverter();
        testEntityType = new EntityType();
        testEntityType.setEntityTypeId("test-entity-type");
        testEntityType.setDescription("Test Description");
        testEntityType.setIcon("test-icon");
        testEntityType.setStatus("active");
    }

    @Test
    void transformFrom_shouldConvertEntityTypeToAttributeValue() {
        // Act
        AttributeValue result = converter.transformFrom(testEntityType);
        
        // Assert
        assertTrue(result.s().contains("test-entity-type"));
        assertTrue(result.s().contains("Test Description"));
        assertTrue(result.s().contains("test-icon"));
        assertTrue(result.s().contains("active"));
    }

    @Test
    void transformFrom_shouldHandleNullInput() {
        // Act
        AttributeValue result = converter.transformFrom(null);
        
        // Assert
        assertTrue(result.nul());
    }

    @Test
    void transformTo_shouldConvertAttributeValueToEntityType() {
        // Arrange
        AttributeValue attributeValue = converter.transformFrom(testEntityType);
        
        // Act
        EntityType result = converter.transformTo(attributeValue);
        
        // Assert
        assertEquals(testEntityType.getEntityTypeId(), result.getEntityTypeId());
        assertEquals(testEntityType.getDescription(), result.getDescription());
        assertEquals(testEntityType.getIcon(), result.getIcon());
        assertEquals(testEntityType.getStatus(), result.getStatus());
    }

    @Test
    void transformTo_shouldHandleNullInput() {
        // Act
        EntityType result = converter.transformTo(null);
        
        // Assert
        assertNull(result);
    }

    @Test
    void transformTo_shouldHandleNullAttributeValue() {
        // Arrange
        AttributeValue nullValue = AttributeValue.builder().nul(true).build();
        
        // Act
        EntityType result = converter.transformTo(nullValue);
        
        // Assert
        assertNull(result);
    }

    @Test
    void type_shouldReturnCorrectEnhancedType() {
        // Act
        EnhancedType<EntityType> type = converter.type();
        
        // Assert
        assertEquals(EnhancedType.of(EntityType.class), type);
    }

    @Test
    void attributeValueType_shouldReturnStringType() {
        // Act
        AttributeValueType valueType = converter.attributeValueType();
        
        // Assert
        assertEquals(AttributeValueType.S, valueType);
    }
}
