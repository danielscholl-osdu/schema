# Schema Service
schema-gcp is a Maven multi-module project service.

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites (Infra and access required) 
Pre-requisites

* GCloud SDK with java (latest version)
* JDK 8
* Lombok 1.16 or later
* Maven

Schema service as per design uses two module from GCP. GCS or Google cloud storage to store actual schemas and Google cloud datastore to store schema metadata. It follows the multi tenancy 
concept of DE, which means service is deployed in one GCP project and data is stored in client specific project. And permission to speccfic tenant project is decided based on data-partition-id user passes
as part of request header. So, to make it work from local we must have following setup done as prerequisite,

1. GCP project setup is done and local gcloud sdk configured by activating the account/user and pointing to correct GCP project. You can follow the steps from [here](https://cloud.google.com/deployment-manager/docs/step-by-step-guide/installation-and-setup)

2. Bucket with name <project-id>-schema (e.g opendes-schema) is created in tenant GCS and tenant datafier service account has read/write access to that bucket. Steps to create bucket and grant access can be followed from [here](https://cloud.google.com/storage/docs/creating-buckets)

3. Tenant datafier service account has read/write access to Google cloud datastore in tenant project. You can follow access control on datastore from [here](https://cloud.google.com/datastore/docs/access/iam). Permission required is ```roles/datastore.user```

4. Service-account/user activated as part of step 1 has service token creator role on datafier service-account of the data partition used. Details on service account creator role can be accessed from [here](https://cloud.google.com/iam/docs/service-accounts#the_service_account_token_creator_role)

5. TenantInfo table should be present in service GCP datastore under namespace ```datascosystem``` and kind ```tenantInfo``` and has entry corresponding to data-partition-id passed. 

6. User/service-account that will be used to run the service has access to ```service.schema-service.editors``` group in the specified data-partition.

### Installation
In order to run the service locally or remotely, you will need to have the following environment variables defined.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `LOG_PREFIX` | `schema` | Logging prefix | no | - |
| `SERVER_SERVLET_CONTEXPATH` | `/api/schema-service/v1` | Servlet context path | no | - |
| `AUTHORIZE_API` | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint | no | output of infrastructure deployment |
| `ACCOUNT_ID_COMMON_PROJECT` | ex `common` | Shared account id | no | - |
| `SERVICE_PARTITION_ENABLED` | `true` OR `false` | Allow to configure TenantInfo provision by Partition service | no | - |
| `GOOGLE_AUDIENCES` | ex `*****.apps.googleusercontent.com` | Client ID for getting access to cloud resources | yes | https://console.cloud.google.com/apis/credentials |
| `PARTITION_API` | ex `http://localhost:8081/api/partition/v1` | Partition service endpoint | no | - |
| `GOOGLE_APPLICATION_CREDENTIALS` | ex `/path/to/directory/service-key.json` | Service account credentials, you only need this if running locally | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |
| `GCLOUD_PROJECT` | `******` | Cloud project id, you only need this if running locally | no | https://console.cloud.google.com |
| `gcp.schema-changed.messagingEnabled` | `true` OR `false` | Allows to configure message publishing about schemas changes to Pub/Sub | no | - |

### Run Locally
Check that maven is installed:

```bash
$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_212, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/jdk8u212-b04/jre
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. This file should live within `~/.mvn/community-maven.settings.xml`:

```bash
$ cat ~/.m2/settings.xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>community-maven-via-private-token</id>
            <!-- Treat this auth token like a password. Do not share it with anyone, including Microsoft support. -->
            <!-- The generated token expires on or before 11/14/2019 -->
             <configuration>
              <httpHeaders>
                  <property>
                      <name>Private-Token</name>
                      <value>${env.COMMUNITY_MAVEN_TOKEN}</value>
                  </property>
              </httpHeaders>
             </configuration>
        </server>
    </servers>
</settings>
```
* Update the Google cloud SDK to the latest version:

```bash
gcloud components update
```
* Set Google Project Id:

```bash
gcloud config set project <YOUR-PROJECT-ID>
```

* Perform a basic authentication in the selected project:

```bash
gcloud auth application-default login
```
Once the above Prerequisite are done, we can follow the below steps to run the service locally,

1. Navigate to the root of the schema project, os-schema. For building the project using command line, run below command :
    ```bash
    mvn clean install
    ```
    This will build the core project as well as all the underlying projects. If we want  to build projects for specific cloud vendor, we can use mvn --projects command. For example, if we want to build only for GCP(Google Cloud Platform), we can use below command :
    ```bash
    mvn --projects schema-core,provider/schema-gcp clean install
    ```
2. Run schema service in command line. We need to select which cloud vendor specific schema-service we want to run. For example, if we want to run schema-service for GCP, run the below command : 
    ```bash 
    # Running GCP : 
    java -jar  provider\schema-gcp\target\os-schema-gcp-0.0.1-spring-boot.jar
3. The port and path for the service endpoint can be configured in ```application.properties``` in the provider folder as following. If not specified, then  the web container (ex. Tomcat) default is used: 
    ```bash
    server.servlet.contextPath=/api/schema-service/v1/
    server.port=8080
   
You can access the service APIs by following the service contract in [schema.yaml](docs/api/schema.yaml) 

## Testing

 ### Running E2E Tests 
 This section describes how to run cloud OSDU E2E tests (testing/schema-test-core).
 
 You will need to have the following environment variables defined.
 
 | name | value | description | sensitive? | source |
 | ---  | ---   | ---         | ---        | ---    |
 | `INTEGRATION_TEST_AUDIENCE` | `*****.apps.googleusercontent.com` | client application ID | yes | https://console.cloud.google.com/apis/credentials |
 | `VENDOR` | `gcp` | Use value 'gcp' to run gcp tests | no | - |
 | `HOST` | ex`http://localhost:8080` | Schema service host | no | - |
 | `INTEGRATION_TESTER` | `********` | Service account base64 encoded string for API calls. Note: this user must have entitlements configured already | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |
 | `PRIVATE_TENANT2` | ex`opendes` | OSDU tenant used for testing | no | - |
 | `PRIVATE_TENANT1` | ex`osdu` | OSDU tenant used for testing | no | - |
 | `SHARED_TENANT` | ex`common` | OSDU tenant used for testing | no | - |
 
 **Entitlements configuration for integration accounts**
 
 | INTEGRATION_TESTER | 
 | ---  | 
 | users<br/>service.entitlements.user<br/>service.schema-service.viewers<br/>service.schema-service.editors<br/>data.integration.test<br/>data.test1 | 
 
 Execute following command to build code and run all the integration tests:
 
 ```bash
 # Note: this assumes that the environment variables for integration tests as outlined
 #       above are already exported in your environment.
 # build + install integration test core
 $ (cd testing/schema-test-core/ && mvn clean test)
 ```

## Deployment

Schema Service is compatible with Cloud Run.

* To deploy into Cloud run, please, use this documentation:
https://cloud.google.com/run/docs/quickstarts/build-and-deploy

## License

Copyright © Google LLC

Copyright © EPAM Systems
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.