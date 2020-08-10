package org.opengroup.osdu.schema.provider.aws.models;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.opengroup.osdu.schema.model.Authority;


@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "Schema.Authority")
public class AuthorityDoc {

  @DynamoDBHashKey(attributeName = "Id")
  private String id;

  @DynamoDBAttribute(attributeName = "DataPartitionId")
  private String dataPartitionId;

  @DynamoDBTypeConverted(converter = AuthorityDoc.AuthorityConverter.class)
  @DynamoDBAttribute(attributeName = "EntityType")
  private Authority authority;

  public static class AuthorityConverter implements DynamoDBTypeConverter<String, Authority> {

    @SneakyThrows
    @Override
    public String convert(Authority object) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(object);
    }

    @SneakyThrows
    @Override
    public Authority unconvert(String object) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(object, new TypeReference<Authority>() {
      });
    }
  }
}