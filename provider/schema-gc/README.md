# Schema Service

os-schema-gc is a Maven multi-module project service.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites (Infra and access required)

Pre-requisites

* GCloud SDK with java (latest version)
* JDK 17
* Lombok 1.18.26 or later
* Maven

### Installation

### Service Configuration

#### Google Cloud

[Google Cloud service configuration](docs/gc/README.md)

### Run Locally

Check that maven is installed:

```bash
$ mvn --version
Apache Maven 3.8.7
Maven home: /usr/share/maven
Java 17.0.7
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

2. This will build the core project as well as all the underlying projects. If we want  to build projects for specific cloud vendor, we can use mvn --projects command. For example, if we want to build only for Google Cloud, we can use below command :

    ```bash
    mvn --projects schema-core,provider/schema-gc clean install
    ```

3. After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*


```bash
CMD java --add-opens java.base/java.lang=ALL-UNNAMED \
         --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
         -Dloader.main=org.opengroup.osdu.schema.GcpSchemaApplication \
         -jar /provider/schema-gc/target/os-schema-gc-${PROVIDER}-spring-boot.jar
```

You can access the service APIs by following the service contract in [schema.yaml](docs/api/schema.yaml)

## Testing

#### Google Cloud

[Google Cloud Testing](docs/gc/README.md)

## Deployment

Schema Service is compatible with Cloud Run.

* To deploy into Cloud run, please, use this documentation:
<https://cloud.google.com/run/docs/quickstarts/build-and-deploy>

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
