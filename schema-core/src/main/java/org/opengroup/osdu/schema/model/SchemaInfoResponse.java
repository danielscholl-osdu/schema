package org.opengroup.osdu.schema.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SchemaInfoResponse {

    List<SchemaInfo> schemaInfos;
    int offset;
    int count;
    int totalCount;
}
