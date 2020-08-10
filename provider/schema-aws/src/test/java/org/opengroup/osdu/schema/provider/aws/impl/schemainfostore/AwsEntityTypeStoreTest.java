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
import org.opengroup.osdu.core.aws.dynamodb.DynamoDBQueryHelper;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.aws.config.AwsServiceConfig;
import org.opengroup.osdu.schema.provider.aws.models.EntityTypeDoc;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AwsEntityTypeStoreTest {

  @InjectMocks
  private AwsEntityTypeStore entityTypeStore;

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

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void get() throws NotFoundException, ApplicationException {
    String entityTypeId = "id";
    String partitionId = "partitionId";
    EntityType expected = new EntityType();
    EntityTypeDoc entityTypeDoc = new EntityTypeDoc("id", partitionId, expected);

    Mockito.when(queryHelper.loadByPrimaryKey(Mockito.any(), Mockito.any())).thenReturn(entityTypeDoc);
    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);

    EntityType actual = entityTypeStore.get(entityTypeId);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void create() throws BadRequestException, ApplicationException {
    String partitionId = "partitionId";
    EntityType expected = new EntityType();

    Mockito.when(queryHelper.keyExistsInTable(Mockito.any(), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(queryHelper).save(Mockito.any());
    Mockito.when(headers.getPartitionId()).thenReturn(partitionId);
    EntityType actual = entityTypeStore.create(expected);
    assertEquals(expected, actual);
  }
}