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

import org.opengroup.osdu.schema.model.EntityType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class EntityTypeConverter implements AttributeConverter<EntityType> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(EntityType input) {
        if (input == null) {
            return AttributeValue.builder().nul(true).build();
        }
        try {
            String json = MAPPER.writeValueAsString(input);
            return AttributeValue.builder().s(json).build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to convert EntityType to JSON string", e);
        }
    }

    @Override
    public EntityType transformTo(AttributeValue input) {
        if (input == null || Boolean.TRUE.equals(input.nul())) {
            return null;
        }        
        String json = input.s();
        try {
            return MAPPER.readValue(json, EntityType.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to convert JSON string to EntityType", e);
        }
    }

    @Override
    public EnhancedType<EntityType> type() {
        return EnhancedType.of(EntityType.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
