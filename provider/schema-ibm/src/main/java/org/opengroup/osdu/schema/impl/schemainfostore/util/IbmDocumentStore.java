package org.opengroup.osdu.schema.impl.schemainfostore.util;

import java.net.MalformedURLException;

import javax.inject.Inject;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.ibm.auth.ServiceCredentials;
import org.opengroup.osdu.core.ibm.cloudant.IBMCloudantClientFactory;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.springframework.beans.factory.annotation.Value;

import com.cloudant.client.api.Database;

/**
 * Repository class to to register authority in IBM store.
 *
 */
public class IbmDocumentStore {
	
	public static final String SCHEMA_DATABASE = "schema2";
	public static final long LIMIT_SIZE = 25;
	
	@Value("${ibm.db.url}")
	private String dbUrl;
	@Value("${ibm.db.user:#{null}}")
	private String dbUser;
	@Value("${ibm.db.password:#{null}}")
	private String dbPassword;
	@Value("${ibm.env.prefix:local-dev}")
	private String dbNamePrefix;
   
	@Inject
    private DpsHeaders headers;
	
	@Inject
	protected JaxRsDpsLog logger;
	
	private IBMCloudantClientFactory cloudantFactory;
	protected Database db = null;
	
	public void initFactory(String dbName) throws MalformedURLException {
		cloudantFactory = new IBMCloudantClientFactory(new ServiceCredentials(dbUrl, dbUser, dbPassword));
		db = cloudantFactory.getDatabase(dbNamePrefix, SchemaConstants.NAMESPACE + "-" + headers.getPartitionIdWithFallbackToAccountId() + "-" + dbName);
	}
	
	public Database getDatabaseForTenant(String tenantId, String dbName) throws MalformedURLException {
		return cloudantFactory.getDatabase(dbNamePrefix, SchemaConstants.NAMESPACE + "-" + tenantId + "-" + dbName);
	}

}
