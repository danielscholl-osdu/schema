#  Copyright 2020-2023 Google LLC
#  Copyright 2020-2023  EPAM Systems, Inc
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import dataclasses
import logging
import os

logger = logging.getLogger()

# common defaults
SCHEMA_DESTINATION = "schema_osm"
COMMON_NAME = "dataecosystem"

# Ref defaults
REF_PARTITION_ID = "osdu"
PG_DB_NAME = "schema"
PG_PORT = 5432
MINIO_HTTPS = True


@dataclasses.dataclass(frozen=True)
class GCSchemaConfig:
  common_namespace: str
  datastore_kind: str
  partition_api: str


@dataclasses.dataclass(frozen=True)
class RefSchemaConfig:
  partition_id: str
  project_id: str
  pg_host: str
  pg_user: str
  pg_pass: str
  pg_db_name: str
  pg_port: int
  pg_table: str
  pg_common_schema: str
  minio_hots: str
  minio_access_key: str
  minio_secret_key: str
  minio_https: bool


@dataclasses.dataclass(init=True)
class Report:
  skipped: list
  success: list
  processed_blob_num: int


def get_gc_schema_config_from_env() -> GCSchemaConfig:
  try:
    partition_api = os.environ["PARTITION_API"]
  except KeyError as err:
    logger.error(
      "You must specify the following env variables: 'PARTITION_API'")
    raise err
  datastore_kind = os.environ.get("SCHEMA_DESTINATION")
  datastore_namespace = os.environ.get("COMMON_NAME")
  if not datastore_kind:
    datastore_kind = SCHEMA_DESTINATION
    logger.warning(
      f"Default Datastore kind '{SCHEMA_DESTINATION}' is used.")

  if not datastore_namespace:
    datastore_namespace = COMMON_NAME
    logger.warning(
      f"Default Datastore namespace '{COMMON_NAME}' is used.")

  return GCSchemaConfig(
    common_namespace=datastore_namespace,
    datastore_kind=datastore_kind,
    partition_api=partition_api
  )


def get_ref_schema_config_from_env() -> RefSchemaConfig:
  try:
    project_id = os.environ["REF_PROJECT_ID"]
    pg_host = os.environ["PG_HOST"]
    pg_user = os.environ["PG_USER"]
    pg_pass = os.environ["PG_PASS"]
    minio_host = os.environ["MINIO_HOST"]
    minio_access_key = os.environ["MINIO_ACCESS_KEY"]
    minio_secret_key = os.environ["MINIO_SECRET_KEY"]
  except KeyError as err:
    logger.error(
      "You must specify the following env variables:'REF_PROJECT_ID','PG_HOST',"
      "'PG_USER','PG_PASS','MINIO_HOST','MINIO_ACCESS_KEY','MINIO_SECRET_KEY'")
    raise err
  partition_id = os.environ.get("REF_PARTITION_ID")
  minio_https = bool(os.environ.get("MINIO_HTTPS"))
  pg_port = os.environ.get("PG_PORT")
  pg_table = os.environ.get("SCHEMA_DESTINATION")
  pg_db_name = os.environ.get("PG_DB_NAME")
  pg_common_schema = os.environ.get("COMMON_NAME")

  if not partition_id:
    partition_id = REF_PARTITION_ID
    logger.warning(
      f"Partition id: '{MINIO_HTTPS}'")

  if not minio_https:
    minio_https = MINIO_HTTPS
    logger.warning(
      f"Minio https: '{MINIO_HTTPS}'")

  if not pg_port:
    pg_port = PG_PORT
    logger.warning(
      f"PG port: '{PG_PORT}'")

  if not pg_table:
    pg_table = SCHEMA_DESTINATION
    logger.warning(
      f"PG table '{SCHEMA_DESTINATION}'")

  if not pg_db_name:
    pg_db_name = PG_DB_NAME
    logger.warning(
      f"PG db name: '{PG_DB_NAME}'")

  if not pg_common_schema:
    pg_common_schema = COMMON_NAME
    logger.warning(
      f"PG schema: '{COMMON_NAME}' is used.")

  return RefSchemaConfig(
    partition_id=partition_id,
    project_id=project_id,
    pg_host=pg_host,
    pg_user=pg_user,
    pg_pass=pg_pass,
    pg_db_name=pg_db_name,
    pg_port=pg_port,
    pg_table=pg_table,
    pg_common_schema=pg_common_schema,
    minio_hots=minio_host,
    minio_access_key=minio_access_key,
    minio_secret_key=minio_secret_key,
    minio_https=minio_https
  )
