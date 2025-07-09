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

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springdoc.core.customizers.OperationCustomizer;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerDocumentationConfig {
        @Autowired
        private SwaggerConfigurationProperties configurationProperties;

        @Bean
        public OpenAPI customOpenAPI() {

                SecurityScheme securityScheme = new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Authorization")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization");
                final String securitySchemeName = "Authorization";
                SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);
                Components components = new Components().addSecuritySchemes(securitySchemeName, securityScheme);

                OpenAPI openAPI = new OpenAPI()
                                .addSecurityItem(securityRequirement)
                                .components(components)
                                .info(apiInfo())
                                .tags(tags());

                if (configurationProperties.isApiServerFullUrlEnabled())
                        return openAPI;
                return openAPI
                        .servers(Arrays.asList(new Server().url(configurationProperties.getApiServerUrl())));
        }
        
        @Bean
        public OperationCustomizer customize() {
            return (operation, handlerMethod) -> {
                if (operation.getTags().contains("schema-api"))
                {
                        Parameter dataPartitionId = new Parameter()
                                .name(DpsHeaders.DATA_PARTITION_ID)
                                .description("Tenant Id")
                                .in("header")
                                .required(true)
                                .schema(new StringSchema());
                        return operation.addParametersItem(dataPartitionId);
                }
                
                return operation;
            };
        }


        private List<Tag> tags() {
                List<Tag> tags = new ArrayList<>();
                tags.add(new Tag().name("schema-api").description("Schema API - Core Schema related endpoints"));
                tags.add(new Tag().name("system-schema-api").description("System Schema API - System Schema related endpoints"));
                tags.add(new Tag().name("info").description("Version info endpoint"));
                return tags;
        }

        private Info apiInfo() {
                return new Info()
                        .title(configurationProperties.getApiTitle())
                        .description(configurationProperties.getApiDescription())
                        .version(configurationProperties.getApiVersion())
                        .license(new License().name(configurationProperties.getApiLicenseName()).url(configurationProperties.getApiLicenseUrl()))
                        .contact(new Contact().name(configurationProperties.getApiContactName()).email(configurationProperties.getApiContactEmail()));
        }
}
