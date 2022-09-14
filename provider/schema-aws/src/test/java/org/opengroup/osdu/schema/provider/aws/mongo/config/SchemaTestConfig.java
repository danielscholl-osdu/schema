package org.opengroup.osdu.schema.provider.aws.mongo.config;

import org.mockito.Mockito;
import org.opengroup.osdu.core.aws.mongodb.MongoDBSimpleFactory;
import org.opengroup.osdu.core.aws.mongodb.MultiClusteredConfigReader;
import org.opengroup.osdu.core.aws.mongodb.config.MongoProperties;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.config.MultiClusteredConfigReaderSchema;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.mockito.ArgumentMatchers.any;

@TestConfiguration
public class SchemaTestConfig {

    private final MongoProperties properties = MongoProperties.builder().
            endpoint("localhost:27019").
            databaseName("test")
            .build();

    @Bean
    public MultiClusteredConfigReader configReader() {
        MultiClusteredConfigReaderSchema multiClusteredConfigReaderSchema = Mockito.mock(MultiClusteredConfigReaderSchema.class);
        Mockito.doReturn(properties).when(multiClusteredConfigReaderSchema).readProperties(any());
        return multiClusteredConfigReaderSchema;
    }

    @Bean
    public MongoTemplate createMongoTemplate(MongoDBSimpleFactory dbSimpleFactory) {

        return dbSimpleFactory.mongoTemplate(properties);
    }
}