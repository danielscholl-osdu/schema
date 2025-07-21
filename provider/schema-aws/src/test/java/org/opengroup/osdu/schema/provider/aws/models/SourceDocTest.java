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

import org.junit.jupiter.api.Test;
import org.opengroup.osdu.schema.model.Source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SourceDocTest {

  @Test
  void testSourceDocConstructorAndGetters() {
    // Arrange
    String id = "test-id";
    String dataPartitionId = "test-partition";
    Source source = new Source();
    source.setSourceId("test-source");
    
    // Act
    SourceDoc doc = new SourceDoc(id, dataPartitionId, source);
    
    // Assert
    assertEquals(id, doc.getId());
    assertEquals(dataPartitionId, doc.getDataPartitionId());
    assertEquals(source, doc.getSource());
  }
  
  @Test
  void testSourceDocSetters() {
    // Arrange
    SourceDoc doc = new SourceDoc();
    String id = "test-id";
    String dataPartitionId = "test-partition";
    Source source = new Source();
    source.setSourceId("test-source");
    
    // Act
    doc.setId(id);
    doc.setDataPartitionId(dataPartitionId);
    doc.setSource(source);
    
    // Assert
    assertEquals(id, doc.getId());
    assertEquals(dataPartitionId, doc.getDataPartitionId());
    assertEquals(source, doc.getSource());
  }
  
  @Test
  void testDefaultConstructor() {
    // Act
    SourceDoc doc = new SourceDoc();
    
    // Assert
    assertNotNull(doc);
  }
}
