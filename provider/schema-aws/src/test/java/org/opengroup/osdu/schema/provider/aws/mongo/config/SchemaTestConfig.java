// Copyright © Amazon Web Services
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