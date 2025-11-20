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


import static org.opengroup.osdu.core.osm.core.model.where.predicate.Eq.eq;

import java.text.MessageFormat;
import org.apache.commons.lang3.ObjectUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.osm.core.model.Destination;
import org.opengroup.osdu.core.osm.core.model.query.GetQuery;
import org.opengroup.osdu.core.osm.core.model.where.Where;
import org.opengroup.osdu.core.osm.core.service.Context;
import org.opengroup.osdu.core.osm.core.translate.TranslatorRuntimeException;
import org.opengroup.osdu.schema.configuration.PropertiesConfiguration;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.destination.provider.DestinationProvider;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.IAuthorityStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository class to register authority in KV Store using OSM
 */
@Repository
public class OsmAuthorityStore implements IAuthorityStore {

  private static final String SYSTEM_AUTHORITY_KIND = "system_authority";
  private static final String NAME_FIELD = "name";
  private final DpsHeaders headers;
  private final DestinationProvider<Destination> destinationProvider;
  private final JaxRsDpsLog log;
  private final PropertiesConfiguration configuration;
  private final Context context;

  @Autowired
  public OsmAuthorityStore(DpsHeaders headers, DestinationProvider<Destination> destinationProvider,
      JaxRsDpsLog log, Context context,
      PropertiesConfiguration configuration) {
    this.headers = headers;
    this.destinationProvider = destinationProvider;
    this.log = log;
    this.configuration = configuration;
    this.context = context;
  }

  @Override
  public Authority get(String authorityId) throws NotFoundException, ApplicationException {
    Destination destination = getPrivateTenantDestination(this.headers.getPartitionId());

    return context.findOne(buildQueryFor(destination, eq(NAME_FIELD, authorityId)))
        .orElseThrow(() ->
            new NotFoundException("bad input parameter"));
  }

  @Override
  public Authority getSystemAuthority(String authorityId)
      throws NotFoundException, ApplicationException {
    Destination systemDestination = getSystemDestination();

    return context.findOne(buildQueryFor(systemDestination, eq(NAME_FIELD, authorityId)))
        .orElseThrow(() ->
            new NotFoundException("bad input parameter"));
  }

  @Override
  public Authority create(Authority authority) throws ApplicationException, BadRequestException {
    Destination tenantDestination = getPrivateTenantDestination(this.headers.getPartitionId());
    checkEntityExistence(authority, tenantDestination);

    Authority entityFromDb;
    try {
      entityFromDb = context.createAndGet(tenantDestination, authority);
    } catch (TranslatorRuntimeException ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }
    log.info(SchemaConstants.AUTHORITY_CREATED);
    return entityFromDb;
  }

  @Override
  public Authority createSystemAuthority(Authority authority)
      throws ApplicationException, BadRequestException {
    Destination systemDestination = getSystemDestination();
    checkEntityExistence(authority, systemDestination);

    Authority entityFromDb;
    try {
      entityFromDb = context.createAndGet(systemDestination, authority);
    } catch (TranslatorRuntimeException ex) {
      log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
      throw new ApplicationException(SchemaConstants.INVALID_INPUT);
    }
    log.info(SchemaConstants.AUTHORITY_CREATED);
    return entityFromDb;
  }

  private GetQuery<Authority> buildQueryFor(Destination destination, Where where) {
    return new GetQuery<>(Authority.class, destination, where);
  }

  private void checkEntityExistence(Authority authority, Destination destination)
      throws BadRequestException {
    Authority entityFromDb =
        context.getOne(buildQueryFor(destination, eq(NAME_FIELD, authority.getAuthorityId())));

    if (ObjectUtils.isNotEmpty(entityFromDb)) {
      log.warning(SchemaConstants.AUTHORITY_EXISTS_ALREADY_REGISTERED);
      throw new BadRequestException(
          MessageFormat.format(SchemaConstants.AUTHORITY_EXISTS_EXCEPTION,
              authority.getAuthorityId()));
    }
  }

  private Destination getPrivateTenantDestination(String partitionId) {
    return destinationProvider.getDestination(
        partitionId, partitionId, SchemaConstants.AUTHORITY_KIND);
  }

  private Destination getSystemDestination() {
    return destinationProvider.getDestination(
        configuration.getSharedTenantName(),
        SchemaConstants.NAMESPACE,
        SYSTEM_AUTHORITY_KIND
    );
  }
}
