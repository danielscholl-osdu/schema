// Copyright © 2020 Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
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