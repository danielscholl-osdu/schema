/*Copyright 2017-2019, Schlumberger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package org.opengroup.osdu.schema.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "${api.title}",
                description = "${api.description}",
                version = "${api.version}",
                contact = @Contact(name = "${api.contact.name}", email = "${api.contact.email}"),
                license = @License(name = "${api.license.name}", url = "${api.license.url}")),
        servers = @Server(url = "${api.server.url}"),
        security = @SecurityRequirement(name = "Authorization"),
        tags = {
                @Tag(name = "schema-api", description = "Schema API - Core Schema related endpoints"),
                @Tag(name = "system-schema-api", description = "System Schema API - System Schema related endpoints"),
                @Tag(name = "info", description = "Version info endpoint")
        }
)
@SecurityScheme(name = "Authorization", scheme = "bearer", bearerFormat = "Authorization", type = SecuritySchemeType.HTTP)
@Configuration
public class SwaggerDocumentationConfig {

}
