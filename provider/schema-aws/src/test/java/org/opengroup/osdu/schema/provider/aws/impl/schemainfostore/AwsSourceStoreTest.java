package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore;

import com.amazonaws.SdkClientException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.schema.provider.aws.models.SourceDoc;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AwsSourceStoreTest {

  @InjectMocks
  private AwsSourceStore sourceStore;

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
    serviceConfig.s3Endpoint = "endpoint";
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
}