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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperV2;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;
import org.opengroup.osdu.schema.provider.aws.models.AuthorityDoc;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AwsAuthorityStoreTest {

  @InjectMocks
  private AwsAuthorityStore authorityStore;

  @Mock
  private AwsServiceConfig serviceConfig;

  @Mock
  private DpsHeaders headers;

  @Mock
  private DynamoDBQueryHelperV2 queryHelper;

  @Mock
  private DynamoDBQueryHelperFactory queryHelperFactory;

  @Mock
  private JaxRsDpsLog logger;

  @Before
  public void setUp() throws Exception {
    serviceConfig.amazonRegion = "us-east-1";
    serviceConfig.environment = "test";    

    Mockito.when(queryHelperFactory.getQueryHelperForPartition(Mockito.any(DpsHeaders.class), Mockito.any()))
    .thenReturn(queryHelper);
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void get() throws NotFoundException, ApplicationException {
    String authorityId = "source";
    String partitionId = "partitionId";
    Authority expected = new Authority();
    AuthorityDoc authorityDoc = new AuthorityDoc("id", partitionId, expected);

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(authorityDoc);
    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);

    Authority actual = authorityStore.get(authorityId);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void create() throws BadRequestException, ApplicationException {
    String partitionId = "partitionId";
    Authority expected = new Authority();

    Mockito.when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(queryHelper).save(Mockito.any());
    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
    Authority actual = authorityStore.create(expected);
    assertEquals(expected, actual);
  }
}