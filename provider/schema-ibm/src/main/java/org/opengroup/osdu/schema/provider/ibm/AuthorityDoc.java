/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.schema.provider.ibm;

import org.opengroup.osdu.schema.model.Authority;

public class AuthorityDoc {

	private String _id;
	private String _rev;
	private Authority authority;

	public AuthorityDoc(Authority authority) {
		this.setId(authority.getAuthorityId());
		this.setAuthority(authority);
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

	public Authority getAuthority() {
		return authority;
	}

	public void setAuthority(Authority authority) {
		this.authority = authority;
	}

}
