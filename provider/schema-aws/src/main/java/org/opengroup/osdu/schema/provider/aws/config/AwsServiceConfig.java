// Copyright © 2020 Amazon Web Services
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

package org.opengroup.osdu.schema.provider.aws.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.opengroup.osdu.core.aws.ssm.ParameterStorePropertySource;
import org.opengroup.osdu.core.aws.ssm.SSMConfig;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Component
public class AwsServiceConfig {

  @Value("${aws.region}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String amazonRegion;

  @Value("${aws.resource.prefix}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String environment;

  @Value("${aws.dynamodb.table.prefix}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String dynamoDbTablePrefix;

  @Value("${aws.dynamodb.endpoint}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String dynamoDbEndpoint;

  @Value("${aws.s3.endpoint}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String s3Endpoint;

  @Value("${aws.s3.bucket}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public String s3DataBucket;

  @Value("${aws.ssm}")
  @Getter()
  @Setter(AccessLevel.PROTECTED)
  public Boolean ssmEnabled;

  /*@Inject
  protected JaxRsDpsLog logger;*/

  @PostConstruct
  public void init() {
    if (ssmEnabled) {
      SSMConfig ssmConfig = new SSMConfig();
      ParameterStorePropertySource ssm = ssmConfig.amazonSSM();
      // String s3DataBucketParameter = "/osdu/" + environment + "/global-s3-bucket";
      String s3DataBucketParameter = "/osdu/" + environment + "/schema/schema-s3-bucket-name";
      try {
        s3DataBucket = ssm.getProperty(s3DataBucketParameter).toString();
      } catch (Exception e) {
        //logger.error(String.format("SSM property %s not found", s3DataBucketParameter));
        System.out.println(String.format("SSM property %s not found", s3DataBucketParameter));
      }
    }
  }

}

