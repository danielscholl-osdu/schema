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
