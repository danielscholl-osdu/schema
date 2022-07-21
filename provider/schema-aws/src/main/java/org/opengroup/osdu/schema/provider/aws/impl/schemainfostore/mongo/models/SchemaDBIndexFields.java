// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.schema.provider.aws.impl.schemainfostore.mongo.models;

public class SchemaDBIndexFields {
    //field names
    public static final String ID = "_id";
    public static final String AUTHORITY = "data.schemaIdentity.authority";
    public static final String ENTITY_TYPE = "data.schemaIdentity.entityType";
    public static final String SOURCE = "data.schemaIdentity.source";
    public static final String MAJOR_VERSION = "data.schemaIdentity.schemaVersionMajor";
    public static final String MINOR_VERSION = "data.schemaIdentity.schemaVersionMinor";
    public static final String PATCH_VERSION = "data.schemaIdentity.schemaVersionPatch";
    public static final String SCOPE = "data.scope";
    public static final String STATUS = "data.status";
}
