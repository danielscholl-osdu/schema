// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.schema.azure.impl.schemainfostore;

import java.text.MessageFormat;
import org.opengroup.osdu.azure.cosmosdb.CosmosStore;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.definitions.SourceDoc;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Source;
import org.opengroup.osdu.schema.provider.interfaces.schemainfostore.ISourceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Repository class to to register Source in Azure store.
 *
 *
 */
@Repository
public class AzureSourceStore implements ISourceStore {

    @Autowired
    private DpsHeaders headers;

    @Autowired
    private String sourceContainer;

    @Autowired
    private CosmosStore cosmosStore;

    @Autowired
    private String cosmosDBName;

    @Autowired
    JaxRsDpsLog log;

    /**
     * Method to create Source in azure store
     * @param sourceId
     * @return
     * @throws NotFoundException
     * @throws ApplicationException
     */
    @Override
    public Source get(String sourceId) throws NotFoundException, ApplicationException {

        String id = headers.getPartitionId().toString() + ":" + sourceId;
        SourceDoc sourceDoc = cosmosStore.findItem(headers.getPartitionId(), cosmosDBName, sourceContainer, id, headers.getPartitionId(), SourceDoc.class)
                .orElseThrow(() -> new NotFoundException("bad input parameter"));

        return sourceDoc.getSource();
    }

    /**
     * Method to create Source in azure store
     * @param source
     * @return
     * @throws BadRequestException
     * @throws ApplicationException
     */
    @Override
    public Source create(Source source) throws BadRequestException, ApplicationException {
        String id = headers.getPartitionId() + ":" + source.getSourceId();

        try {
            SourceDoc sourceDoc = new SourceDoc(id, headers.getPartitionId(), source);
            cosmosStore.createItem(headers.getPartitionId(), cosmosDBName, sourceContainer, headers.getPartitionId(), sourceDoc);
        } catch (AppException ex) {
            if (ex.getError().getCode() == 409) {
                log.warning(SchemaConstants.SOURCE_EXISTS);
                throw new BadRequestException(MessageFormat.format(SchemaConstants.SOURCE_EXISTS_EXCEPTION,
                        source.getSourceId()));
            } else {
                log.error(MessageFormat.format(SchemaConstants.OBJECT_INVALID, ex.getMessage()));
                throw new ApplicationException(SchemaConstants.INVALID_INPUT);
            }
        }

        log.info(SchemaConstants.SOURCE_CREATED);
        return source;
    }
}
