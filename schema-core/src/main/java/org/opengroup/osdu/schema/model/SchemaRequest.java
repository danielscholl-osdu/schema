package org.opengroup.osdu.schema.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opengroup.osdu.schema.validation.request.SchemaRequestConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Represents a model to Schema Request")
public class SchemaRequest {

	@NotNull(message = "schemaInfo must not be null")
	@Valid
	private SchemaInfo schemaInfo;

	@NotNull(message = "schema must not be null")
    @SchemaRequestConstraint
	private Object schema;

}
