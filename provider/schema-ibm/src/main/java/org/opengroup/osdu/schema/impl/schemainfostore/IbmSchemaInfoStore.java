/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.schema.impl.schemainfostore;

import static com.cloudant.client.api.query.Expression.eq;
import static com.cloudant.client.api.query.Operation.and;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.ibm.multitenancy.TenantFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.exceptions.UnauthorizedException;
import org.opengroup.osdu.schema.impl.schemainfostore.util.IbmDocumentStore;
import org.opengroup.osdu.schema.model.QueryParams;
import org.opengroup.osdu.schema.model.SchemaIdentity;
import org.opengroup.osdu.schema.model.SchemaInfo;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.provider.ibm.SchemaDoc;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISchemaInfoStore;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.opengroup.osdu.schema.util.VersionHierarchyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.query.Expression;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.api.query.Selector;

/**
 * Repository class to to register Schema in IBM Document Store.
 *
 */
@Repository
@RequestScope
public class IbmSchemaInfoStore extends IbmDocumentStore implements ISchemaInfoStore {
	
	private final static String AUTHORITY = "schemaIdentity.authority";
	private final static String SOURCE = "schemaIdentity.source";
	private final static String ENTITY_TYPE = "schemaIdentity.entityType";
	private final static String SCHEMA_VERSION_MAJOR = "schemaIdentity.schemaVersionMajor";
	private final static String SCHEMA_VERSION_MINOR = "schemaIdentity.schemaVersionMinor";
	private final static String SCHEMA_VERSION_PATCH = "schemaIdentity.schemaVersionPatch";
	private final static String SCOPE = "scope";
	private final static String STATUS = "status";
	
	@Inject
	private TenantFactory tenantFactory;
	
	@Inject
	private TenantInfo tenant;
	
	@Autowired
    private ISchemaStore schemaStore;
	
	@Value("${shared.tenant.name:common}")
	private String sharedTenant;

	
	@PostConstruct
	public void init() throws MalformedURLException {
		initFactory(SCHEMA_DATABASE);
	}


	@Override
	public boolean isUnique(String schemaId, String tenantId) throws ApplicationException {
		try {
			tenant.getName();
		} catch (Exception e) {
			throw new UnauthorizedException("Unauthorized");
		}
		Set<String> tenantList = new HashSet<>();
		tenantList.add(sharedTenant);
		tenantList.add(tenantId);

		// code to call check uniqueness
		if (tenantId.equalsIgnoreCase(sharedTenant)) {
			List<String> privateTenantList = tenantFactory.listTenantInfo().stream().map(TenantInfo::getDataPartitionId)
					.collect(Collectors.toList());
			tenantList.addAll(privateTenantList);

		}

		for (String tenant : tenantList) {
			try {
				if (getDatabaseForTenant(tenant, SCHEMA_DATABASE).contains(schemaId)) {
					return false; // DB exists and contains schemaId, so is not unique
				}
			} catch (MalformedURLException e) {
				// DB doesn't exist, so its unique
				throw new ApplicationException("Unable to find database for tenant " + tenant);
			}
		}

		return true;
	}

	@Override
	public boolean isUniqueSystemSchema(String schemaId) throws ApplicationException {
		return this.isUnique(schemaId, sharedTenant);
	}

	/**
	 * Method to get schemaInfo from IBM store
	 *
	 * @param schemaId
	 * @return schemaInfo object
	 * @throws ApplicationException
	 * @throws NotFoundException
	 */
	@Override
	public SchemaInfo getSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
		try {
			tenant.getName();
		} catch (Exception e) {
			throw new UnauthorizedException("Unauthorized");
		}
		if (db.contains(schemaId)) {
			SchemaDoc sd = db.find(SchemaDoc.class, schemaId);
			return sd.getSchemaInfo();
		} else {
			throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
		}
		
	}

	/**
	 * Method to get System schemaInfo from IBM store
	 * @param schemaId
	 * @return	Schema info object
	 * @throws ApplicationException
	 * @throws NotFoundException
	 */
	@Override
	public SchemaInfo getSystemSchemaInfo(String schemaId) throws ApplicationException, NotFoundException {
		this.updateDataPartitionId();
		return this.getSchemaInfo(schemaId);
	}

	/**
	 * Method to Create schema in IBM store
	 *
	 * @param schemaRequest
	 * @return schemaInfo object
	 * @throws ApplicationException
	 * @throws BadRequestException
	 */
	@Override
	public SchemaInfo createSchemaInfo(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException {

		String schemaId = schemaRequest.getSchemaInfo().getSchemaIdentity().getId();
		
		if (db.contains(schemaId)) {
			logger.warning("Schema " + schemaId + " already exist. Can't create again.");
			throw new BadRequestException("Schema " + schemaId + " already exist. Can't create again.");
		}
		validateSupercededBy(schemaRequest);
		SchemaDoc sd = new SchemaDoc(schemaRequest.getSchemaInfo());
		try {
			Date date = new Date();
            long now = date.getTime();
			sd.setCreatedDate(now);
			db.save(sd);
		} catch (Exception ex) {
			logger.error(SchemaConstants.OBJECT_INVALID);
			throw new ApplicationException(SchemaConstants.SCHEMA_CREATION_FAILED_INVALID_OBJECT);
		}
		logger.info(SchemaConstants.SCHEMA_CREATED);
		return sd.getSchemaInfo();
	}

	/**
	 * Method to Create System schema in IBM store
	 * @param schema
	 * @return
	 * @throws ApplicationException
	 * @throws BadRequestException
	 */
	@Override
	public SchemaInfo createSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
		this.updateDataPartitionId();
		return this.createSchemaInfo(schema);
	}

	/**
	 * Method to update schema in IBM store
	 *
	 * @param schemaRequest
	 * @return schemaInfo object
	 * @throws ApplicationException
	 * @throws BadRequestException
	 */
	@Override
	public SchemaInfo updateSchemaInfo(SchemaRequest schemaRequest) throws ApplicationException, BadRequestException {
		String schemaId = schemaRequest.getSchemaInfo().getSchemaIdentity().getId();
		validateSupercededBy(schemaRequest);
		SchemaDoc sdStored = db.find(SchemaDoc.class, schemaId);
		SchemaDoc sd = new SchemaDoc(schemaRequest.getSchemaInfo());
		sd.setRev(sdStored.getRev());

		try {
			db.update(sd);
		} catch (Exception ex) {
			logger.error(SchemaConstants.OBJECT_INVALID, ex);
			throw new ApplicationException("Invalid object, updation failed");
		}
		return sd.getSchemaInfo();
	}

	/**
	 * Method to update System schema in IBM store
	 * @param schema
	 * @return
	 * @throws ApplicationException
	 * @throws BadRequestException
	 */
	@Override
	public SchemaInfo updateSystemSchemaInfo(SchemaRequest schema) throws ApplicationException, BadRequestException {
		this.updateDataPartitionId();
		return this.updateSchemaInfo(schema);
	}

	/**
	 * Method to clean schemaInfo in IBM datastore of tenantId
	 *
	 * @param schemaId
	 * @return status
	 * @throws ApplicationException
	 */
	@Override
	public boolean cleanSchema(String schemaId) throws ApplicationException {

		try {
			if (db.contains(schemaId)) {
				SchemaDoc sd = db.find(SchemaDoc.class, schemaId);
				db.remove(sd);
				return true;
			}
			return false;

		} catch (Exception ex) {
			logger.error(SchemaConstants.OBJECT_INVALID, ex);
			return false;
		}

	}

	/**
	 * Method to clean System schemaInfo in IBM datastore
	 * @param schemaId
	 * @return
	 * @throws ApplicationException
	 */
	@Override
	public boolean cleanSystemSchema(String schemaId) throws ApplicationException {
		this.updateDataPartitionId();
		return this.cleanSchema(schemaId);
	}

	@Override
	public String getLatestMinorVerSchema(SchemaInfo schemaInfo) throws ApplicationException {

		QueryResult<SchemaDoc> results = db.query(new QueryBuilder(getSelectorForMinorVerSchema(schemaInfo)).build(),
				SchemaDoc.class);
		TreeMap<Long, SchemaDoc> sortedMap = new TreeMap<>(Collections.reverseOrder());

		for (SchemaDoc doc : results.getDocs()) {
			sortedMap.put(doc.getSchemaInfo().getSchemaIdentity().getSchemaVersionMinor(), doc);
		}

		if (sortedMap.size() != 0) {
			Entry<Long, SchemaDoc> sd = sortedMap.firstEntry();
			
			SchemaDoc doc  = sd.getValue();
			try {
				return schemaStore.getSchema(null, doc.getId());
			} catch (NotFoundException e) {
				return new String();
			}
		}

		return new String();
	}


	private void validateSupercededBy(SchemaRequest schemaRequest) throws BadRequestException{
		if (schemaRequest.getSchemaInfo().getSupersededBy() != null) {
			if (schemaRequest.getSchemaInfo().getSupersededBy().getId() == null
                    || !db.contains(schemaRequest.getSchemaInfo().getSupersededBy().getId())) {
				logger.error(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
                throw new BadRequestException(SchemaConstants.INVALID_SUPERSEDEDBY_ID);
            } 
			 
		 }
	}
	
	private Selector getSelectorForMinorVerSchema(SchemaInfo schemaInfo) {
		return and(
				eq(AUTHORITY, schemaInfo.getSchemaIdentity().getAuthority()),
				eq(ENTITY_TYPE, schemaInfo.getSchemaIdentity().getEntityType()),
				eq(SCHEMA_VERSION_MAJOR, schemaInfo.getSchemaIdentity().getSchemaVersionMajor()),
				eq(SOURCE, schemaInfo.getSchemaIdentity().getSource())
		);
	}

	@Override
	/**
	 * Modefied the method on May 24th 2021, for having a configurable Max limit on a open select (select with no query params)
	 */
	public List<SchemaInfo> getSchemaInfoList(QueryParams queryParams, String tenantId) throws ApplicationException {
		try {
			tenant.getName();
		} catch (Exception e) {
			throw new UnauthorizedException("Unauthorized");
		}
		List<SchemaDoc> listSchemaDocs=new ArrayList<>();
		List<SchemaDoc> filterSchemaDocs=new ArrayList<>();
		Database dbWithTenant = null;
		
		try {
			dbWithTenant = getDatabaseForTenant(tenantId, SCHEMA_DATABASE);
		} catch (MalformedURLException e) {
			throw new ApplicationException("Unable to find database for tenant " + tenantId);
		}
		try {
			 listSchemaDocs=dbWithTenant.getAllDocsRequestBuilder().includeDocs(true).build().getResponse().getDocsAs(SchemaDoc.class);
			 filterSchemaDocs=getSchemaRecordsBasedOnFilterCheck(listSchemaDocs,queryParams);
		} catch (IOException e) {
			throw new ApplicationException("Unable to find schema records for tenant " + tenantId);
		}
		List<SchemaInfo> schemaList = new LinkedList<>();
		for (SchemaDoc doc : filterSchemaDocs) {
			schemaList.add(doc.getSchemaInfo());
		}
        if (queryParams.getLatestVersion() != null && queryParams.getLatestVersion()) {
            return getLatestVersionSchemaList(schemaList);
        }
        logger.info("list of filter Schema records:"+filterSchemaDocs.size());
		return schemaList;
		}

	/**
	 * Method to fetch system schema info list
	 * @param queryParams
	 * @return
	 * @throws ApplicationException
	 */
	@Override
	public List<SchemaInfo> getSystemSchemaInfoList(QueryParams queryParams) throws ApplicationException {
		return this.getSchemaInfoList(queryParams, sharedTenant);
	}

	private Selector getSelector(QueryParams queryParams) throws ApplicationException {

		HashMap<String, Object> filterHashMap = getFilters(queryParams);

		List<Expression> expressionList = new ArrayList<Expression>();

		for (String filter : filterHashMap.keySet()) {
			try {
				expressionList.add(eq(filter, (Long) filterHashMap.get(filter)));
			} catch (ClassCastException e) {
				expressionList.add(eq(filter, filterHashMap.get(filter)));
			}
		}

		Selector finalSelector = null;
		if (expressionList.size() == 1)
			finalSelector = expressionList.get(0);
		else if (expressionList.size() > 1) {
			Expression[] expressionArray = expressionList.toArray(new Expression[0]);
			finalSelector = and(expressionArray);
		}
		return finalSelector;

	}

	private HashMap<String, Object> getFilters(QueryParams queryParams) {
		HashMap<String, Object> filterHashMap = new HashMap<String, Object>();
		if (queryParams.getAuthority() != null) {
			filterHashMap.put(AUTHORITY, queryParams.getAuthority());
		}
		if (queryParams.getSource() != null) {
			filterHashMap.put(SOURCE, queryParams.getSource());
		}
		if (queryParams.getEntityType() != null) {
			filterHashMap.put(ENTITY_TYPE, queryParams.getEntityType());
		}
		if (queryParams.getSchemaVersionMajor() != null) {
			filterHashMap.put(SCHEMA_VERSION_MAJOR,
					queryParams.getSchemaVersionMajor());
		}
		if (queryParams.getSchemaVersionMinor() != null) {
			filterHashMap.put(SCHEMA_VERSION_MINOR,
					queryParams.getSchemaVersionMinor());
		}
		if (queryParams.getSchemaVersionPatch() != null) {
			filterHashMap.put(SCHEMA_VERSION_PATCH, queryParams.getSchemaVersionPatch());
		}
		if (queryParams.getScope() != null) {
			filterHashMap.put(SCOPE, queryParams.getScope());
		}
		if (queryParams.getStatus() != null) {
			filterHashMap.put(STATUS, queryParams.getStatus());
		}
		return filterHashMap;
	}

    private List<SchemaInfo> getLatestVersionSchemaList(List<SchemaInfo> filteredSchemaList) {
        List<SchemaInfo> latestSchemaList = new LinkedList<>();
        SchemaInfo previousSchemaInfo = null;
        TreeMap<VersionHierarchyUtil, SchemaInfo> sortedMap = new TreeMap<>(
                new VersionHierarchyUtil.SortingVersionComparator());

        for (SchemaInfo schemaInfoObject : filteredSchemaList) {
            if ((previousSchemaInfo != null) && !(checkAuthorityMatch(previousSchemaInfo, schemaInfoObject)
                    && checkSourceMatch(previousSchemaInfo, schemaInfoObject)
                    && checkEntityMatch(previousSchemaInfo, schemaInfoObject))) {
                Entry<VersionHierarchyUtil, SchemaInfo> latestVersionEntry = sortedMap.firstEntry();
                latestSchemaList.add(latestVersionEntry.getValue());
                sortedMap.clear();
            }
            previousSchemaInfo = schemaInfoObject;
            SchemaIdentity schemaIdentity = schemaInfoObject.getSchemaIdentity();
            VersionHierarchyUtil version = new VersionHierarchyUtil(schemaIdentity.getSchemaVersionMajor(),
                    schemaIdentity.getSchemaVersionMinor(), schemaIdentity.getSchemaVersionPatch());
            sortedMap.put(version, schemaInfoObject);
        }
        if (sortedMap.size() != 0) {
            Entry<VersionHierarchyUtil, SchemaInfo> latestVersionEntry = sortedMap.firstEntry();
            latestSchemaList.add(latestVersionEntry.getValue());
        }

        return latestSchemaList;
    }

    private boolean checkEntityMatch(SchemaInfo previousSchemaInfo, SchemaInfo schemaInfoObject) {
        return schemaInfoObject.getSchemaIdentity().getEntityType()
                .equalsIgnoreCase(previousSchemaInfo.getSchemaIdentity().getEntityType());
    }

    private boolean checkSourceMatch(SchemaInfo previousSchemaInfo, SchemaInfo schemaInfoObject) {
        return schemaInfoObject.getSchemaIdentity().getSource()
                .equalsIgnoreCase(previousSchemaInfo.getSchemaIdentity().getSource());
    }

    private boolean checkAuthorityMatch(SchemaInfo previousSchemaInfo, SchemaInfo schemaInfoObject) {
        return schemaInfoObject.getSchemaIdentity().getAuthority()
                .equalsIgnoreCase(previousSchemaInfo.getSchemaIdentity().getAuthority());
    }
    
    public List<SchemaDoc>  getSchemaRecordsBasedOnFilterCheck(List<SchemaDoc> listSchemaDocs,QueryParams queryParams)
    {
    	 Stream<SchemaDoc> schemaDoc=listSchemaDocs.stream();
    	     if(StringUtils.isNotEmpty(queryParams.getAuthority()))
    	     schemaDoc=schemaDoc.filter(p->p.getSchemaInfo().getSchemaIdentity().getAuthority().equals(queryParams.getAuthority()));
    	    
    		 if(StringUtils.isNotEmpty(queryParams.getEntityType()))
    		 schemaDoc=schemaDoc.filter(p->p.getSchemaInfo().getSchemaIdentity().getEntityType().equals(queryParams.getEntityType()));
    		 
    	     if(StringUtils.isNotEmpty(queryParams.getSource()))
    	     schemaDoc=schemaDoc.filter(p->p.getSchemaInfo().getSchemaIdentity().getSource().equals(queryParams.getSource()));
    	    
    		 if(StringUtils.isNotEmpty(queryParams.getStatus()))
    		 schemaDoc=schemaDoc.filter(p->p.getSchemaInfo().getStatus().name().equals(queryParams.getStatus()));
    		 
    		 if(StringUtils.isNotEmpty(queryParams.getScope()))
    		 schemaDoc=schemaDoc.filter(p->p.getSchemaInfo().getScope().name().equals(queryParams.getScope()));
    		 
    		 if(queryParams.getSchemaVersionMajor()!=null)
    		 schemaDoc=schemaDoc.filter(p->p.getSchemaInfo().getSchemaIdentity().getSchemaVersionMajor().equals(queryParams.getSchemaVersionMajor()));
    		 
    		 if(queryParams.getSchemaVersionMinor()!=null)
    		 schemaDoc=schemaDoc.filter(p->p.getSchemaInfo().getSchemaIdentity().getSchemaVersionMinor().equals(queryParams.getSchemaVersionMinor()));
    		 
    		 if(queryParams.getSchemaVersionPatch()!=null)
    		 schemaDoc=schemaDoc.filter(p->p.getSchemaInfo().getSchemaIdentity().getSchemaVersionPatch().equals(queryParams.getSchemaVersionPatch()));
    		 
    	return schemaDoc.collect(Collectors.toList());
    	
    }
}
