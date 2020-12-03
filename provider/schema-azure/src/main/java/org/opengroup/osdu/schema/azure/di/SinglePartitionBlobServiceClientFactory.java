package org.opengroup.osdu.schema.azure.di;

import com.azure.storage.blob.BlobServiceClient;
import org.opengroup.osdu.azure.blobstorage.IBlobServiceClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SinglePartitionBlobServiceClientFactory implements IBlobServiceClientFactory {
    @Autowired
    BlobServiceClient blobServiceClient;

    @Override
    public BlobServiceClient getBlobServiceClient(String s) {
        return blobServiceClient;
    }
}
