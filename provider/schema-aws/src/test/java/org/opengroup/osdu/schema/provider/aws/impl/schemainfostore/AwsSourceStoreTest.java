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

import com.amazonaws.SdkClientException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperFactory;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelperV2;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;

import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.schema.provider.aws.models.SourceDoc;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AwsSourceStoreTest {

  @InjectMocks
  private AwsSourceStore sourceStore;


  @Mock
  private DpsHeaders headers;

  @Mock
  private DynamoDBQueryHelperV2 queryHelper;

  @Mock
  private DynamoDBQueryHelperFactory queryHelperFactory;

  @Mock
  private JaxRsDpsLog logger;

  private static final String COMMON_TENANT_ID = "common";

  @Before
  public void setUp() throws Exception {

    Mockito.when(queryHelperFactory.getQueryHelperForPartition(Mockito.any(DpsHeaders.class), Mockito.any()))
    .thenReturn(queryHelper);
    ReflectionTestUtils.setField(sourceStore, "sharedTenant", COMMON_TENANT_ID);
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void get() throws NotFoundException, ApplicationException {
    String sourceId = "source";
    String partitionId = "partitionId";
    Source expected = new Source();
    SourceDoc sourceDoc = new SourceDoc("id", partitionId, expected);

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(sourceDoc);
    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);

    Source actual = sourceStore.get(sourceId);
    Assert.assertEquals(expected, actual);

  }

  @Test
  public void get_SystemSchemas() throws NotFoundException, ApplicationException {
    String sourceId = "source";
    Source expected = new Source();
    SourceDoc sourceDoc = new SourceDoc("id", COMMON_TENANT_ID, expected);

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(sourceDoc);

    Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
    Source actual = sourceStore.getSystemSource(sourceId);
    Assert.assertEquals(expected, actual);

  }

  @Test
  public void create() throws BadRequestException, ApplicationException {
    String sourceId = "source";
    String partitionId = "partitionId";
    Source expected = new Source();

    Mockito.doNothing().when(queryHelper).save(Mockito.any());
    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
    Source actual = sourceStore.create(expected);
    assertEquals(expected, actual);
  }

  @Test
  public void create_SystemSchemas() throws BadRequestException, ApplicationException {
    String sourceId = "source";
    Source expected = new Source();

    Mockito.doNothing().when(queryHelper).save(Mockito.any());
    Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
    Source actual = sourceStore.createSystemSource(expected);
    assertEquals(expected, actual);
  }

  @Test
  public void get_throwsException_whenNotFound() throws NotFoundException, ApplicationException {
    String sourceId = "source";
    String partitionId = "partitionId";

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(null);
    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(AwsSourceStore.SOURCE_NOT_FOUND);
    Source actual = sourceStore.get(sourceId);

  }

  @Test
  public void get_throwsException_whenNotFound_SystemSchemas() throws NotFoundException, ApplicationException {
    String sourceId = "source";

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(null);
    Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(AwsSourceStore.SOURCE_NOT_FOUND);
    Source actual = sourceStore.getSystemSource(sourceId);

  }

  @Test
  public void create_throwsException_whenItemExists() throws BadRequestException, ApplicationException {
    String sourceId = "source";
    String partitionId = "partitionId";
    String expectedErrorMessage = "Source already registered with Id: source";
    Source expected = new Source();
    expected.setSourceId(sourceId);
    SourceDoc sourceDoc = new SourceDoc("id", partitionId, expected);

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(sourceDoc);

    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(expectedErrorMessage);
    Source actual = sourceStore.create(expected);

  }

  @Test
  public void create_throwsException_whenItemExists_SystemSchemas() throws BadRequestException, ApplicationException {
    String sourceId = "source";
    String expectedErrorMessage = "Source already registered with Id: source";
    Source expected = new Source();
    expected.setSourceId(sourceId);
    SourceDoc sourceDoc = new SourceDoc("id", COMMON_TENANT_ID, expected);

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(sourceDoc);

    Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(expectedErrorMessage);
    Source actual = sourceStore.createSystemSource(expected);

  }

  @Test
  public void create_throwsException_onSaveFailure() throws BadRequestException, ApplicationException {
    String sourceId = "source";
    String partitionId = "partitionId";
    String expectedErrorMessage = "Source already registered with Id: source";
    Source expected = new Source();

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(null);

    Mockito.doThrow(SdkClientException.class).when(queryHelper).save(Mockito.any());
    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
    expectedException.expect(ApplicationException.class);
    expectedException.expectMessage(SchemaConstants.INVALID_INPUT);
    Source actual = sourceStore.create(expected);

  }

  @Test
  public void create_throwsException_onSaveFailure_SystemSchemas() throws BadRequestException, ApplicationException {
    String sourceId = "source";
    String expectedErrorMessage = "Source already registered with Id: source";
    Source expected = new Source();

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(null);

    Mockito.doThrow(SdkClientException.class).when(queryHelper).save(Mockito.any());
    Mockito.when(headers.getPartitionId()).thenReturn(COMMON_TENANT_ID);
    expectedException.expect(ApplicationException.class);
    expectedException.expectMessage(SchemaConstants.INVALID_INPUT);
    Source actual = sourceStore.createSystemSource(expected);

  }
}