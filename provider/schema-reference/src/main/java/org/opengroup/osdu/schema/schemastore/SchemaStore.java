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

package org.opengroup.osdu.schema.schemastore;

import static org.opengroup.osdu.schema.constants.SchemaAnthosConstants.MINIO_SCHEMA_BUCKET;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.NotFoundException;
import org.opengroup.osdu.schema.factory.CloudObjectStorageFactory;
import org.opengroup.osdu.schema.provider.interfaces.schemastore.ISchemaStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchemaStore implements ISchemaStore {

    private final CloudObjectStorageFactory storageFactory;

    @Override
    public String createSchema(String filePath, String content) throws ApplicationException {
        MinioClient client = storageFactory.getClient();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        headers.put("X-Amz-Storage-Class", "REDUCED_REDUNDANCY");
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try {
            ObjectWriteResponse objectWriteResponse = client.putObject(
                PutObjectArgs.builder()
                    .bucket(MINIO_SCHEMA_BUCKET)
                    .object(filePath + SchemaConstants.JSON_EXTENSION)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .headers(headers)
                    .build());
            return objectWriteResponse.etag();
        } catch (InvalidKeyException e) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ErrorResponseException e) {
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, e.errorResponse().message());
        } catch (Exception e) {
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String getSchema(String dataPartitionId, String filePath) throws NotFoundException, ApplicationException {
        MinioClient client = storageFactory.getClient();
        try {
            InputStream stream =
                client.getObject(GetObjectArgs.builder().bucket(MINIO_SCHEMA_BUCKET).object(filePath + SchemaConstants.JSON_EXTENSION).build());
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        } catch (InvalidKeyException e) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ErrorResponseException e) {
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, e.errorResponse().message());
        } catch (Exception e) {
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean cleanSchemaProject(String schemaId) throws ApplicationException {
        MinioClient client = storageFactory.getClient();
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(MINIO_SCHEMA_BUCKET).object(schemaId).build());
            return true;
        } catch (InvalidKeyException e) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ErrorResponseException e) {
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, e.errorResponse().message());
        } catch (Exception e) {
            throw new ApplicationException(SchemaConstants.INTERNAL_SERVER_ERROR);
        }
    }
}
