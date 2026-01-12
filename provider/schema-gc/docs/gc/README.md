## Service Configuration for Google Cloud

## Environment variables

Define the following environment variables.

Must have:

| name                     | value     | description                                                                     | sensitive? | source |
|--------------------------|-----------|---------------------------------------------------------------------------------|------------|--------|
| `SHARED_TENANT_NAME`     | ex `osdu` | Shared account id                                                               | no         | -      |

Defined in default application property file but possible to override:

| name                                             | value                                         | description                                                             | sensitive? | source                                                       |
|--------------------------------------------------|-----------------------------------------------|-------------------------------------------------------------------------|------------|--------------------------------------------------------------|
| `LOG_PREFIX`                                     | `schema`                                      | Logging prefix                                                          | no         | -                                                            |
| `LOG_LEVEL`                                      | `DEBUG`                                       | Logging level                                                           | no         | -                                                            |
| `SERVER_SERVLET_CONTEXPATH`                      | `/api/schema-service/v1`                      | Servlet context path                                                    | no         | -                                                            |
| `AUTHORIZE_API`                                  | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint                                               | no         | output of infrastructure deployment                          |
| `PARTITION_API`                                  | ex `http://localhost:8081/api/partition/v1`   | Partition service endpoint                                              | no         | -                                                            |
| `GOOGLE_APPLICATION_CREDENTIALS`                 | ex `/path/to/directory/service-key.json`      | Service account credentials, you only need this if running locally      | yes        | <https://console.cloud.google.com/iam-admin/serviceaccounts> |
| `SCHEMA_CHANGED_MESSAGING_ENABLED`               | `true` OR `false`                             | Allows to configure message publishing about schemas changes to Pub/Sub | no         | -                                                            |
| `SCHEMA_CHANGED_TOPIC_NAME`                      | `schema-changed`                              | Topic for schema changes events                                         | no         | -                                                            |
| `PARTITION_PROPERTIES_SCHEMA_BUCKET_NAME`        | ex `schema.bucket.name`                       | name of partition property for schema bucket name value                 | yes        | -                                                            |
| `PARTITION_PROPERTIES_SYSTEM_SCHEMA_BUCKET_NAME` | ex `system.schema.bucket.name`                | name of partition property for system schema bucket name value          | yes        | -                                                            |
| `MANAGEMENT_ENDPOINTS_WEB_BASE`                  | ex `/`                                        | Web base for Actuator                                                   | no         | -                                                            |
| `MANAGEMENT_SERVER_PORT`                         | ex `8081`                                     | Port for Actuator                                                       | no         | -                                                            |
| `OTEL_JAVAAGENT_ENABLED`                         | ex `true` or `false`                          | `true` - OpenTelemetry Java agent enabled, `false` - disabled           | no         |                                                              |
| `OTEL_EXPORTER_OTLP_ENDPOINT`                    | ex `http://127.0.0.1:4318`                    | OpenTelemetry collector endpoint                                        | no         |                                                              |

## Partition level config

### Non-sensitive partition properties
| name                                        | value                          | description                          | sensitive? | source                                           |
|---------------------------------------------|--------------------------------|--------------------------------------|------------|--------------------------------------------------|
| `<SCHEMA_BUCKET_NAME_PROPERTY_NAME>`        | ex `schema.bucket.name`        | schema address in OBM storage        | no         | `PARTITION_PROPERTIES_SCHEMA_BUCKET_NAME`        |
| `<SYSTEM_SCHEMA_BUCKET_NAME_PROPERTY_NAME>` | ex `system.schema.bucket.name` | system schema address in OBM storage | no         | `PARTITION_PROPERTIES_SYSTEM_SCHEMA_BUCKET_NAME` |

## Testing

### Running E2E Tests

This section describes how to run cloud OSDU E2E tests (testing/schema-test-core).

You will need to have the following environment variables defined.

| name                 | value                                    | description                                                                                                                      | sensitive? | source                                                       |
|----------------------|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|------------|--------------------------------------------------------------|
| `VENDOR`             | `gc`                                     | Use value 'gc' to run Google Cloud tests                                                                                         | no         | -                                                            |
| `HOST`               | ex`http://localhost:8080`                | Schema service host                                                                                                              | no         | -                                                            |
| `INTEGRATION_TESTER` | `ewogICJ0....` or `tmp/service-acc.json` | Service account base64 encoded string or path to a file for API calls. Note: this user must have entitlements configured already | yes        | <https://console.cloud.google.com/iam-admin/serviceaccounts> |
| `PRIVATE_TENANT2`    | ex`opendes`                              | OSDU tenant used for testing                                                                                                     | no         | -                                                            |
| `PRIVATE_TENANT1`    | ex`osdu`                                 | OSDU tenant used for testing                                                                                                     | no         | -                                                            |
| `SHARED_TENANT`      | ex`common`                               | OSDU tenant used for testing                                                                                                     | no         | -                                                            |

**Entitlements configuration for integration accounts**

| INTEGRATION_TESTER                                                                                                                                                                         |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| users<br/>service.schema-service.system-admin<br/>service.entitlements.user<br/>service.schema-service.viewers<br/>service.schema-service.editors<br/>data.integration.test<br/>data.test1 |

Execute following command to build code and run all the integration tests:

 ```bash
 # Note: this assumes that the environment variables for integration tests as outlined
 #       above are already exported in your environment.
 # build + install integration test core
 $ (cd testing/schema-test-core/ && mvn clean test)
 ```

## Datastore configuration

### For private tenants:

There must be a namespace `<data-partition-id>`

Example:

![Screenshot](./pics/namespace_osdu.PNG)

Kind `schema_osm` `authority` `entityType` `source` will be created by service if it does not exist.

### For shared tenant:

There must be a namespace `dataecosystem`.

Example:

![Screenshot](./pics/namespace_dataecosystem.PNG)


Kind `system_schema_osm` `system_authority` `system_entityType` `system_source` will be created by service if it does not exist.

## Pub/Sub configuration

At Pub/Sub should be created topic with name:

**name:** `schema-changed`

It can be overridden by:

- through the Spring Boot property `gcp.schema-changed.topic-name`
- environment variable `GCP_SCHEMA_CHANGED_TOPIC_NAME`

Schema service responsible for publishing only.
Consumer side `schema-changed` topic configuration located in
[Indexer Google Cloud PubSub documentation](https://community.opengroup.org/osdu/platform/system/indexer-service/-/tree/master/provider/indexer-gc/docs/gc#pubsub-configuration)

## GCS configuration <a name="ObjectStoreConfig"></a>

For each private tenant:

At Google cloud storage should be created bucket:

<table>
  <tr>
   <td>Bucket Naming template
   </td>
   <td>Permissions required
   </td>
  </tr>
  <tr>
   <td>&lt;PartitionInfo.projectId-PartitionInfo.name><strong>-schema</strong>
   </td>
   <td>ListObjects, CRUDObject
   </td>
  </tr>
</table>

For shared tenant only:

At Google cloud storage should be created bucket:

<table>
  <tr>
   <td>Bucket Naming template
   </td>
   <td>Permissions required
   </td>
  </tr>
  <tr>
   <td>&lt;PartitionInfo.projectId-PartitionInfo.name><strong>-system-schema</strong>
   </td>
   <td>ListObjects, CRUDObject
   </td>
  </tr>
</table>

## Google cloud service account configuration

TBD

| Required roles |
|----------------|
| -              |

## Monitoring
### OpenTelemetry Integration

The opentelemetry-javaagent.jar file is the OpenTelemetry Java agent. It is used to
automatically instrument the Java application at runtime, without requiring manual changes
to the source code.

This provides critical observability features:
* Distributed Tracing: To trace the path of requests as they travel across different
  services.
* Metrics: To capture performance indicators and application-level metrics.
* Logs: To correlate logs with traces and other telemetry data.

Enabling this agent makes it significantly easier to monitor, debug, and manage the
application in development and production environments. The agent is activated by the
startup.sh script when the OTEL_JAVAAGENT_ENABLED environment variable is set to true.

The agent is available from the official OpenTelemetry GitHub repository. It is
recommended to use the latest stable version.

Official Download Page:
https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases

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

