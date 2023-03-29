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
import datetime
import json

import psycopg2
from minio import Minio
from psycopg2.extensions import connection

from utils import RefSchemaConfig


class RefMigration:

  def __init__(self, schema_config: RefSchemaConfig) -> None:
    self.config = schema_config
    print("ENV vars configured.")

  def process_migration(self):
    start_time = datetime.datetime.now()
    print('Migration started.')
    client = self._get_minio_client()
    print('MinIO client built.')
    conn = self.get_pg_client()
    print('PG client built.')
    self._run_migration(conn, client, self.config.partition_id)
    end_time = datetime.datetime.now()
    print("Time taken: " + str(end_time - start_time))

  def get_pg_client(self):
    conn = psycopg2.connect(database=self.config.pg_db_name,
                            user=self.config.pg_user,
                            password=self.config.pg_pass,
                            host=self.config.pg_host,
                            port=self.config.pg_port)
    return conn

  def _get_minio_client(self):
    client = Minio(endpoint=self.config.minio_hots,
                   access_key=self.config.minio_access_key,
                   secret_key=self.config.minio_secret_key,
                   secure=self.config.minio_https)
    return client

  def _run_migration(self, pg_connection: connection, minio_client: Minio,
      partition: str):
    bucket_name = f"{self.config.project_id}-{partition}-schema"
    print("Diff source bucket: {0}".format(bucket_name))
    blobs = minio_client.list_objects(bucket_name=bucket_name,
                                      recursive=True)
    part_cur = pg_connection.cursor()

    # get list of file names in bucket, remove extension to get id
    file_names = [blob.object_name.replace(".json", "") for blob in blobs]
    errors = []
    copied_ids = []
    print("Loop through file names and use them to select from PostgreSQL.")
    origin_table = 'dataecosystem.%s' % self.config.pg_table
    target_table = partition + "." + self.config.pg_table
    for file_name in file_names:
      part_cur.execute(
        "SELECT id, data FROM {} WHERE id = %s".format(origin_table),
        (file_name,))
      rows = part_cur.fetchall()
      for row in rows:
        try:
          part_cur.execute(
            "INSERT INTO {} (id, data) VALUES (%s, %s) RETURNING id".format(
              target_table), (row[0], json.dumps(row[1])))
          inserted_id = part_cur.fetchone()[0]
          copied_ids.append(inserted_id)
          pg_connection.commit()
        except psycopg2.Error as err:
          errors.append(row[0] + " reason: " + str(err))
          pg_connection.rollback()

    print('Write the results to the output file.')
    with open("{0}-fails.txt".format(partition), 'w') as f:
      f.write("Ignored ids:\n")
      f.write("\n".join(errors))
    with open("{0}-success.txt".format(partition), 'w') as f:
      f.write("Saved ids:\n")
      f.write("\n".join(copied_ids))
    # close PostgreSQL connection
    pg_connection.close()
