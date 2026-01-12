/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.schema.provider.ibm;

import org.opengroup.osdu.schema.model.EntityType;

public class EntityTypeDoc {

	private String _id;
	private String _rev;
	private EntityType entityType;

	public EntityTypeDoc(EntityType entityType) {
		this.setId(entityType.getEntityTypeId());
		this.setEntityType(entityType);
	}

	public String getId() {
		return _id;
	}

	public void setId(String _id) {
		this._id = _id;
	}

	public String getRev() {
		return _rev;
	}

	public void setRev(String _rev) {
		this._rev = _rev;
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

}
