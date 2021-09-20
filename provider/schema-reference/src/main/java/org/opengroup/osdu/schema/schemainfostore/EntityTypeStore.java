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

package org.opengroup.osdu.schema.schemainfostore;

import static com.mongodb.client.model.Filters.eq;
import static org.opengroup.osdu.schema.constants.SchemaAnthosConstants.ENTITY_TYPE_COLLECTION;
import static org.opengroup.osdu.schema.constants.SchemaAnthosConstants.SCHEMA_DATABASE;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.factory.MongoClientFactory;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IEntityTypeStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntityTypeStore implements IEntityTypeStore {

    private final MongoClientFactory mongoClientFactory;

    @Override
    public EntityType get(String entityTypeId) throws NotFoundException, ApplicationException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(ENTITY_TYPE_COLLECTION);
        Document entityType = collection.find(eq("entityTypeId", entityTypeId)).first();
        if (Objects.isNull(entityType)) {
            throw new NotFoundException("bad input parameter");
        }
        return new Gson().fromJson(entityType.toJson(), EntityType.class);
    }

    @Override
    public EntityType create(EntityType entityType) throws BadRequestException, ApplicationException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(ENTITY_TYPE_COLLECTION);
        collection.insertOne(Document.parse(new Gson().toJson(entityType)));
        return entityType;
    }
}
