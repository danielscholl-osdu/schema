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
package org.opengroup.osdu.schema.provider.aws.impl.schemastore;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;

import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AwsSchemaStoreTest {

  @InjectMocks
  private AwsSchemaStore schemaStore;

  @Mock
  private AwsServiceConfig serviceConfig;

  @Mock
  private DpsHeaders headers;

  @Mock
  private AmazonS3 s3;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private JaxRsDpsLog logger;

  @Before
  public void beforeAll() {

  }

  @Before
  public void setUp() {
    serviceConfig.amazonRegion = "us-east-1";
    serviceConfig.s3DataBucket = "bucket";
    serviceConfig.environment = "test";
    serviceConfig.s3Endpoint = "endpoint";
  }

  @After
  public void tearDown() {
  }

  @Test
  public void createSchema() throws MalformedURLException, ApplicationException {
    String filePath = "file/path";
    String content = "content";
    String dataPartitionId = "partitionid";
    URL file = new URL("http", "localhost", "file" );
    Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
    doReturn(null).when(s3).putObject(Mockito.any());
    Mockito.when(s3.getUrl(Mockito.eq(serviceConfig.s3DataBucket), Mockito.eq("schema/partitionid/file/path")))
      .thenReturn(new URL("http", "localhost", "file" ));
    String result = schemaStore.createSchema(filePath, content);
    Assert.assertEquals(file.toString(), result);
  }

  @Test
  public void getSchema() throws NotFoundException, ApplicationException {
    String dataPartitionId = "partitionid";
    String filePath = "file/path";
    String content = "content";
    Mockito.when(s3.getObjectAsString(Mockito.any(), Mockito.any())).thenReturn(content);
    String result = schemaStore.getSchema(dataPartitionId, filePath);
    Assert.assertEquals(content, result);
  }

  @Test
  public void getSchema_NotFound() throws NotFoundException, ApplicationException {
    String dataPartitionId = "partitionid";
    String filePath = "file/path";
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(SchemaConstants.SCHEMA_NOT_PRESENT);
    AmazonS3Exception toThrow = new AmazonS3Exception("NA");
    toThrow.setErrorCode("NoSuchKey");
    Mockito.when(s3.getObjectAsString(Mockito.any(), Mockito.any())).thenThrow(toThrow);
    schemaStore.getSchema(dataPartitionId, filePath);
  }

  @Test
  public void getSchema_S3Exception() throws NotFoundException, ApplicationException {
    String dataPartitionId = "partitionid";
    String filePath = "file/path";
    expectedException.expect(ApplicationException.class);
    expectedException.expectMessage(SchemaConstants.INTERNAL_SERVER_ERROR);
    Mockito.when(s3.getObjectAsString(Mockito.any(), Mockito.any())).thenThrow(SdkClientException.class);
    schemaStore.getSchema(dataPartitionId, filePath);
  }

  @Test
  public void cleanSchemaProject() throws ApplicationException {
    String schemaId = "file";
    String dataPartitionId = "partitionid";
    Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
    doNothing().when(s3).deleteObject(Mockito.eq(serviceConfig.s3DataBucket), Mockito.eq("schema/partitionid/file"));
    Boolean result = schemaStore.cleanSchemaProject(schemaId);
    Assert.assertEquals(true, result);
  }

  @Test
  public void cleanSchemaProject_S3Exception() throws ApplicationException {
    String schemaId = "file";
    String dataPartitionId = "partitionid";
    Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
    doThrow(SdkClientException.class).when(s3).deleteObject(Mockito.eq(serviceConfig.s3DataBucket), Mockito.eq("schema/partitionid/file"));
    Boolean result = schemaStore.cleanSchemaProject(schemaId);
    Assert.assertEquals(false, result);
  }
}