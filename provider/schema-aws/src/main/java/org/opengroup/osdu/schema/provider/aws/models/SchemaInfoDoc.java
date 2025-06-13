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

import lombok.*;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.provider.aws.utils.converters.SchemaInfoConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDbBean
public class SchemaInfoDoc {

  private String id;
  private String dataPartitionId;
  private String authority;
  private String source;
  private String entityType;
  private String gsiPartitionKey;
  private Long majorVersion;
  private Long minorVersion;
  private Long patchVersion;
  private String scope;
  private String status;
  private SchemaInfo schemaInfo;

  @DynamoDbPartitionKey
  @DynamoDbAttribute("Id")
  public String getId() {
    return id;
  }

  @DynamoDbAttribute("DataPartitionId")
  public String getDataPartitionId() {
    return dataPartitionId;
  }

  @DynamoDbAttribute("SchemaAuthority")
  public String getAuthority() {
    return authority;
  }

  @DynamoDbAttribute("SchemaSource")
  public String getSource() {
    return source;
  }

  @DynamoDbAttribute("SchemaEntityType")
  public String getEntityType() {
    return entityType;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = "major-version-index")
  @DynamoDbAttribute("PartitionAuthoritySourceEntityType")
  public String getGsiPartitionKey() {
    return gsiPartitionKey;
  }

  @DynamoDbSecondarySortKey(indexNames = "major-version-index")
  @DynamoDbAttribute("MajorVersion")
  public Long getMajorVersion() {
    return majorVersion;
  }

  @DynamoDbAttribute("MinorVersion")
  public Long getMinorVersion() {
    return minorVersion;
  }

  @DynamoDbAttribute("PatchVersion")
  public Long getPatchVersion() {
    return patchVersion;
  }

  @DynamoDbAttribute("SchemaScope")
  public String getScope() {
    return scope;
  }

  @DynamoDbAttribute("SchemaStatus")
  public String getStatus() {
    return status;
  }

  @DynamoDbConvertedBy(SchemaInfoConverter.class)
  @DynamoDbAttribute("SchemaInfo")
  public SchemaInfo getSchemaInfo() {
    return schemaInfo;
  }

  /**
   * Maps a SchemaInfo object to a new SchemaInfoDoc
   * @param schemaInfo The SchemaInfo object to map
   * @param dataPartitionId The data partition ID
   * @return A new SchemaInfoDoc instance
   */
  public static SchemaInfoDoc mapFrom(SchemaInfo schemaInfo, String dataPartitionId) {
    SchemaIdentity schemaIdentity = schemaInfo.getSchemaIdentity();
    SchemaStatus schemaStatus = schemaInfo.getStatus();
    SchemaScope schemaScope = schemaInfo.getScope();

    return SchemaInfoDoc.builder()
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
            .gsiPartitionKey(String.format("%s:%s:%s:%s", dataPartitionId, schemaIdentity.getAuthority(), 
                    schemaIdentity.getSource(), schemaIdentity.getEntityType()))
            .build();
  }
}
