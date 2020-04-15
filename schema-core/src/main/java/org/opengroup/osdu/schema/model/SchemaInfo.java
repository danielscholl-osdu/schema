package org.opengroup.osdu.schema.model;

import java.util.Date;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaInfo {

	@NotNull(message = "schemaIdentity must not be null")
	@Valid
	private SchemaIdentity schemaIdentity;

	private String createdBy;

	private Date dateCreated;

	@NotNull(message = "status must not be null")
	private SchemaStatus status;

	private SchemaScope scope;

	@JsonInclude(Include.NON_NULL)
	@Valid
	private SchemaIdentity supersededBy;

}