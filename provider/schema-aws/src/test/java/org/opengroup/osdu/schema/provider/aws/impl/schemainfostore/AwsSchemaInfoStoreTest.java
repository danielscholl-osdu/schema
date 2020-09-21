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
package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AwsSchemaInfoStoreTest {

  @InjectMocks
  private AwsSchemaInfoStore schemaInfoStore;

  @Mock
  private AwsServiceConfig serviceConfig;

  @Mock
  private DpsHeaders headers;

  @Mock
  private DynamoDBQueryHelper queryHelper;

  @Mock
  private JaxRsDpsLog logger;

  @Before
  public void setUp() throws Exception {
    serviceConfig.amazonRegion = "us-east-1";
    serviceConfig.s3DataBucket = "bucket";
    serviceConfig.environment = "test";
    serviceConfig.s3Endpoint = "s3endpoint";
    serviceConfig.dynamoDbEndpoint = "dbendpoint";
    serviceConfig.dynamoDbTablePrefix = "pre-";
  }

  @Test
  public void isUnique_ifNotExists_returnTrue() throws ApplicationException {
    String partitionId = "partitionId";
    String schemaId = "schemaId";
    Mockito.when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);
    Boolean actual = schemaInfoStore.isUnique(schemaId, partitionId);
    assertEquals(true, actual);
  }

  @Test
  public void isUnique_ifExists_returnFalse() throws ApplicationException {
    String partitionId = "partitionId";
    String schemaId = "schemaId";
    Mockito.when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(true);
    Boolean actual = schemaInfoStore.isUnique(schemaId, partitionId);
    assertEquals(false, actual);
  }
}