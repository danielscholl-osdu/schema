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

package org.opengroup.osdu.schema.provider.aws.mongo.util;

import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collection;


public final class MongoTemplateHelper<T> {

    private final MongoTemplate mongoTemplate;

    public MongoTemplateHelper(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void insert(Collection<T> items, String collection) {
        items.forEach(t -> this.insert(t, collection));
    }

    public T insert(T t, String dataPartition) {
        return this.mongoTemplate.insert(t, dataPartition);
    }

    public T findById(Object id, Class<T> clazz, String dataPartition) {
        return this.mongoTemplate.findById(id, clazz, dataPartition);
    }

    public void dropCollections() {
        this.mongoTemplate.getCollectionNames().forEach(this.mongoTemplate::dropCollection);
    }
}
