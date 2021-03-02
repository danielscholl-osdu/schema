/* Licensed Materials - Property of IBM              */		
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.schema.impl.schemastore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.ibm.objectstorage.CloudObjectStorageFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;

import org.opengroup.osdu.schema.exceptions.UnauthorizedException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.model.AmazonS3Exception;
import com.ibm.cloud.objectstorage.services.s3.model.ObjectMetadata;
import com.ibm.cloud.objectstorage.services.s3.model.PutObjectRequest;

/**
 * Repository class to to register resolved Schema in IBM storage.
 * 
 *
 */
@Repository
@RequestScope
public class IBMSchemaStore implements ISchemaStore {

	@Inject
	private DpsHeaders headers;

	@Inject
	private CloudObjectStorageFactory cosFactory;
	
	@Inject
	private JaxRsDpsLog logger;
	
	@Inject
	private TenantInfo tenant;

	AmazonS3 s3Client;

	@PostConstruct
	public void init() {
		s3Client = cosFactory.getClient();
		logger.info("COS client initialized");
	}

	/**
	 * Method to get schema from IBM Storage given Tenant ProjectInfo
	 *
	 * @param dataPartitionId
	 * @param schemaId
	 * @throws NotFoundException
	 * @return schema object
	 * @throws ApplicationException
	 * @throws NotFoundException
	 */
	@Override
	public String getSchema(String dataPartitionId, String schemaId) throws ApplicationException, NotFoundException {
		// dataPartitionId not used b/c getting from header
		
		String content = null;
		try {
			tenant.getName();
		} catch (Exception e) {
			throw new UnauthorizedException("Unauthorized");
		}

		try {
			content = getObjectAsString(schemaId);
		} catch (AmazonS3Exception s3Exp) {
			if(s3Exp.getStatusCode() == 404) {
				throw new NotFoundException(HttpStatus.NOT_FOUND, SchemaConstants.SCHEMA_NOT_PRESENT);
			} 
		} catch (Exception e) {
			 throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
		}
		if (content != null)
			return content;
		throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);

	}

	/**
	 * Method to write schema to IBM Storage given Tenant ProjectInfo
	 *
	 * @param schemaId
	 * @param content
	 * @return schema object
	 * @throws ApplicationException
	 */

	@Override
	public String createSchema(String schemaId, String content) throws ApplicationException {
		
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		int bytesSize = bytes.length;

		InputStream newStream = new ByteArrayInputStream(bytes);

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bytesSize);
		
		String bucket;
		try {
			bucket = getSchemaBucketName();
			PutObjectRequest req = new PutObjectRequest(bucket, schemaId, newStream, metadata);
			s3Client.putObject(req);

		} catch (Exception e) {
			 throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
		}
		
		return s3Client.getUrl(bucket, schemaId).toString();
	}

	private String getObjectAsString(String objectName) throws Exception {
		return s3Client.getObjectAsString(getSchemaBucketName(), objectName);
	}

	private String getSchemaBucketName()  throws Exception {
		return getSchemaBucketName(headers.getPartitionIdWithFallbackToAccountId());
	}

	private String getSchemaBucketName(String dataPartitionId)  throws Exception {
		return cosFactory.getBucketName(dataPartitionId, "schema");
	}


	/**
	 * Method to clean schema object from IBM Storage given schemaId
	 *
	 * @param schemaId
	 * @return boolean
	 * @throws ApplicationException
	 */
	
	@Override
	public boolean cleanSchemaProject(String schemaId) throws ApplicationException {
		logger.info("Delete schema: " + schemaId);
		try {
			s3Client.deleteObject(getSchemaBucketName(), schemaId);
			logger.info("Scehma deleted: " + schemaId);
			return true;
		} catch (Exception  e) {
		    logger.error("Failed to delete schema " +schemaId);
		    return false;
		}
	}

}
