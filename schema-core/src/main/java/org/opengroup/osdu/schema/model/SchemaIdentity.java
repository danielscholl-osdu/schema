package org.opengroup.osdu.schema.model;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaIdentity {

    @NotNull(message = "authority must not be null")
    private String authority;

    @NotNull(message = "source must not be null")
    private String source;

    @NotNull(message = "entityType must not be null")
    private String entityType;

    @NotNull(message = "schemaVersionMajor must not be null")
    private Long schemaVersionMajor;

    @NotNull(message = "schemaVersionMinor must not be null")
    private Long schemaVersionMinor;

    @NotNull(message = "schemaVersionPatch must not be null")
    private Long schemaVersionPatch;

    private String id;

}
