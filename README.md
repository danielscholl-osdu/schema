## Running the schema service locally

The Schema Service is a Maven multi-module project with each cloud implemention placed in its submodule. To build or run Schema Service locally, follow the below steps :

### 1. GCP deployment

#### Prerequisite (Infra and access required) 

Schema service as per design uses two module from GCP. GCS or Google cloud storage to store actual schemas and Google cloud datastore to store schema metadata. It follows the multi tenancy 
concept of DE, which means service is deployed in one GCP project and data is stored in client specific project. And permission to speccfic tenant project is decided based on data-partition-id user passes
as part of request header. So, to make it work from local we must have following setup done as prerequisite,

1. GCP project setup is done and local gcloud sdk configured by activating the account/user and pointing to correct GCP project. You can follow the steps from [here](https://cloud.google.com/deployment-manager/docs/step-by-step-guide/installation-and-setup)

2. Bucket with name <project-id>-schema (e.g opendes-schema) is created in tenant GCS and tenant datafier service account has read/write access to that bucket. Steps to create bucket and grant access can be followed from [here](https://cloud.google.com/storage/docs/creating-buckets)

3. Tenant datafier service account has read/write access to Google cloud datastore in tenant project. You can follow access control on datastore from [here](https://cloud.google.com/datastore/docs/access/iam). Permission required is ```roles/datastore.user```

4. Service-account/user activated as part of step 1 has service token creator role on datafier service-account of the data partition used. Details on service account creator role can be accessed from [here](https://cloud.google.com/iam/docs/service-accounts#the_service_account_token_creator_role)

5. TenantInfo table should be present in service GCP datastore under namespace ```datascosystem``` and kind ```tenantInfo``` and has entry corresponding to data-partition-id passed. 

6. User/service-account that will be used to run the service has access to ```service.schema-service.editors``` group in the specified data-partition.

### Local deployment Steps

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
    ```

You can access the service APIs by following the service contract in [schema.yaml](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/os-schema?path=%2Fdocs%2Fapi%2Fschema.yaml) 

### 2. Azure deployment 

Instructions for running the Azure implementation in the cloud can be found [here](https://community.opengroup.org/osdu/platform/system/schema-service/-/blob/master/provider/schema-azure/README.md).

## Running Automated Integration Test
DevSanity tests are located in a schema-core project in testing directory under the project root directory.

1. GCP

These tests validate functionality of schema service. 

They can then be run/debugged directly in your IDE of choice using the GUI or via the commandline using below command from schema-core project.
Below command has to be run post building complete project.
    

    cd testing/schema-test-core
    mvn verify -DVENDOR=gcp -DHOST=https://open.opendes.cloud.slb-ds.com -DPRIVATE_TENANT1=opendes -DPRIVATE_TENANT2=tenant2 -DSHARED_TENANT=common
    
Below command can be run through azure-pipeline.yml after setting environment variables in the pipeline.

	verify
	
## Deploy Shared Schemas
Schema service as part of deployment deploys pre-defined OSDU schemas so end users can get community accepted schemas to refer. Such schemas are present in [folder](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/os-schema?path=%2Fdeployments%2Fshared-schemas%2Fosdu) and script to deploy the schema are present [here](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/os-schema?path=%2Fdeployments%2Fscripts). 

Details to deploy shared schemas can be found under [README.md](deployments/shared-schemas/README.md)
    


## License
Copyright 2017-2020, Schlumberger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at 

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License