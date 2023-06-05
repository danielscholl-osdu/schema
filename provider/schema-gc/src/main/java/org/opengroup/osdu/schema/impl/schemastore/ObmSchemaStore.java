/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.schema.impl.schemastore;

import static org.opengroup.osdu.core.gcp.obm.driver.S3CompatibleErrors.NO_SUCH_KEY;

import com.google.cloud.storage.StorageException;
import java.nio.charset.StandardCharsets;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.gcp.obm.driver.Driver;
import org.opengroup.osdu.core.gcp.obm.driver.ObmDriverRuntimeException;
import org.opengroup.osdu.core.gcp.obm.model.Blob;
import org.opengroup.osdu.core.gcp.obm.persistence.ObmDestination;
import org.opengroup.osdu.schema.configuration.PropertiesConfiguration;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/**
 * Repository class to register resolved Schema in Blob storage.
 */
@Repository
public class ObmSchemaStore implements ISchemaStore {

  private static final String SCHEMA_BUCKET_EXTENSION = "-system-schema";
  private final ITenantFactory tenantFactory;
  private final DpsHeaders headers;
  private final DestinationProvider<ObmDestination> destinationProvider;
  private final JaxRsDpsLog log;
  private final PropertiesConfiguration configuration;
  private final Driver driver;

  public ObmSchemaStore(ITenantFactory tenantFactory, DpsHeaders headers,
      DestinationProvider<ObmDestination> destinationProvider, JaxRsDpsLog log,
      PropertiesConfiguration configuration,
      Driver driver) {
    this.tenantFactory = tenantFactory;
    this.headers = headers;
    this.destinationProvider = destinationProvider;
    this.log = log;
    this.configuration = configuration;
    this.driver = driver;
  }

  /**
   * Method to get schema from Blob Storage given Tenant ProjectInfo
   *
   * @param dataPartitionId
   * @param filePath
   * @return schema object
   * @throws ApplicationException
   * @throws NotFoundException
   */
  @Override
  public String getSchema(String dataPartitionId, String filePath)
      throws ApplicationException, NotFoundException {
    filePath = filePath + SchemaConstants.JSON_EXTENSION;
    String bucketName = getSchemaBucketName(dataPartitionId);

    byte[] blob = null;

    try {
      blob = driver.getBlobContent(bucketName, filePath,
          getDestination(this.headers.getPartitionId()));
    } catch (ObmDriverRuntimeException | NullPointerException ex) {
      if (isNotFoundException(ex)) {
        log.warning(ex.getMessage());
        throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
      } else {
        throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
      }
    }

    if (blob != null) {
      return new String(blob, StandardCharsets.UTF_8);
    }
    throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
  }

  private boolean isNotFoundException(RuntimeException ex) {
    if (ex instanceof NullPointerException) {
      return true;
    }

    ObmDriverRuntimeException obmException = (ObmDriverRuntimeException) ex;

    if (obmException.getCause() instanceof IllegalArgumentException) {
      return NO_SUCH_KEY.equals(obmException.getError());
    }

    if (obmException.getCause() instanceof StorageException) {
      return HttpStatus.NOT_FOUND.value()
          == (((StorageException) obmException.getCause()).getCode());
    }

    return false;
  }

  /**
   * Method to get System schema from Blob Storage
   *
   * @param filePath
   * @return Schema object
   * @throws NotFoundException
   * @throws ApplicationException
   */
  @Override
  public String getSystemSchema(String filePath) throws NotFoundException, ApplicationException {
    filePath = filePath + SchemaConstants.JSON_EXTENSION;
    String systemSchemaBucketName = getSystemSchemaBucketName();

    byte[] blob = null;

    try {
      blob = driver.getBlobContent(systemSchemaBucketName, filePath, getSystemDestination());
    } catch (ObmDriverRuntimeException | NullPointerException ex) {
      if (isNotFoundException(ex)) {
        log.warning(ex.getMessage());
        throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
      } else {
        throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
      }
    }

    if (blob != null) {
      return new String(blob, StandardCharsets.UTF_8);
    }
    throw new NotFoundException(SchemaConstants.SCHEMA_NOT_PRESENT);
  }

  /**
   * Method to write schema to Blob Storage given Tenant ProjectInfo
   *
   * @param filePath
   * @param content
   * @return schema object
   * @throws ApplicationException
   */

  @Override
  public String createSchema(String filePath, String content) throws ApplicationException {
    String dataPartitionId = headers.getPartitionId();
    filePath = filePath + SchemaConstants.JSON_EXTENSION;
    String bucketName = getSchemaBucketName(dataPartitionId);

    Blob blob = Blob.builder()
        .bucket(bucketName)
        .name(filePath)
        .build();

    try {
      Blob blobFromStorage = driver.createAndGetBlob(blob, content.getBytes(StandardCharsets.UTF_8),
          getDestination(this.headers.getPartitionId()));
      log.info(SchemaConstants.SCHEMA_CREATED);
      return blobFromStorage.getName();
    } catch (ObmDriverRuntimeException ex) {
      throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Method to write System schema to Blob Storage
   *
   * @param filePath
   * @param content
   * @return schema object
   * @throws ApplicationException
   */
  @Override
  public String createSystemSchema(String filePath, String content) throws ApplicationException {
    filePath = filePath + SchemaConstants.JSON_EXTENSION;
    String systemSchemaBucketName = getSystemSchemaBucketName();

    Blob blob = Blob.builder()
        .bucket(systemSchemaBucketName)
        .name(filePath)
        .build();

    try {
      Blob blobFromStorage = driver.createAndGetBlob(blob, content.getBytes(StandardCharsets.UTF_8),
          getSystemDestination());
      log.info(SchemaConstants.SCHEMA_CREATED);
      return blobFromStorage.getName();
    } catch (ObmDriverRuntimeException ex) {
      throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public boolean cleanSchemaProject(String schemaId) {
    String dataPartitionId = headers.getPartitionId();
    String fileName = schemaId + SchemaConstants.JSON_EXTENSION;
    String bucketName = getSchemaBucketName(dataPartitionId);
    return driver.deleteBlob(bucketName, fileName, getDestination(this.headers.getPartitionId()));
  }

  /**
   * Method to clean System schema from Blob Storage
   *
   * @param schemaId
   * @return
   * @throws ApplicationException
   */
  @Override
  public boolean cleanSystemSchemaProject(String schemaId) {
    String fileName = schemaId + SchemaConstants.JSON_EXTENSION;
    String systemSchemaBucketName = getSystemSchemaBucketName();
    return driver.deleteBlob(systemSchemaBucketName, fileName, getSystemDestination());
  }

  private String getSchemaBucketName(String dataPartitionId) {
    TenantInfo tenantInfo = tenantFactory.getTenantInfo(dataPartitionId);
    return String.format("%s-%s%s", tenantInfo.getProjectId(), tenantInfo.getName(),
        SchemaConstants.SCHEMA_BUCKET_EXTENSION);
  }

  private String getSystemSchemaBucketName() {
    TenantInfo tenantInfo = tenantFactory.getTenantInfo(configuration.getSharedTenantName());
    return String.format("%s-%s%s", tenantInfo.getProjectId(), tenantInfo.getName(),
        SCHEMA_BUCKET_EXTENSION);
  }

  private ObmDestination getDestination(String partitionId) {
    return destinationProvider.getDestination(partitionId);
  }

  private ObmDestination getSystemDestination() {
    return destinationProvider.getDestination(configuration.getSharedTenantName());
  }

}
