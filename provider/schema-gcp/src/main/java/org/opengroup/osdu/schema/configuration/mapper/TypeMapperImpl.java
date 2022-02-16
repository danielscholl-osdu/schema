/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.schema.configuration.mapper;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Key;
import com.google.common.collect.ImmutableList;
import org.opengroup.osdu.core.gcp.osm.persistence.IdentityTranslator;
import org.opengroup.osdu.core.gcp.osm.translate.Instrumentation;
import org.opengroup.osdu.core.gcp.osm.translate.TypeMapper;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.model.Authority;
import org.opengroup.osdu.schema.model.EntityType;
import org.opengroup.osdu.schema.model.SchemaRequest;
import org.opengroup.osdu.schema.model.Source;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
@ConditionalOnProperty("osmDriver")
public class TypeMapperImpl extends TypeMapper {
    public TypeMapperImpl(){
        super(ImmutableList.of(
                new Instrumentation<>(
                        Authority.class,
                        new HashMap<String, String>(){{
                            put("authorityId", "name");
                        }},
                        new HashMap<>(),
                        new IdentityTranslator<>(
                                Authority::getAuthorityId,
                                (a, o) -> a.setAuthorityId(((Key) o).getName())
                        ),
                        Collections.singletonList("name")
                ),
                new Instrumentation<>(
                        EntityType.class,
                        new HashMap<String, String>(){{
                            put("entityTypeId", "name");
                        }},
                        new HashMap<>(),
                        new IdentityTranslator<>(
                                EntityType::getEntityTypeId,
                                (et, o) -> et.setEntityTypeId(((Key) o).getName())
                        ),
                        Collections.singletonList("name")
                ),
                new Instrumentation<>(
                        SchemaRequest.class,
                        new HashMap<String, String>(){{
                            put("schemaVersionMajor", SchemaConstants.MAJOR_VERSION);
                            put("schemaVersionMinor", SchemaConstants.MINOR_VERSION);
                            put("schemaVersionPatch", SchemaConstants.PATCH_VERSION);
                        }},
                        new HashMap<String, Class<?>>() {{
                            put("schema", Blob.class);
                            put("dateCreated", Timestamp.class);
                        }},
                        new IdentityTranslator<>(
                                (r) -> r.getSchemaInfo().getSchemaIdentity().getId(),
                                (r, o) -> r.getSchemaInfo().getSchemaIdentity().setId(((Key)o).getName())
                        ),
                        Collections.singletonList("")
                ),
                new Instrumentation<>(
                        Source.class,
                        new HashMap<String, String>() {{
                            put("sourceId", "name");
                        }},
                        new HashMap<>(),
                        new IdentityTranslator<>(
                                Source::getSourceId,
                                (s, o) -> s.setSourceId(((Key) o).getName())
                        ),
                        Collections.singletonList("name")
                )
        ));
    }
}
