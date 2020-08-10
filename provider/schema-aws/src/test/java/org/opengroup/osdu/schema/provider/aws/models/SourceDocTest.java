package org.opengroup.osdu.schema.provider.aws.models;

import org.junit.Test;
import org.opengroup.osdu.schema.model.Source;

import static org.junit.Assert.*;

public class SourceDocTest {

  @Test
  public void sourceConverter_Success() {
    SourceDoc.SourceConverter converter = new SourceDoc.SourceConverter();
    Source obj = new Source();
    obj.setSourceId("id");
    Source actual = converter.unconvert(converter.convert(obj));
    assertEquals(obj, actual);
  }
}