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
import org.opengroup.osdu.schema.model.Authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthorityDocTest {

  @Test
  void testAuthorityDocConstructorAndGetters() {
    // Arrange
    String id = "test-id";
    String dataPartitionId = "test-partition";
    Authority authority = new Authority();
    authority.setAuthorityId("test-authority");
    
    // Act
    AuthorityDoc doc = new AuthorityDoc(id, dataPartitionId, authority);
    
    // Assert
    assertEquals(id, doc.getId());
    assertEquals(dataPartitionId, doc.getDataPartitionId());
    assertEquals(authority, doc.getAuthority());
  }
  
  @Test
  void testAuthorityDocSetters() {
    // Arrange
    AuthorityDoc doc = new AuthorityDoc();
    String id = "test-id";
    String dataPartitionId = "test-partition";
    Authority authority = new Authority();
    authority.setAuthorityId("test-authority");
    
    // Act
    doc.setId(id);
    doc.setDataPartitionId(dataPartitionId);
    doc.setAuthority(authority);
    
    // Assert
    assertEquals(id, doc.getId());
    assertEquals(dataPartitionId, doc.getDataPartitionId());
    assertEquals(authority, doc.getAuthority());
  }
  
  @Test
  void testDefaultConstructor() {
    // Act
    AuthorityDoc doc = new AuthorityDoc();
    
    // Assert
    assertNotNull(doc);
  }
}
