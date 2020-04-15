## Running the schema service locally

The Schema Service is a Maven multi-module project with each cloud implemention placed in its submodule. To build or run Schema Service locally, follow the below steps :

1. Clone the os-schema repository from git . Below is the URL : 

    [https://dev.azure.com/slb-swt/data-management/_git/os-schema/](https://dev.azure.com/slb-swt/data-management/_git/os-schema/)

2. Navigate to the root of the schema project, os-schema. For building the project using command line, run below command :
    ```bash
    mvn clean install
    ```
    This will build the core project as well as all the underlying projects. If we want  to build projects for specific cloud vendor, we can use mvn --projects command. For example, if we want to build only for GCP(Google Cloud Platform), we can use below command :
    ```bash
    mvn --projects schema-core,provider/schema-gcp clean install
    ```
3. Run schema service in command line. We need to select which cloud vendor specific schema-service we want to run. For example, if we want to run schema-service for GCP, run the below command : 
    ```bash 
    # Running GCP : 
    java -jar -Dspring.profiles.active=local provider\schema-gcp\target\os-schema-gcp-0.0.1-spring-boot.jar
4. The port and path for the service endpoint can be configured in ```application.properties``` in the provider folder as following. If not specified, then  the web container (ex. Tomcat) default is used: 
    ```bash
    server.servlet.contextPath=/api/schema-service/v1/
    server.port=8080
    ```

## Running Automated (DevSanity) Tests
DevSanity tests are located in a schema-core project in testing directory under the project root directory.

1. GCP

These tests validate functionality of schema service. 

They can then be run/debugged directly in your IDE of choice using the GUI or via the commandline using below command from GCP project.
Below command has to be run post building complete project.
    

    cd provider\schema-gcp
    mvn verify -P DevSanity
    


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