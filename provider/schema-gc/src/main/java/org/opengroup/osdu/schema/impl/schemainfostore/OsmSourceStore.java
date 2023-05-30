/*
 * Copyright 2020-2023 Google LLC
 * Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.schema.impl.schemainfostore;

import static org.opengroup.osdu.core.gcp.osm.model.where.predicate.Eq.eq;

import java.text.MessageFormat;
import org.apache.commons.lang3.ObjectUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.gcp.osm.model.Destination;
import org.opengroup.osdu.core.gcp.osm.model.query.GetQuery;
import org.opengroup.osdu.core.gcp.osm.model.where.Where;
import org.opengroup.osdu.core.gcp.osm.service.Context;
import org.opengroup.osdu.core.gcp.osm.translate.TranslatorRuntimeException;
import org.opengroup.osdu.schema.configuration.PropertiesConfiguration;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.stereotype.Repository;

/**
 * Repository class to register Source in KV store.
 */
@Repository
public class OsmSourceStore implements ISourceStore {

  private static final String NAME_FIELD = "name";
  private static final String SYSTEM_SOURCE_KIND = "system_source";
  private final DpsHeaders headers;
  private final DestinationProvider<Destination> destinationProvider;
  private final JaxRsDpsLog log;
  private final PropertiesConfiguration configuration;
  private final Context context;

  public OsmSourceStore(DpsHeaders headers,
      DestinationProvider<Destination> destinationProvider, JaxRsDpsLog log,
      PropertiesConfiguration configuration,
      Context context) {
    this.headers = headers;
    this.destinationProvider = destinationProvider;
    this.log = log;
    this.configuration = configuration;
    this.context = context;
  }

  @Override
  public Source get(String sourceId) throws NotFoundException, ApplicationException {
    Destination tenantDestination = getPrivateTenantDestination(this.headers.getPartitionId());

    return context.findOne(buildQueryFor(tenantDestination, eq(NAME_FIELD, sourceId)))
        .orElseThrow(() ->
            new NotFoundException("bad input parameter"));
  }

  /**
   * Method to get System Source in KV store
   *
   * @param sourceId
   * @return Source object
   * @throws NotFoundException
   * @throws ApplicationException
   */
  @Override
  public Source getSystemSource(String sourceId) throws NotFoundException, ApplicationException {
    Destination systemDestination = getSystemDestination();

    return context.findOne(buildQueryFor(systemDestination, eq(NAME_FIELD, sourceId)))
        .orElseThrow(() ->
            new NotFoundException("bad input parameter"));
  }

  @Override
  public Source create(Source source) throws BadRequestException, ApplicationException {
    Destination tenantDestination = getPrivateTenantDestination(this.headers.getPartitionId());
    checkEntityExistence(source, tenantDestination);

    Source entityFromDb;
    try {
      entityFromDb = context.createAndGet(source, tenantDestination);
    } catch (TranslatorRuntimeException ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }
    log.info(SchemaConstants.SOURCE_CREATED);
    return entityFromDb;
  }

  /**
   * Method to create System Source in KV store
   *
   * @param source
   * @return Source object
   * @throws BadRequestException
   * @throws ApplicationException
   */
  @Override
  public Source createSystemSource(Source source) throws BadRequestException, ApplicationException {
    Destination systemDestination = getSystemDestination();
    checkEntityExistence(source, systemDestination);

    Source entityFromDb;
    try {
      entityFromDb = context.createAndGet(source, systemDestination);
    } catch (TranslatorRuntimeException ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }
    log.info(SchemaConstants.SOURCE_CREATED);
    return entityFromDb;
  }

  private Destination getPrivateTenantDestination(String partitionId) {
    return destinationProvider.getDestination(
        partitionId, partitionId, SchemaConstants.SOURCE_KIND);
  }

  private Destination getSystemDestination() {
    return destinationProvider.getDestination(
        configuration.getSharedTenantName(),
        SchemaConstants.NAMESPACE,
        SYSTEM_SOURCE_KIND
    );
  }

  private GetQuery<Source> buildQueryFor(Destination destination, Where where) {
    return new GetQuery<>(Source.class, destination, where);
  }

  private void checkEntityExistence(Source source, Destination destination)
      throws BadRequestException {
    Source entityFromDb = context.getOne(
        buildQueryFor(destination, eq(NAME_FIELD, source.getSourceId())));

    if (ObjectUtils.isNotEmpty(entityFromDb)) {
      log.warning(SchemaConstants.SOURCE_EXISTS);
      throw new BadRequestException(
          MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION, source.getSourceId()));
    }
  }
}
