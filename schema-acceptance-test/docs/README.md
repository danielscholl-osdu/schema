### Running E2E Tests

You will need to have the following environment variables defined.

| name                                            | value                  | description                              | sensitive? | source                                                       |
|-------------------------------------------------|------------------------|------------------------------------------|------------|--------------------------------------------------------------|
| `HOST`                                          | eg. `https://osdu.com` | -                                        | no         | -                                                            |
| `PRIVATE_TENANT1`                               | eg. `osdu`             | OSDU tenant used for testing             | no         | -                                                            |
| `PRIVATE_TENANT2`                               | eg. `osdu`             | Alternative OSDU tenant used for testing | no         | -                                                            |
| `SHARED_TENANT`                                 | eg. `system`           | -                                        | no         | -                                                            |
| `VENDOR`                                        | eg. `anthos`           | -                                        | no         | -                                                            |
Authentication can be provided as OIDC config:

| name                                            | value                                   | description                   | sensitive? | source |
|-------------------------------------------------|-----------------------------------------|-------------------------------|------------|--------|
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_ID`     | `********`                              | ROOT_USER Client Id           | yes        | -      |
| `PRIVILEGED_USER_OPENID_PROVIDER_CLIENT_SECRET` | `********`                              | ROOT_USER Client secret       | yes        | -      |
| `TEST_OPENID_PROVIDER_URL`                      | `https://keycloak.com/auth/realms/osdu` | OpenID provider url           | yes        | -      |

Or tokens can be used directly from env variables:

| name                    | value      | description           | sensitive? | source |
|-------------------------|------------|-----------------------|------------|--------|
| `PRIVILEGED_USER_TOKEN` | `********` | PRIVILEGED_USER Token | yes        | -      |

#### Entitlements configuration for Integration Accounts

| INTEGRATION_TESTER |
|--------------------|
| users |
| service.schema-service.system-admin |
| service.entitlements.user |
| service.schema-service.viewers |
| service.schema-service.editors |
| data.integration.test |
| data.test1 |

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