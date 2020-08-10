package org.opengroup.osdu.schema.provider.aws.models;

import org.junit.Test;
import org.opengroup.osdu.schema.model.Authority;

import static org.junit.Assert.assertEquals;

public class AuthorityDocTest {

  @Test
  public void authorityConverter() {
    AuthorityDoc.AuthorityConverter converter = new AuthorityDoc.AuthorityConverter();
    Authority obj = new Authority();
    obj.setAuthorityId("id");
    Authority actual = converter.unconvert(converter.convert(obj));
    assertEquals(obj, actual);
  }
}