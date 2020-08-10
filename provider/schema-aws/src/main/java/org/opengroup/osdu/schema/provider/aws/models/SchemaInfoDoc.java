package org.opengroup.osdu.schema.provider.aws.models;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDBTable(tableName = "Schema.SchemaInfo")
public class SchemaInfoDoc {

  @DynamoDBHashKey(attributeName = "Id")
  private String id;

  @DynamoDBAttribute(attributeName = "DataPartitionId")
  private String dataPartitionId;

  @DynamoDBAttribute(attributeName = "SchemaAuthority")
  private String authority;

  // `Source` is a DynamoDB reserved word
  @DynamoDBAttribute(attributeName = "SchemaSource")
  private String source;

  @DynamoDBAttribute(attributeName = "SchemaEntityType")
  private String entityType;

  @DynamoDBIndexHashKey(globalSecondaryIndexName = "major-version-index")
  @DynamoDBAttribute(attributeName = "PartitionAuthoritySourceEntityType")
  public String getPartialKey() {
    return String.format("%s:%s:%s:%s", getDataPartitionId(),
            getAuthority(),
            getSource(),
            getEntityType());
  }
  public void setPartialKey(String value) {}

  @DynamoDBIndexRangeKey(globalSecondaryIndexName = "major-version-index")
  @DynamoDBAttribute(attributeName = "MajorVersion")
  private Long majorVersion;

  @DynamoDBAttribute(attributeName = "MinorVersion")
  private Long minorVersion;

  @DynamoDBAttribute(attributeName = "PatchVersion")
  private Long patchVersion;

  // `Scope` is a DynamoDB reserved word
  @DynamoDBAttribute(attributeName = "SchemaScope")
  private String scope;

  // `Status` is a DynamoDB reserved word
  @DynamoDBAttribute(attributeName = "SchemaStatus")
  private String status;

  @DynamoDBTypeConverted(converter = SchemaInfoDoc.SchemaInfoConverter.class)
  @DynamoDBAttribute(attributeName = "SchemaInfo")
  private SchemaInfo schemaInfo;

  public static class SchemaInfoConverter implements DynamoDBTypeConverter<String, SchemaInfo> {

    @SneakyThrows
    @Override
    public String convert(SchemaInfo object) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(object);
    }

    @SneakyThrows
    @Override
    public SchemaInfo unconvert(String object) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(object, new TypeReference<SchemaInfo>(){});
    }
  }

  /**
   * Maps a SchemaInfo object to a new SchemaInfoDoc
   * @param schemaInfo
   * @return
   */
  public static SchemaInfoDoc mapFrom(SchemaInfo schemaInfo, String dataPartitionId) {
    SchemaIdentity schemaIdentity = schemaInfo.getSchemaIdentity();
    SchemaStatus schemaStatus = schemaInfo.getStatus();
    SchemaScope schemaScope = schemaInfo.getScope();
    SchemaIdentity schemaSupersededBy = schemaInfo.getSupersededBy();

    SchemaInfoDocBuilder schemaInfoDocBuilder = new SchemaInfoDoc().builder()
            .dataPartitionId(dataPartitionId)
            .schemaInfo(schemaInfo)
            .authority(schemaIdentity.getAuthority())
            .scope(schemaScope.name())
            .source(schemaIdentity.getSource())
            .entityType(schemaIdentity.getEntityType())
            .status(schemaStatus.name())
            .majorVersion(schemaIdentity.getSchemaVersionMajor())
            .minorVersion(schemaIdentity.getSchemaVersionMinor())
            .patchVersion(schemaIdentity.getSchemaVersionPatch());

    return schemaInfoDocBuilder.build();
  }
}
