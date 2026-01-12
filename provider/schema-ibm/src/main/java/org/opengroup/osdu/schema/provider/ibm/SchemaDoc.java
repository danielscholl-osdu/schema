/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

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
