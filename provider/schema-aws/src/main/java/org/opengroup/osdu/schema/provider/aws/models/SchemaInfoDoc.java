/* Copyright © 2020 Amazon Web Services

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
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

  @DynamoDBIndexHashKey(attributeName = "PartitionAuthoritySourceEntityType", globalSecondaryIndexName = "major-version-index")
  private String gsiPartitionKey;

  @DynamoDBIndexRangeKey(attributeName = "MajorVersion",globalSecondaryIndexName = "major-version-index")
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
            .patchVersion(schemaIdentity.getSchemaVersionPatch())
            .gsiPartitionKey(String.format("%s:%s:%s:%s",dataPartitionId,schemaIdentity.getAuthority(),schemaIdentity.getSource(),schemaIdentity.getEntityType()));

    return schemaInfoDocBuilder.build();
  }
}
