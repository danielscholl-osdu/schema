package org.opengroup.osdu.schema.provider.aws.mongo.config;

import org.mockito.Mockito;
import org.opengroup.osdu.core.aws.mongodb.MongoDBSimpleFactory;
import org.opengroup.osdu.core.aws.mongodb.config.MongoProperties;
import org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.config.MongoPropertiesReader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

@TestConfiguration
@ComponentScan(
        basePackages = {"org.opengroup.osdu"}
)
public class SchemaTestConfig {
    @Bean
    @Primary
    public MongoPropertiesReader mongoPropertiesReader() {
        MongoPropertiesReader propertiesReader = Mockito.mock(MongoPropertiesReader.class);
        MongoProperties properties = MongoProperties.builder().
                endpoint("localhost:27019").
                databaseName("test")
                .build();
        Mockito.doReturn(properties).when(propertiesReader).getProperties();
        return propertiesReader;
    }

    @Bean
    public MongoTemplate createMongoTemplate(MongoDBSimpleFactory dbSimpleFactory, MongoPropertiesReader propertiesReader) {
        return dbSimpleFactory.mongoTemplate(propertiesReader.getProperties());
    }
}