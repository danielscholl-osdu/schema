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
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.provider.aws.utils.converters.SchemaInfoConverter;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchemaInfoDocTest {

	@Test
	void schemaInfoConverter_Success() {
		SchemaInfoConverter converter = new SchemaInfoConverter();
		SchemaInfo obj = new SchemaInfo();
		obj.setCreatedBy("createdby");
		obj.setDateCreated(new Date());
		obj.setScope(SchemaScope.INTERNAL);
		obj.setSchemaIdentity(new SchemaIdentity());
		obj.setStatus(SchemaStatus.DEVELOPMENT);

		SchemaInfo actual = converter.transformTo(converter.transformFrom(obj));
		assertEquals(obj, actual);
	}

	@Test
	void mapFromMapsObject() {
		SchemaInfo schemaInfo = new SchemaInfo(new SchemaIdentity(), "user@opendes.com", new Date(),
				SchemaStatus.PUBLISHED, SchemaScope.INTERNAL, new SchemaIdentity());
		String dataPartitionId = "dataPartitionId";
		
		SchemaIdentity schemaIdentity = schemaInfo.getSchemaIdentity();
		SchemaStatus schemaStatus = schemaInfo.getStatus();
		SchemaScope schemaScope = schemaInfo.getScope();

		SchemaInfoDoc expected = SchemaInfoDoc.builder()
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
		
		SchemaInfoDoc actual = SchemaInfoDoc.mapFrom(schemaInfo, dataPartitionId);
		assertEquals(expected, actual);
	}
}
