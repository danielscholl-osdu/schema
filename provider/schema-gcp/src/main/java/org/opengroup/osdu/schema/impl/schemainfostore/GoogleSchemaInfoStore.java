package org.opengroup.osdu.schema.impl.schemainfostore;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.multitenancy.DatastoreFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.enums.SchemaScope;
import org.opengroup.osdu.schema.enums.SchemaStatus;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

/**
 * Repository class to to register Schema in Google store.
 *
 */

@Repository
public class GoogleSchemaInfoStore implements ISchemaInfoStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private DatastoreFactory dataStoreFactory;

    @Autowired
    private ITenantFactory tenantFactory;

    @Autowired
    JaxRsDpsLog log;

    @Value("${account.id.common.project}")
    private String commonAccountId;

    /**
     * Method to get schemaInfo from google store
     *
     * @param schemaId
     * @return schemaInfo object
     * @throws ApplicationException
     * @throws NotFoundException
     */
    @Override
    public SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
        Datastore datastore = dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE);
        Key key = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE).setKind(SchemaConstants.SCHEMA_KIND)
                .newKey(schemaId);
        Entity entity = datastore.get(key);
        if (entity != null) {
            return getSchemaInfoObject(entity, datastore);
        }
        throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);

    }

    /**
     * Method to Create schema in google store of tenantId GCP
     *
     * @param schema
     * @param tenantId
     * @return schemaInfo object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public SchemaInfo createSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        Datastore datastore = dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE);
        KeyFactory keyFactory = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.SCHEMA_KIND);
        Entity entity = getEntityObject(schema, datastore, keyFactory);
        try {
            datastore.add(entity);
        } catch (DatastoreException ex) {
            if (SchemaConstants.ALREADY_EXISTS.equals(ex.getReason())) {
                log.warning(SchemaConstants.SCHEMA_ID_EXISTS);
                throw new BadRequestException(SchemaConstants.SCHEMA_ID_EXISTS);
            } else {
                log.error(SchemaConstants.OBJECT_INVALID);
                throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
            }
        }
        log.info(SchemaConstants.SCHEMA_INFO_CREATED);
        return getSchemaInfoObject(entity, datastore);
    }

    /**
     * Method to update schema in google store of tenantId GCP
     *
     * @param schema
     * @param tenantId
     * @return schemaInfo object
     * @throws ApplicationException
     * @throws BadRequestException
     */
    @Override
    public SchemaInfo updateSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
        Datastore datastore = dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE);
        KeyFactory keyFactory = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.SCHEMA_KIND);
        Entity entity = getEntityObject(schema, datastore, keyFactory);
        try {
            datastore.put(entity);
        } catch (DatastoreException ex) {
            log.error(SchemaConstants.OBJECT_INVALID);
            throw new ApplicationException("Invalid object, updation failed");
        }
        log.info(SchemaConstants.SCHEMA_INFO_UPDATED);
        return getSchemaInfoObject(entity, datastore);
    }

    /**
     * Method to clean schemaInfo in google datastore of tenantId GCP
     *
     * @param schemaId
     * @return status
     * @throws ApplicationException
     */
    @Override
    public boolean cleanSchema(String schemaId) throws ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE);
        KeyFactory keyFactory = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.SCHEMA_KIND);
        Key key = keyFactory.newKey(schemaId);
        try {
            datastore.delete(key);
            return true;
        } catch (DatastoreException ex) {
            return false;
        }
    }

    @Override
    public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {
        Datastore datastore = dataStoreFactory.getDatastore(headers.getPartitionId(), SchemaConstants.NAMESPACE);
        Query<Entity> query = Query.newEntityQueryBuilder().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.SCHEMA_KIND)
                .setFilter(CompositeFilter.and(
                        PropertyFilter.eq(SchemaConstants.AUTHORITY, schemaInfo.getSchemaIdentity().getAuthority()),
                        PropertyFilter.eq(SchemaConstants.ENTITY_TYPE, schemaInfo.getSchemaIdentity().getEntityType()),
                        PropertyFilter.eq(SchemaConstants.MAJOR_VERSION,
                                schemaInfo.getSchemaIdentity().getSchemaVersionMajor()),
                        PropertyFilter.eq(SchemaConstants.SOURCE, schemaInfo.getSchemaIdentity().getSource())))
                .build();

        QueryResults<Entity> result = datastore.run(query);
        TreeMap<Long, Blob> sortedMap = new TreeMap<>(Collections.reverseOrder());
        while (result.hasNext()) {
            Entity entity = result.next();
            sortedMap.put(entity.getLong(SchemaConstants.MINOR_VERSION), entity.getBlob(SchemaConstants.SCHEMA));
        }
        if (sortedMap.size() != 0) {
            Entry<Long, Blob> blob = sortedMap.firstEntry();
            return new String(blob.getValue().toByteArray());
        }
        return new String();
    }

    private SchemaInfo getSchemaInfoObject(Entity entity, Datastore datastore) {

        SchemaIdentity superSededBy = null;
        if (entity.contains(SchemaConstants.SUPERSEDED_BY)) {
            KeyFactory keyFactory = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                    .setKind(SchemaConstants.SCHEMA_KIND);
            Entity superSededEntity = datastore.get(keyFactory.newKey(entity.getString(SchemaConstants.SUPERSEDED_BY)));
            superSededBy = getSchemaIdentity(superSededEntity);
        }

        return SchemaInfo.builder().createdBy(entity.getString(SchemaConstants.CREATED_BY))
                .dateCreated(entity.getTimestamp(SchemaConstants.DATE_CREATED).toDate())
                .scope(SchemaScope.valueOf(entity.getString(SchemaConstants.SCOPE)))
                .status(SchemaStatus.valueOf(entity.getString(SchemaConstants.STATUS)))
                .schemaIdentity(getSchemaIdentity(entity)).supersededBy(superSededBy).build();

    }

    private Entity getEntityObject(SchemaRequest schema, Datastore datastore, KeyFactory keyFactory)
            throws BadRequestException, ApplicationException {

        Key key = keyFactory.newKey(schema.getSchemaInfo().getSchemaIdentity().getId());

        Entity.Builder entityBuilder = Entity.newBuilder(key);
        if (schema.getSchemaInfo().getSupersededBy() != null) {
            if (schema.getSchemaInfo().getSupersededBy().getId() == null
                    || datastore.get(keyFactory.newKey(schema.getSchemaInfo().getSupersededBy().getId())) == null) {
                log.error(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
                throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
            }
            entityBuilder.set(SchemaConstants.SUPERSEDED_BY, schema.getSchemaInfo().getSupersededBy().getId());
        }

        entityBuilder.set(SchemaConstants.DATE_CREATED, Timestamp.now());
        entityBuilder.set(SchemaConstants.CREATED_BY, headers.getUserEmail());
        entityBuilder.set(SchemaConstants.AUTHORITY, schema.getSchemaInfo().getSchemaIdentity().getAuthority());
        entityBuilder.set(SchemaConstants.SOURCE, schema.getSchemaInfo().getSchemaIdentity().getSource());
        entityBuilder.set(SchemaConstants.ENTITY_TYPE, schema.getSchemaInfo().getSchemaIdentity().getEntityType());
        entityBuilder.set(SchemaConstants.MAJOR_VERSION,
                schema.getSchemaInfo().getSchemaIdentity().getSchemaVersionMajor());
        entityBuilder.set(SchemaConstants.MINOR_VERSION,
                schema.getSchemaInfo().getSchemaIdentity().getSchemaVersionMinor());
        entityBuilder.set(SchemaConstants.PATCH_VERSION,
                schema.getSchemaInfo().getSchemaIdentity().getSchemaVersionPatch());
        entityBuilder.set(SchemaConstants.SCOPE, schema.getSchemaInfo().getScope().name());
        entityBuilder.set(SchemaConstants.STATUS, schema.getSchemaInfo().getStatus().name());
        Gson gson = new Gson();
        Blob schemaBlob = Blob.copyFrom(gson.toJson(schema.getSchema()).getBytes());
        entityBuilder.set(SchemaConstants.SCHEMA, BlobValue.newBuilder(schemaBlob).setExcludeFromIndexes(true).build());

        return entityBuilder.build();
    }

    private SchemaIdentity getSchemaIdentity(Entity entity) {

        return SchemaIdentity.builder().id(entity.getKey().getName())
                .authority(entity.getString(SchemaConstants.AUTHORITY)).source(entity.getString(SchemaConstants.SOURCE))
                .entityType(entity.getString(SchemaConstants.ENTITY_TYPE))
                .schemaVersionMajor(entity.getLong(SchemaConstants.MAJOR_VERSION))
                .schemaVersionMinor(entity.getLong(SchemaConstants.MINOR_VERSION))
                .schemaVersionPatch(entity.getLong(SchemaConstants.PATCH_VERSION)).build();

    }

    @Override
    public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException {
        List<SchemaInfo> schemaList = new LinkedList<>();
        if (SchemaConstants.ACCOUNT_ID_COMMON_PROJECT.equals(tenantId)) {
            return schemaList;
        }
        Datastore datastore = dataStoreFactory.getDatastore(tenantId, SchemaConstants.NAMESPACE);
        List<Filter> filterList = getFilters(queryParams);

        EntityQuery.Builder queryBuilder = Query.newEntityQueryBuilder().setNamespace(SchemaConstants.NAMESPACE)
                .setKind(SchemaConstants.SCHEMA_KIND);

        if (!filterList.isEmpty()) {
            queryBuilder.setFilter(
                    CompositeFilter.and(filterList.get(0), filterList.toArray(new Filter[filterList.size()])));
        }

        QueryResults<Entity> result = datastore.run(queryBuilder.build());
        while (result.hasNext()) {
            Entity entity = result.next();
            schemaList.add(getSchemaInfoObject(entity, datastore));
        }

        return schemaList;
    }

    private List<Filter> getFilters(QueryParams queryParams) {
        List<Filter> filterList = new LinkedList<>();
        if (queryParams.getAuthority() != null) {
            filterList.add(PropertyFilter.eq(SchemaConstants.AUTHORITY, queryParams.getAuthority()));
        }
        if (queryParams.getSource() != null) {
            filterList.add(PropertyFilter.eq(SchemaConstants.SOURCE, queryParams.getSource()));
        }
        if (queryParams.getEntityType() != null) {
            filterList.add(PropertyFilter.eq(SchemaConstants.ENTITY_TYPE, queryParams.getEntityType()));
        }
        if (queryParams.getSchemaVersionMajor() != null) {
            filterList.add(PropertyFilter.eq(SchemaConstants.MAJOR_VERSION, queryParams.getSchemaVersionMajor()));
        }
        if (queryParams.getSchemaVersionMinor() != null) {
            filterList.add(PropertyFilter.eq(SchemaConstants.MINOR_VERSION, queryParams.getSchemaVersionMinor()));
        }
        if (queryParams.getSchemaVersionPatch() != null) {
            filterList.add(PropertyFilter.eq(SchemaConstants.PATCH_VERSION, queryParams.getSchemaVersionPatch()));
        }
        if (queryParams.getStatus() != null) {
            filterList.add(PropertyFilter.eq(SchemaConstants.STATUS, queryParams.getStatus().toUpperCase()));
        }
        return filterList;
    }

    @Override
    public boolean isUnique(String schemaId, String tenantId) throws ApplicationException {

        Set<String> tenantList = new HashSet<>();
        tenantList.add(commonAccountId);
        tenantList.add(tenantId);

        // code to call check uniqueness
        if (tenantId.equalsIgnoreCase(commonAccountId)) {
            List<String> privateTenantList = tenantFactory.listTenantInfo().stream().map(TenantInfo::getDataPartitionId)
                    .collect(Collectors.toList());
            tenantList.addAll(privateTenantList);

        }
        for (String tenant : tenantList) {
            Datastore datastore = dataStoreFactory.getDatastore(tenant, SchemaConstants.NAMESPACE);
            Key schemaKey = datastore.newKeyFactory().setNamespace(SchemaConstants.NAMESPACE)
                    .setKind(SchemaConstants.SCHEMA_KIND).newKey(schemaId);

            Query<Key> query = Query.newKeyQueryBuilder().setNamespace(SchemaConstants.NAMESPACE)
                    .setKind(SchemaConstants.SCHEMA_KIND).setFilter(PropertyFilter.eq("__key__", schemaKey)).build();
            QueryResults<Key> keys = datastore.run(query);
            if (keys.hasNext())
                return false;
        }
        return true;
    }

    public String getCommonAccountId() {
        return commonAccountId;
    }

    public void setCommonAccountId(String commonAccountId) {
        this.commonAccountId = commonAccountId;
    }
}