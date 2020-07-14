/**
 * Copyright 2020 IBM Corp. All Rights Reserved.
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

package org.opengroup.osdu.schema.provider.ibm;

import java.util.Date;

import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemaDoc extends SchemaInfo {
	
    private String _id;
    private String _rev;
    private long createdDate;
    
	public SchemaDoc(SchemaInfo schemaInfo) {
		this.set_id(schemaInfo.getSchemaIdentity().getId()); 
		super.setCreatedBy(schemaInfo.getCreatedBy());
		if (schemaInfo.getDateCreated() != null) {
			this.setCreatedDate(schemaInfo.getDateCreated().getTime());
			super.setDateCreated(null);
		}
		// sub objects
		super.setStatus(schemaInfo.getStatus());
		schemaInfo.getSchemaIdentity().setId(null);
		super.setSchemaIdentity(schemaInfo.getSchemaIdentity());
		super.setScope(schemaInfo.getScope());
		super.setSupersededBy(schemaInfo.getSupersededBy());
	}
	
	public SchemaInfo getSchemaInfo() {
		SchemaIdentity schemaIdentity = super.getSchemaIdentity();
		schemaIdentity.setId(this._id);
		
		SchemaInfo info = new SchemaInfo();
		info.setSchemaIdentity(schemaIdentity);
		info.setCreatedBy(super.getCreatedBy());
		info.setDateCreated(new Date(this.createdDate));
		info.setStatus(super.getStatus());
		info.setScope(super.getScope());
		info.setSupersededBy(super.getSupersededBy());
		return info;

		//		return SchemaInfo(schemaIdentity, super.getCreatedBy(), (new Date(this.createdDate)), 
		//				super.getStatus(), super.getScope(), super.getSupersededBy());
	}

	public String getRev() {
		return this._rev;
	}

	public void setRev(String rev) {
		this._rev = rev;
	}
	
	public String getId() {
		return _id;
	}
	public String getKind() {
		return _id;
	}
		

	
	

}
