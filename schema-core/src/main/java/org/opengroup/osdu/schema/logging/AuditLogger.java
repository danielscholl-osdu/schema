package org.opengroup.osdu.schema.logging;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequiredArgsConstructor
@RequestScope
public class AuditLogger {

	private final JaxRsDpsLog logger;
	private final DpsHeaders headers;

	private AuditEvents events = null;

	private AuditEvents getAuditEvents() {
		if (this.events == null) {
			this.events = new AuditEvents(this.headers.getUserEmail());
		}
		return this.events;
	}

	public void schemaRegisteredSuccess(List<String> resources){
		this.writeLog(this.getAuditEvents().getSchemaRegistered(AuditStatus.SUCCESS, resources));
	}
	public void schemaRegisteredFailure(List<String> resources){
		this.writeLog(this.getAuditEvents().getSchemaRegistered(AuditStatus.FAILURE, resources));
	}
	public void schemaRetrievedSuccess(List<String> resources){
		this.writeLog(this.getAuditEvents().getSchemaRetrieved(AuditStatus.SUCCESS, resources));
	}
	public void schemaRetrievedFailure(List<String> resources){
		this.writeLog(this.getAuditEvents().getSchemaRetrieved(AuditStatus.FAILURE, resources));
	}
	public void searchSchemaSuccess(List<String> resources){
		this.writeLog(this.getAuditEvents().getSearchForSchema(AuditStatus.SUCCESS, resources));
	}
	public void searchSchemaFailure(List<String> resources){
		this.writeLog(this.getAuditEvents().getSearchForSchema(AuditStatus.FAILURE, resources));
	}
	public void schemaUpdatedSuccess(List<String> resources){
		this.writeLog(this.getAuditEvents().getSchemaUpdated(AuditStatus.SUCCESS, resources));
	}
	public void schemaUpdatedFailure(List<String> resources){
		this.writeLog(this.getAuditEvents().getSchemaUpdated(AuditStatus.FAILURE, resources));
	}

	private void writeLog(AuditPayload log) {
		this.logger.audit(log);
	}
}