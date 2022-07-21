package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.config;

import org.opengroup.osdu.core.aws.mongodb.MultiClusteredConfigReader;
import org.opengroup.osdu.core.aws.mongodb.config.MongoProperties;
import org.opengroup.osdu.core.aws.partition.PartitionInfoAws;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class MultiClusteredConfigReaderSchema implements MultiClusteredConfigReader {

    private final MongoPropertiesReader propertiesReader;

    @Autowired
    public MultiClusteredConfigReaderSchema(MongoPropertiesReader propertiesReader) {
        this.propertiesReader = propertiesReader;
    }

    @Override
    public MongoProperties readProperties(PartitionInfoAws partitionInfoAws) {
        return propertiesReader.getProperties();
    }
}
