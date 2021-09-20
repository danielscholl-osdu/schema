/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.schema.factory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.schema.config.MongoDBConfigProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoClientFactory {

    private static final String MONGO_PREFIX = "mongodb://";
    private static final String MONGO_OPTIONS = "retryWrites=true&w=majority&maxIdleTimeMS=10000";
    private final MongoDBConfigProperties mongoDBConfigProperties;
    private MongoClient mongoClient;

    @PostConstruct
    private MongoClient init() {
        String connectionString = String.format("%s%s:%s@%s/?%s",
            MONGO_PREFIX,
            mongoDBConfigProperties.getMongoDbUser(),
            mongoDBConfigProperties.getMongoDbPassword(),
            mongoDBConfigProperties.getMongoDbUrl(),
            MONGO_OPTIONS);
        ConnectionString connString = new ConnectionString(connectionString);
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connString)
            .retryWrites(true)
            .build();
        try {
            mongoClient = MongoClients.create(settings);
        } catch (Exception ex) {
            log.error("Error connecting MongoDB", ex);
        }
        return mongoClient;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    @PreDestroy
    public void destroy() {
        if (Objects.nonNull(mongoClient)) {
            mongoClient.close();
        }
    }
}
