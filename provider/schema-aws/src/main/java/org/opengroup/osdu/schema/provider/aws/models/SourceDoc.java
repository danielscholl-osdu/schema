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
package org.opengroup.osdu.schema.provider.aws.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.aws.utils.converters.SourceTypeConverter;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class SourceDoc {

  private String id;
  private String dataPartitionId;
  private Source source;

  @DynamoDbPartitionKey
  @DynamoDbAttribute("Id")
  public String getId() {
    return id;
  }

  @DynamoDbAttribute("DataPartitionId")
  public String getDataPartitionId() {
    return dataPartitionId;
  }

  @DynamoDbAttribute("Source")
  @DynamoDbConvertedBy(SourceTypeConverter.class)
  public Source getSource() {
    return source;
  }
}
