package org.opengroup.osdu.schema.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryParams {

    private String authority;
    private String source;
    private String entity;
    private Long schemaVersionMajor;
    private Long schemaVersionMinor;
    private int limit;
    private int offset;
    private String status;
    private String scope;
    private Boolean latestVersion;

}
