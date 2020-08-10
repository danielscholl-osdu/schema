package org.opengroup.osdu.schema.provider.aws.models;

import org.junit.Test;
import org.opengroup.osdu.schema.model.EntityType;

import static org.junit.Assert.assertEquals;

public class EntityTypeDocTest {

  @Test
  public void entityTypeConverter_Success() {
    EntityTypeDoc.EntityTypeConverter converter = new EntityTypeDoc.EntityTypeConverter();
    EntityType obj = new EntityType();
    obj.setEntityTypeId("id");
    obj.setDescription("description");
    obj.setIcon("icon");
    obj.setStatus("status");
    EntityType actual = converter.unconvert(converter.convert(obj));
    assertEquals(obj, actual);
  }
}