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

package org.opengroup.osdu.schema.azure.utils;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Repository
public class OSDUAzureBlobStorageImpl {
    @Autowired
    private DpsHeaders headers;

    @Autowired
    private BlobContainerClient blobContainerClient;

    @Autowired
    JaxRsDpsLog log;

    public String readFromBlob(String filePath) throws ApplicationException {
        String content = "";
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        try (ByteArrayOutputStream downloadStream = new ByteArrayOutputStream()) {
            blockBlobClient.download(downloadStream);
            content = downloadStream.toString(StandardCharsets.UTF_8.name());
            return content;
        } catch (Exception ex) {
            log.warning(ex.getMessage());
            throw new ApplicationException(ex.getMessage());
        }
    }

    public boolean deleteFromBlob(String filePath) throws ApplicationException {
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        try
        {
            blockBlobClient.delete();
            return true;
        }
        catch (Exception ex)
        {
            log.warning(ex.getMessage());
            throw new ApplicationException("Error deleting file.");
        }
    }

    public String writeToBlob(String filePath, String content) throws ApplicationException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        int bytesSize = bytes.length;
        
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(filePath).getBlockBlobClient();
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(bytes)) {
            blockBlobClient.upload(dataStream, bytesSize, true);
        } catch (Exception ex) {
            log.warning(ex.getMessage());
            throw new ApplicationException("Write to blob failed");
        }

        // Get the blobname
        return blockBlobClient.getBlobName();
    }
}
