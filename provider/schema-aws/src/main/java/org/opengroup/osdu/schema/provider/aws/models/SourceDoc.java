// Copyright © 2020 Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.schema.provider.aws.models;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.opengroup.osdu.schema.model.Source;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "Schema.Source")
public class SourceDoc {

  @DynamoDBHashKey(attributeName = "Id")
  private String id;

  @DynamoDBAttribute(attributeName = "DataPartitionId")
  private String dataPartitionId;

  @DynamoDBTypeConverted(converter = SourceDoc.SourceConverter.class)
  @DynamoDBAttribute(attributeName = "Source")
  private Source source;

  public static class SourceConverter implements DynamoDBTypeConverter<String, Source> {

    @SneakyThrows
    @Override
    public String convert(Source object) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(object);
    }

    @SneakyThrows
    @Override
    public Source unconvert(String object) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(object, new TypeReference<Source>() {});
    }
  }
}
