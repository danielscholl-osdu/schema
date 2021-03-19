# Schema Service

- [Introduction](#introduction)
- [Concepts](#concepts)
- [How to use this service?](#using_this_service)
- [Current limitation](#limitation)


## Introduction <a name="introduction"></a>
Schema Service enables a centralized governance and management of schema in the Data Ecosystem. It offers an implementation of the schema standard. 
Schema Service provides all necessary APIs to Fetch, create, update and mark a schema obsolete.


## Concepts <a name="concepts"></a>
* Schema - A data model definition. Schema Service allows data models to be defined in rich JSON objects (as per JSON schema specifications JSON Schema [draft #07](https://json-schema.org/understanding-json-schema/index.html)).
* Schema Resolution - Schema definitions could include references to other schema definitions or fragments. During Schema creation, Schema Service resolves all the referred schema fragments and creates a completely resolved schema. A sample entity-relationship diagram below demonstrates the associations between various domain entities and how schema fragment references can be helpful in defining models for these entities.
![](drilling_e_r.png)
* Schema Uniqueness - A schema is uniquely identified by the schema identity information. The identity information includes authority, source, entity type, major version, minor version and patch version. These attributes are used to generate the kind and id of the schema
* Schema Scope - Schema defined with Schema Service has a defined scope. Scope values could be SHARED or INTERNAL. PUBLIC scope may get introduced in future versions.
    * **INTERNAL** - Schema with INTERNAL scope are defined in a specific data partition and is available only to the users of that data partition.
    * **SHARED** - Concept of sharing of schema is intended to allow creator of schema, to share a schema with other friend data partition. In current version a user of Schema Service cannot explicitly share an INTERNAL schema. Certain standards schema are available out of box with all data partitions on the Data Ecosystem. So these are the ones that are shared by default.
    * PUBLIC - Not available currently. In future, could include community approved and governed schema that would, by default be available in the entire data platform.
* Schema State - Schema defined with Schema Service could be in one of the following state
    * **PUBLISHED** - When a schema is created it could be set to PUBLISHED state. Or a schema in DEVELOPMENT state can be moved to a PUBLISHED state. Schema in PUBLISHED state is immutable.
    * **DEVELOPMENT** - Schema in draft or evolutionary state. This state is usually set when schema definition is work in progress.Schema in DEVELOPMENT state is mutable.
    * **OBSOLETE** - Schema when no longer in use or not needed can be marked as OBSOLETE. Only Schema in DEVELOPMENT state can be moved to OBSOLETE state.
* Semantic Schema versioning - Schema definitions are expected to be semantically versioned. Major, Minor and Patch versions are part of Schema identity information. Schema service does check for breaking changes across the schema(s) being created. And if a breaking change is found, it prompts the user to change the major version of the schema.
* Schema Vs Storage Schema

| Schema defined with Schema Service                                   | Schema defined with Storage Schema Service             |
|----------------------------------------------------------------------|--------------------------------------------------------|
| Rich JSON Objects as per JSON Schema draft#07                        | Flattened JSON having an array of attributes           |
| Being JSON object allows defining real entity model                  | Schema definition does not support this                |
| Schema fragments can be referred within the definition               | Schema definition does not support this                |
| Intended for central governance for all schema defined in the system | Intended primarily for discovery and searching of data |



## How to use this service? <a name="using_this_service"></a>

The Schema Service promotes re-use and composition via the JSON schema draft 07 features. Typically one would start
with the registration of reusable schema fragments or schema elements, which are used e.g. by API specifications or
other schemas.

Then these schema fragments can be referenced and included in more complex constructs up to actual record schemas
for Storage.

The example below defines a schema fragment defining a map position having a latitude and a longitude.  

<details><summary>Schema Fragment Example, 2D Map position</summary>

```json
{
  "schemaInfo": {
    "schemaIdentity": {
      "authority": "slb",
      "source": "wks",
      "entityType": "mapPosition",
      "schemaVersionMajor": 1,
      "schemaVersionMinor": 0,
      "schemaVersionPatch": 0,
      "id": "slb:wks:mapPosition:1.0.0"
    },
    "createdBy": "Schlumberger",
    "scope": "SHARED",
    "status": "DEVELOPMENT"
  },
  "schema": {
    "description": "A 2D point location in latitude and longitude referenced to WGS 84 if not specified otherwise.",
    "properties": {
      "latitude": {
        "description": "The latitude value in degrees of arc (dega). Value range [-90, 90].",
        "title": "Latitude",
        "type": "number",
        "minimum": -90,
        "maximum": 90
      },
      "longitude": {
        "description": "The longitude value in degrees of arc (dega). Value range [-180, 180]",
        "title": "Longitude",
        "type": "number",
        "minimum": -180,
        "maximum": 180
      }
    },
    "required": [
      "latitude",
      "longitude"
    ],
    "title": "2D Map Location",
    "type": "object"
  }
}    
```

</details>

Eventually a Storage record schema will be created. It will use a variety of other standard 
schema fragments, which are boiler-plate record properties. An example record schema is given 
in the following example:

<details><summary>Storage Record Schema Example for a PointOfInterest Record</summary>

```json
{
  "schemaInfo": {
    "schemaIdentity": {
      "authority": "slb",
      "source": "wks",
      "entityType": "pointOfInterest",
      "schemaVersionMajor": 1,
      "schemaVersionMinor": 0,
      "schemaVersionPatch": 0,
      "id": "slb:wks:pointOfInterest:1.0.0"
    },
    "createdBy": "Schlumberger",
    "scope": "SHARED",
    "status": "DEVELOPMENT"
  },
  "schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "Point Of Interest",
    "description": "A Point of Interest data association a textual label with a description to a 2D map position.",
    "type": "object",
    "properties": {
      "id": {
        "description": "The entity ID which identifies this OSDU resource object without version.",
        "title": "Entity ID",
        "type": "string",
        "example": "someDataPartitionId:dataSourceNamespace:pointOfInterest:unique-id-3680fceb-bfef-4027-abba-6e580f6d212e"
      },
      "kind": {
        "description": "The schema identification for the DELFI/OSDU resource object following the pattern <Namespace>:<Source>:<Type>:<VersionMajor>.<VersionMiddle>.<VersionMinor>",
        "title": "Kind",
        "default": "slb:wks:pointOfInterest:1.0.0",
        "type": "string",
        "pattern": "[A-Za-z0-9-_]+:[A-Za-z0-9-_]+:[A-Za-z0-9-_]+:[0-9]+.[0-9]+.[0-9]+"
      },
      "version": {
        "description": "The version number of this OSDU resource; set by the framework.",
        "title": "Version Number",
        "type": "integer",
        "format": "int64",
        "example": "1831253916104085"
      },
      "acl": {
        "description": "The access control tags associated with this entity.",
        "title": "Access Control List",
        "$ref": "slb:wks:accessControlList:1.0.0"
      },
      "legal": {
        "description": "The entity's legal tags and compliance status.",
        "title": "Legal Tags",
        "$ref": "slb:wks:AbstractLegalTags:1.0.0"
      },
      "ancestry": {
        "description": "The links to data, which constitute the inputs.",
        "title": "Ancestry",
        "$ref": "slb:wks:AbstractLegalParentList:1.0.0"
      },
      "meta": {
        "description": "The meta data section linking the 'unitKey', 'crsKey' to self-contained definitions.",
        "title": "Frame of Reference Meta Data",
        "type": "array",
        "items": {
          "$ref": "slb:wks:metaItem:1.0.0"
        }
      },
      "data": {
        "$ref": "#/definitions/data"
      }
    },
    "required": [
      "kind",
      "acl",
      "legal"
    ],
    "definitions": {
      "relationships": {
        "description": "All relationships from this Point of Interest.",
        "properties": {
          "relatedItems": {
            "description": "Any entities related to this entity.",
            "$ref": "slb:wks:toManyRelationship:1.0.0",
            "title": "Related Entities"
          }
        },
        "title": "Relationships",
        "type": "object"
      },
      "data": {
        "type": "object",
        "title": "Point of Interest",
        "description": "",
        "properties": {
          "description": {
            "description": "The textual description for this point of interest.",
            "example": "Proposed Drilling Location",
            "title": "Description",
            "type": "string"
          },
          "additionalURL": {
            "description": "An optional URL leading to more contextual information.",
            "example": "https://www.bigoil.com/internal/campaigns/area/Drilling2021/plannedLocation.html",
            "title": "Additional URL",
            "type": "string"
          },
          "location": {
            "description": "The 2D map location of the Point of Interest",      
            "title": "Location",
            "$ref": "slb:wks:mapPosition:1.0.0"
          },
          "relationships": {
            "description": "The related entities.",
            "$ref": "#/definitions/relationships",
            "title": "Relationships"
          }
        }
      }
    }
  }
}
```

</details>

As the example demonstrates, the record schema is composed of a number of schema fragments, which are
included by `$ref` and the schema identity, e.g. `slb:wks:mapPosition:1.0.0` for the `location` 
property. All the referenced schema fragments must have been registered prior to the new schema
`slb:wks:pointOfInterest:1.0.0`.



The following parameters are required to create a schema: 


| Attribute          | Use                                               | Example                                  | Datatype | Required (Yes/No) |
|--------------------|---------------------------------------------------|------------------------------------------|----------|-------------------|
| authority          | Authority defining the schema                     | authority: "slb"                         | String   | Yes               |
| source             | Source from data of the given schema is expected. | source: "prosource"                      | String   | Yes               |
| entityType         | Entity Type for which the schema is being defined | entityType: "wellbore"                   | String   | Yes               |
| schemaVersionMajor | Major version of the schema                       | schemaVersionMajor: 1                    | String   | Yes               |
| schemaVersionMinor | Minor version of the schema                       | schemaVersionMinor : 0                   | String   | Yes               |
| schemaVersionPatch | Patch version of the schema                       | schemaVersionPatch : 0                   | String   | Yes               |
| schema             | Schema definition                                 | JSON object. Please refer example above. | Object   | No                |



