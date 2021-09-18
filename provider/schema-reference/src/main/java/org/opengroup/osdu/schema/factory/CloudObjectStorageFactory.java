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

package org.opengroup.osdu.schema.factory;

import io.minio.MinioClient;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.schema.config.MinIoConfigProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudObjectStorageFactory {

    private final MinIoConfigProperties minIoConfigProperties;
    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
            .endpoint(minIoConfigProperties.getMinIoEndpointUrl())
            .credentials(minIoConfigProperties.getMinIoAccessKey(), minIoConfigProperties.getMinIoSecretKey())
            .build();
        log.info("Minio client initialized");
    }

    public MinioClient getClient() {
        return this.minioClient;
    }

}
