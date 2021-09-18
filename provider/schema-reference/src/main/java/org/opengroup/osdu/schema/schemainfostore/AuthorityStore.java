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
import static org.opengroup.osdu.schema.constants.SchemaAnthosConstants.AUTHORITY_COLLECTION;
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
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorityStore implements IAuthorityStore {

    private final MongoClientFactory mongoClientFactory;

    @Override
    public Authority get(String authorityId) throws NotFoundException, ApplicationException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(AUTHORITY_COLLECTION);
        Document authorityDoc = collection.find(eq("authorityId", authorityId)).first();
        if (Objects.isNull(authorityDoc)) {
            throw new NotFoundException("bad input parameter");
        }
        return new Gson().fromJson(authorityDoc.toJson(), Authority.class);
    }

    @Override
    public Authority create(Authority authority) throws ApplicationException, BadRequestException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(AUTHORITY_COLLECTION);
        collection.insertOne(Document.parse(new Gson().toJson(authority)));
        return authority;
    }
}
