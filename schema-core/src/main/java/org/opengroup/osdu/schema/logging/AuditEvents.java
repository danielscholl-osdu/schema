package org.opengroup.osdu.schema.logging;

import static java.lang.String.format;

import com.google.common.base.Strings;
import java.util.List;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;

public class AuditEvents {

	public static final String SCHEMA_REGISTERED_ID = "SC001";
	public static final String SCHEMA_REGISTERED_MESSAGE = "Schema registered ";

	public static final String SCHEMA_RETRIEVED_ID = "SC002";
	public static final String SCHEMA_RETRIEVED_MESSAGE = "Schema retrieved";

	public static final String SEARCH_FOR_SCHEMA_ID = "SC003";
	public static final String SEARCH_FOR_SCHEMA_MESSAGE = "Search for schema";

	public static final String SCHEMA_UPDATED_ID = "SC004";
	public static final String SCHEMA_UPDATED_MESSAGE = "Schema update";

	private final String user;

	public AuditEvents(String user) {
		if (Strings.isNullOrEmpty(user)) {
			throw new IllegalArgumentException("User not provided for audit events.");
		}
		this.user = user;
	}

	public AuditPayload getSchemaRegistered(AuditStatus status, List<String> resources){
		return AuditPayload.builder()
				.action(AuditAction.CREATE)
				.status(status)
				.user(this.user)
				.actionId(SCHEMA_REGISTERED_ID)
				.message(getStatusMessage(status, SCHEMA_REGISTERED_MESSAGE))
				.resources(resources)
				.build();
	}

	public AuditPayload getSchemaRetrieved(AuditStatus status, List<String> resources){
		return AuditPayload.builder()
				.action(AuditAction.READ)
				.status(status)
				.user(this.user)
				.actionId(SCHEMA_RETRIEVED_ID)
				.message(getStatusMessage(status, SCHEMA_RETRIEVED_MESSAGE))
				.resources(resources)
				.build();
	}

	public AuditPayload getSearchForSchema(AuditStatus status, List<String> resources){
		return AuditPayload.builder()
				.action(AuditAction.READ)
				.status(status)
				.user(this.user)
				.actionId(SEARCH_FOR_SCHEMA_ID)
				.message(getStatusMessage(status, SEARCH_FOR_SCHEMA_MESSAGE))
				.resources(resources)
				.build();
	}

	public AuditPayload getSchemaUpdated(AuditStatus status, List<String> resources){
		return AuditPayload.builder()
				.action(AuditAction.UPDATE)
				.status(status)
				.user(this.user)
				.actionId(SCHEMA_UPDATED_ID)
				.message(getStatusMessage(status, SCHEMA_UPDATED_MESSAGE))
				.resources(resources)
				.build();
	}

	private String getStatusMessage(AuditStatus status, String message) {
		return format("%s %s", message, status.name().toLowerCase());
	}
}
