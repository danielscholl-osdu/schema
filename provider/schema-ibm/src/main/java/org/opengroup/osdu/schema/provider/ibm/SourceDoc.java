/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.schema.provider.ibm;

import org.opengroup.osdu.schema.model.Source;

public class SourceDoc {

	private String _id;
	private String _rev;
	private Source source;

	public SourceDoc(Source source) {
		this.setId(source.getSourceId());
		this.setSource(source);
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

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

}
