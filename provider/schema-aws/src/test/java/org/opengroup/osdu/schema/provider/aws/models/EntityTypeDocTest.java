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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opengroup.osdu.schema.model.EntityType;

class EntityTypeDocTest {
	
	@Test
	public void constructor_Success() {
		// Arrange
		String id = "test-id";
		String dataPartitionId = "test-partition";
		EntityType entityType = new EntityType();
		entityType.setEntityTypeId("test-entity-type");
		
		// Act
		EntityTypeDoc doc = new EntityTypeDoc(id, dataPartitionId, entityType);
		
		// Assert
		assertEquals(id, doc.getId());
		assertEquals(dataPartitionId, doc.getDataPartitionId());
		assertEquals(entityType, doc.getEntityType());
	}
	
	@Test
	public void gettersAndSetters_Success() {
		// Arrange
		EntityTypeDoc doc = new EntityTypeDoc();
		String id = "test-id";
		String dataPartitionId = "test-partition";
		EntityType entityType = new EntityType();
		entityType.setEntityTypeId("test-entity-type");
		
		// Act
		doc.setId(id);
		doc.setDataPartitionId(dataPartitionId);
		doc.setEntityType(entityType);
		
		// Assert
		assertEquals(id, doc.getId());
		assertEquals(dataPartitionId, doc.getDataPartitionId());
		assertEquals(entityType, doc.getEntityType());
	}
}
