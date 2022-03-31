package org.opengroup.osdu.schema.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.schema.constants.SchemaConstants;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaIdentity {

    @NotNull(message = "authority must not be null")
    @Pattern(regexp = SchemaConstants.SCHEMA_EMPTY_REGEX, message = "authority must not contain whitespaces and special characters except - and .")
    private String authority;

    @NotNull(message = "source must not be null")
    @Pattern(regexp = SchemaConstants.SCHEMA_EMPTY_REGEX, message = "source must not contain whitespaces and special characters except - and .")
    private String source;

    @NotNull(message = "entityType must not be null")
    @Pattern(regexp = SchemaConstants.SCHEMA_EMPTY_REGEX, message = "entityType must not contain whitespaces and special characters except - and .")
    private String entityType;

    @NotNull(message = "schemaVersionMajor must not be null")
    private Long schemaVersionMajor;

    @NotNull(message = "schemaVersionMinor must not be null")
    private Long schemaVersionMinor;

    @NotNull(message = "schemaVersionPatch must not be null")
    private Long schemaVersionPatch;

    private String id;

}
