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

import static com.mongodb.client.model.Accumulators.max;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.opengroup.osdu.schema.constants.SchemaAnthosConstants.SCHEMA_COLLECTION;
import static org.opengroup.osdu.schema.constants.SchemaAnthosConstants.SCHEMA_DATABASE;
import static org.opengroup.osdu.schema.constants.SchemaAnthosConstants.SCHEMA_IDENTITY;

import com.google.gson.Gson;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.factory.MongoClientFactory;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchemaInfoStore implements ISchemaInfoStore {

    private final MongoClientFactory mongoClientFactory;

    @Override
    public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(SCHEMA_COLLECTION);
        UpdateResult updateResult =
            collection.replaceOne(eq(schema.getSchemaInfo().getSchemaIdentity().getId()), Document.parse(new Gson().toJson(schema.getSchemaInfo())));
        if (!updateResult.wasAcknowledged()) {
            log.error(SchemaConstants.OBJECT_INVALID);
            throw new ApplicationException("Invalid object, updation failed");
        }
        return schema.getSchemaInfo();
    }

    @Override
    public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(SCHEMA_COLLECTION);
        SchemaIdentity supersededBy = schema.getSchemaInfo().getSupersededBy();
        if (Objects.nonNull(supersededBy)) {
            try {
                getSchemaInfo(supersededBy.getId());
            } catch (NotFoundException e) {
                log.error(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
                throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
            }
        }
        collection.insertOne(Document.parse(new Gson().toJson(schema.getSchemaInfo())).append("_id", schema.getSchemaInfo().getSchemaIdentity().getId()));
        return schema.getSchemaInfo();
    }

    @Override
    public SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(SCHEMA_COLLECTION);
        Document schemaIdentity = collection.find(eq(schemaId)).first();
        if (Objects.isNull(schemaIdentity)) {
            throw new NotFoundException("bad input parameter");
        }
        return new Gson().fromJson(schemaIdentity.toJson(), SchemaInfo.class);
    }

    @Override
    public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(SCHEMA_COLLECTION);
        AggregateIterable<Document> documents = collection.aggregate(Arrays.asList(match(and(
            eq(SchemaConstants.AUTHORITY, schemaInfo.getSchemaIdentity().getAuthority()),
            eq(SchemaConstants.ENTITY_TYPE, schemaInfo.getSchemaIdentity().getEntityType()),
            eq(SchemaConstants.MAJOR_VERSION, schemaInfo.getSchemaIdentity().getSchemaVersionMajor()),
            eq(SchemaConstants.SOURCE, schemaInfo.getSchemaIdentity().getSource())
        )), group(SchemaConstants.MINOR_VERSION, max(SchemaConstants.MINOR_VERSION, "$_id"))));
        return documents.first().toJson();
    }

    @Override
    public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(SCHEMA_COLLECTION);
        FindIterable<Document> documents = collection.find(getFilters(queryParams));
        return StreamSupport.stream(documents.spliterator(), false)
            .map(document -> new Gson().fromJson(document.toJson(), SchemaInfo.class))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isUnique(String schemaId, String tenantId) throws ApplicationException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(SCHEMA_COLLECTION);
        Document schemaInfo = collection.find(eq(schemaId)).first();
        return Objects.isNull(schemaInfo);
    }

    @Override
    public boolean cleanSchema(String schemaId) throws ApplicationException {
        MongoCollection<Document> collection = mongoClientFactory.getMongoClient().getDatabase(SCHEMA_DATABASE).getCollection(SCHEMA_COLLECTION);
        DeleteResult deleteResult = collection.deleteOne(eq(schemaId));
        return deleteResult.wasAcknowledged();
    }

    private Bson getFilters(QueryParams queryParams) {
        ArrayList<Bson> filters = new ArrayList<>();
        if (Objects.nonNull(queryParams.getAuthority())) {
            filters.add(eq(SCHEMA_IDENTITY + SchemaConstants.AUTHORITY, queryParams.getAuthority()));
        }
        if (Objects.nonNull(queryParams.getSource())) {
            filters.add(eq(SCHEMA_IDENTITY + SchemaConstants.SOURCE, queryParams.getSource()));
        }
        if (Objects.nonNull(queryParams.getEntityType())) {
            filters.add(eq(SCHEMA_IDENTITY + SchemaConstants.ENTITY_TYPE, queryParams.getEntityType()));
        }
        if (Objects.nonNull(queryParams.getSchemaVersionMajor())) {
            filters.add(eq(SCHEMA_IDENTITY + SchemaConstants.MAJOR_VERSION, queryParams.getSchemaVersionMajor()));
        }
        if (Objects.nonNull(queryParams.getSchemaVersionMinor())) {
            filters.add(eq(SCHEMA_IDENTITY + SchemaConstants.MINOR_VERSION, queryParams.getSchemaVersionMinor()));
        }
        if (Objects.nonNull(queryParams.getSchemaVersionPatch())) {
            filters.add(eq(SCHEMA_IDENTITY + SchemaConstants.PATCH_VERSION, queryParams.getSchemaVersionPatch()));
        }
        if (Objects.nonNull(queryParams.getStatus())) {
            filters.add(eq(SCHEMA_IDENTITY + SchemaConstants.STATUS, queryParams.getStatus().toUpperCase()));
        }
        return and(filters);
    }
}
