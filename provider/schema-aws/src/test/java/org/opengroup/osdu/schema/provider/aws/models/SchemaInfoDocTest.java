package org.opengroup.osdu.schema.provider.aws.models;

import org.junit.Test;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class SchemaInfoDocTest {

  @Test
  public void getPartialKey() {
    SchemaInfoDoc obj = SchemaInfoDoc.builder()
            .dataPartitionId("partition")
            .authority("authority")
            .source("source")
            .entityType("entity")
            .build();
    assertEquals("partition:authority:source:entity", obj.getPartialKey());
  }

  @Test
  public void schemaInfoConverter_Success() {
    SchemaInfoDoc.SchemaInfoConverter converter = new SchemaInfoDoc.SchemaInfoConverter();
    SchemaInfo obj = new SchemaInfo();
    obj.setCreatedBy("createdby");
    obj.setDateCreated(new Date());
    obj.setScope(SchemaScope.INTERNAL);
    obj.setSchemaIdentity(new SchemaIdentity());
    obj.setStatus(SchemaStatus.DEVELOPMENT);

    SchemaInfo actual = converter.unconvert(converter.convert(obj));
    assertEquals(obj, actual);
  }


  @Test
  public void mapFrom() {
    // TBD
  }
}