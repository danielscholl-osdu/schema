
This script is used to preserve schemas data integrity between M16 and M16 + N <br/>
To resolve multi partition issue: https://community.opengroup.org/osdu/platform/system/schema-service/-/merge_requests/444 <br/>
Schemas should be moved from common storage to tenants private storages. <br/>
Scripts using schema files from S3 storages to determine diffs and copy schemas metadata to tenant database. <br/>
Old schemas not deleted. <br/>

# Environmental variables

### Google Cloud:

| Name                           | Description                                                      | Type      | Default         | Required | Sensitive |
|--------------------------------|------------------------------------------------------------------|-----------|-----------------|----------|-----------|
| PARTITION_API                  | https://partition/api/partition/v1/partitions/                   | string    | -               | yes      | no        |
| GOOGLE_APPLICATION_CREDENTIALS | mnt/cred/sa.json, with access to all resources of all partitions | file path | -               | yes      | yes       |
| SCHEMA_DESTINATION             | kind name                                                        | string    | "schema_osm"    | no       | no        |
| COMMON_NAME                    | common namespace name                                            | string    | "dataecosystem" | no       | no        |


### Reference

| Name               | Description                                   | Type    | Default         | Required | Sensitive |
|--------------------|-----------------------------------------------|---------|-----------------|----------|-----------|
| REF_PROJECT_ID     | project id, should be the as in tenant config | string  | -               | yes      | no        |
| REF_PARTITION_ID   | partition id                                  | string  | "osdu"          | no       | no        |
| PG_HOST            | postgresql host ex: "127.0.0.1"               | string  | -               | yes      | yes       |
| PG_PASS            | postgresql password                           | string  | -               | yes      | yes       |
| PG_USER            | postgresql user                               | string  | -               | yes      | yes       |
| PG_DB_NAME         | database name                                 | string  | "schema"        | no       | no        |
| PG_PORT            | postgresql port                               | int     | 5432            | no       | no        |
| MINIO_HOST         | minio host ex: "127.0.0.1"                    | string  | -               | yes      | yes       |
| MINIO_SECRET_KEY   | minio secret key                              | string  | -               | yes      | yes       |
| MINIO_ACCESS_KEY   | minio access key                              | string  | -               | yes      | yes       |
| MINIO_HTTPS        | minio schema, https if "true"                 | boolean | "true"          | no       | no        |
| SCHEMA_DESTINATION | table name                                    | string  | "schema_osm"    | no       | no        |
| COMMON_NAME        | postgres common schema name                   | string  | "dataecosystem" | no       | no        |

### Run Args

To run reference OSDU platform migration (PostgreSQL and MinIO):
```
-u REF
```
To run Google Cloud platform migration (Datastore and Google Storage):
```
-u GC
```