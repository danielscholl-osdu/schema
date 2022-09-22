package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.config;

import org.opengroup.osdu.core.aws.mongodb.AbstractMultiClusteredConfigReader;
import org.opengroup.osdu.core.aws.ssm.SSMManagerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class MultiClusteredConfigReaderSchema extends AbstractMultiClusteredConfigReader {
    String serviceName = "schema";

    @Autowired
    public MultiClusteredConfigReaderSchema(SSMManagerUtil ssmManagerUtil) {
        super(ssmManagerUtil);
    }

    @Override
    protected String applyServiceName(String originalName) {
        return originalName.replace(serviceNamePlaceHolder, serviceName);
    }
}
