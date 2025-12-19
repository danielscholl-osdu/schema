### Running E2E Tests

You will need to have the following environment variables defined.

| name             | value                                          | description                              | sensitive? | source | required |
|------------------|------------------------------------------------|------------------------------------------|------------|--------|----------|
| `HOST`           | ex `http://localhost:8080/api/schema-service/v1/` | Schema service URL                    | no         | -      | yes      |
| `PRIVATE_TENANT1`| ex `opendes`                                   | OSDU tenant used for testing             | no         | -      | yes      |
| `PRIVATE_TENANT2`| ex `opendes`                                   | Alternative OSDU tenant for testing      | no         | -      | no       |
| `SHARED_TENANT`  | ex `system`                                    | Shared tenant name                       | no         | -      | no       |
| `VENDOR`         | ex `azure`                                     | Cloud vendor identifier                  | no         | -      | no       |

Authentication can be provided as OIDC config:

| name                                            | value                                      | description                                 | sensitive? | source |
|-------------------------------------------------|--------------------------------------------|---------------------------------------------|------------|--------|
| `ROOT_USER_OPENID_PROVIDER_CLIENT_ID`           | `********`                                 | Root User Client Id                         | yes        | -      |
| `ROOT_USER_OPENID_PROVIDER_CLIENT_SECRET`       | `********`                                 | Root User Client secret                     | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | ex `https://keycloak.com/auth/realms/osdu` | OpenID provider url                         | yes        | -      |
| `ROOT_USER_OPENID_PROVIDER_SCOPE`               | ex `api://my-app/.default`                 | OAuth2 scope (optional, defaults to openid) | no         | -      |

Or tokens can be used directly from env variables:

| name              | value      | description     | sensitive? | source |
|-------------------|------------|-----------------|------------|--------|
| `ROOT_USER_TOKEN` | `********` | Root User Token | yes        | -      |

Authentication configuration is optional and could be omitted if not needed.

**Entitlements configuration for integration accounts**

| ROOT_USER                           |
|-------------------------------------|
| users                               |
| service.schema-service.system-admin |
| service.entitlements.user           |
| service.schema-service.viewers      |
| service.schema-service.editors      |
| data.integration.test               |
| data.test1                          |

Execute following command to build code and run all the integration tests:

 ```bash
 # Note: this assumes that the environment variables for integration tests as outlined
 #       above are already exported in your environment.
 # build + install integration test core
 $ (cd schema-acceptance-test && mvn clean verify)
 ```

## License

Copyright © Google LLC

Copyright © EPAM Systems

Copyright © ExxonMobil

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
