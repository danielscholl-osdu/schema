package org.opengroup.osdu.schema.provider.aws.mongo.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opengroup.osdu.core.aws.partition.PartitionInfoAws;
import org.opengroup.osdu.core.aws.partition.PartitionServiceClientWithCache;
import org.opengroup.osdu.core.common.http.DpsHeaderFactory;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.provider.interfaces.authorization.IAuthorizationServiceForServiceAdmin;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.anyString;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class ParentUtil {
    public static final String DATA_PARTITION = "osdu_schema";

    public MongoTemplateHelper mongoTemplateHelper;

    @MockBean
    protected DpsHeaderFactory headers;
    @MockBean
    private PartitionServiceClientWithCache partitionServiceClient;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private ISchemaStore awsSchemaStore;
    @MockBean
    private IAuthorizationServiceForServiceAdmin IAuthorizationServiceForServiceAdmin;

    @Before
    public void setUpAnyTime() {
        MockitoAnnotations.openMocks(this);
        Mockito.when(headers.getPartitionId()).thenReturn(DATA_PARTITION);
        PartitionInfoAws partitionInfoAws = new PartitionInfoAws();
        Property tenantIdProperty = new Property();
        tenantIdProperty.setValue(DATA_PARTITION);
        partitionInfoAws.setTenantIdProperty(tenantIdProperty);
        Mockito.when(partitionServiceClient.getPartition(anyString())).thenReturn(partitionInfoAws);
    }


    @Rule
    public ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() {
            ParentUtil.this.mongoTemplateHelper.dropCollections();
        }

        @Override
        protected void after() {
            ParentUtil.this.mongoTemplateHelper.dropCollections();
            ParentUtil.this.headers.put(SchemaConstants.DATA_PARTITION_ID, null);
            ParentUtil.this.headers.put("user", null);
        }
    };

    @Autowired
    public void set(MongoTemplate mongoTemplate) {
        this.mongoTemplateHelper = new MongoTemplateHelper(mongoTemplate);
    }


}