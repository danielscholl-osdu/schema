package org.opengroup.osdu.schema.provider.aws.models;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.opengroup.osdu.schema.model.EntityType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "Schema.EntityType")
public class EntityTypeDoc {

  @DynamoDBHashKey(attributeName = "Id")
  private String id;

  @DynamoDBAttribute(attributeName = "DataPartitionId")
  private String dataPartitionId;

  @DynamoDBTypeConverted(converter = EntityTypeDoc.EntityTypeConverter.class)
  @DynamoDBAttribute(attributeName = "EntityType")
  private EntityType entityType;

  public static class EntityTypeConverter implements DynamoDBTypeConverter<String, EntityType> {

    @SneakyThrows
    @Override
    public String convert(EntityType object) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(object);
    }

    @SneakyThrows
    @Override
    public EntityType unconvert(String object) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(object, new TypeReference<EntityType>() {});
    }
  }
}
