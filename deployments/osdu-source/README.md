# OSDU Raw Schemas

This sub-folder contains the OSDU raw schemas as they are provided. Each version has its 
separate folder. Each version contains a `schema-version-info.json` file providing the
meta data to be used for the schema registration.

Example:
```json
{
  "authority": "osdu",
  "source": "osdu",
  "majorVersion": 0,
  "minorVersion": 2,
  "patchVersion": 0,
  "createdBy": "OSDU Data Definition Group",
  "status": "DEVELOPMENT",
  "scope": "SHARED",
  "exclude": ["WorkProductLoadManifestStagedFiles", "WoodchuckWorkProductComponent", "WoodchuckWorkProduct"]
}
```
It is assumed that a folder contains one consistent version. This assumption is used by 
the [ImportFromOSDU.py](../scripts/ImportFromOSDU.py) script.

Files, which may look like schema definitions but aren't, can be excluded but the `"exclude"` 
list. This applies also to schema definitions, which refer to non-existing schema 
fragments.  

* OSDU R2 [source as provided via GitLab](https://gitlab.opengroup.org/osdu/json-schemas/-/tree/master)
  * The contents is regarded as 'development'
  * [schema-version-info.json](./R2-json-schema/schema-version-info.json)
* OSDU R3
  * not published yet
  * [schema-version-info.json](./R3-json-schema/schema-version-info.json)
  
