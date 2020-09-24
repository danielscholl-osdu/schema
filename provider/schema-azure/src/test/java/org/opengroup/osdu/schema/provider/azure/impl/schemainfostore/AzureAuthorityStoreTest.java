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

package org.opengroup.osdu.schema.provider.azure.impl.schemainfostore;

import com.azure.cosmos.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.azure.impl.schemainfostore.AzureAuthorityStore;
import org.opengroup.osdu.schema.azure.definitions.AuthorityDoc;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.azure.CosmosStore;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AzureAuthorityStoreTest {
    @Mock
    private CosmosStore cosmosStore;

    @InjectMocks
    private AzureAuthorityStore store;

    @Mock
    Authority mockAuthority;

    @Mock
    DpsHeaders headers;

    @Mock
    JaxRsDpsLog log;

    private static final String dataPartitionId = "testPartitionId";
    private static final String authorityId = "testAuthorityId";

    @Before
    public void init() {
        initMocks(this);
        Mockito.when(headers.getPartitionId()).thenReturn(dataPartitionId);
        Mockito.when(mockAuthority.getAuthorityId()).thenReturn(authorityId);
    }

    @Test
    public void testGetAuthority() throws NotFoundException, ApplicationException, IOException {
        AuthorityDoc authorityDoc = getAuthorityDoc(dataPartitionId, authorityId);
        Optional<AuthorityDoc> cosmosItem = Optional.of(authorityDoc);
        doReturn(cosmosItem)
                .when(cosmosStore)
                .findItem(
                        eq(dataPartitionId),
                        any(),
                        any(),
                        eq(dataPartitionId + ":" + authorityId),
                        eq(dataPartitionId),
                        any());

        assertNotNull(store.get(authorityId));
        assertEquals(authorityId, store.get(authorityId).getAuthorityId());
    }

    @Test
    public void testGetAuthority_NotFoundException() throws IOException {
        Optional<AuthorityDoc> cosmosItem = Optional.empty();
        doReturn(cosmosItem)
                .when(cosmosStore)
                .findItem(
                        eq(dataPartitionId),
                        any(),
                        any(),
                        eq(dataPartitionId + ":" + ""),
                        eq(dataPartitionId),
                        any());
        try {
            store.get("");
            fail("Should not succeed");
        } catch (NotFoundException e) {
            assertEquals("bad input parameter", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateAuthority() throws  ApplicationException, BadRequestException {
        doNothing().when(cosmosStore).createItem(eq(dataPartitionId), any(), any(), any());
        assertNotNull(store.create(mockAuthority));
    }

    @Test
    public void testCreateAuthority_BadRequestException()
            throws NotFoundException, ApplicationException, BadRequestException, IOException {
        AppException exception = getMockAppException(409);
        doThrow(exception).when(cosmosStore).createItem(eq(dataPartitionId), any(), any(), any());

        try {
            store.create(mockAuthority);
            fail("Should not succeed");
        } catch (BadRequestException e) {
            assertEquals("Authority already registered with Id: testAuthorityId", e.getMessage());

        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    @Test
    public void testCreateAuthority_ApplicationException()
            throws NotFoundException, ApplicationException, BadRequestException, CosmosClientException {
        AppException exception = getMockAppException(500);
        doThrow(exception).when(cosmosStore).createItem(eq(dataPartitionId), any(), any(), any());
        try {
            store.create(mockAuthority);
            fail("Should not succeed");
        } catch (ApplicationException e) {
            assertEquals(SchemaConstants.INVALID_INPUT, e.getMessage());
        } catch (Exception e) {
            fail("Should not get different exception");
        }
    }

    private AuthorityDoc getAuthorityDoc(String partitionId, String authorityName)
    {
        String id = partitionId + ":" + authorityName;
        Authority authority = new Authority();
        authority.setAuthorityId(authorityName);
        return new AuthorityDoc(id, partitionId, authority);
    }

    private AppException getMockAppException(int errorCode) {
        AppException mockException = mock(AppException.class);
        AppError mockError = mock(AppError.class);
        lenient().when(mockException.getError()).thenReturn(mockError);
        lenient().when(mockError.getCode()).thenReturn(errorCode);
        return mockException;
    }
}
