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
from itertools import islice
from typing import Iterator, Iterable
from urllib.parse import urljoin

import requests
from google.api_core.page_iterator import Page
from google.cloud import datastore
from google.cloud import storage
from google.cloud.datastore import Client as DatastoreClient, Key
from google.cloud.storage import Client as StorageClient

from utils import GCSchemaConfig
from utils import Report


class GCMigration:

  def __init__(self, schema_config: GCSchemaConfig) -> None:
    self.config = schema_config
    print("ENV vars configured.")

  def process_gc_migration(self):
    print("Processing started.")
    start_time = datetime.datetime.now()

    # get partitions
    partition_api = self.config.partition_api
    response = requests.get(partition_api)
    partitions = response.json()
    print("Received partitions: " + response.text)

    # get config for each partition
    for partition in partitions:
      project_id = self._get_project_id(partition, partition_api)
      datastore_client = datastore.Client(project=project_id)
      storage_client = storage.Client(project=project_id)
      # run migration for partition
      self._run_partition_migration(datastore_client, storage_client, partition,
                                    project_id)
    end_time = datetime.datetime.now()
    print("Time taken: " + str(end_time - start_time))

  def _get_project_id(self, partition: str, partition_api: str):
    url = urljoin(partition_api, partition)
    partition_resp = requests.get(url)
    data = partition_resp.json()
    project_id = data["projectId"]["value"]
    return project_id

  def _run_partition_migration(self, datastore_client: DatastoreClient,
                               storage_client: StorageClient, partition: str,
                               project_id: str):
    bucket_name = f"{project_id}-{partition}-schema"
    print("Tenant schema source bucket: {0}".format(bucket_name))
    # get a partial response of blobs meta, name and page token for iteration
    blobs = storage_client.list_blobs(bucket_name,
                                      fields='items(name),nextPageToken',
                                      page_size=5000, max_results=100)
    print(
      "Looping through file names to determine diff and move schemas.")
    partition_report = Report([], [], 0)
    for page in blobs.pages:
      report = self._process_page(datastore_client, page, partition)
      partition_report.skipped.extend(report.skipped)
      partition_report.success.extend(report.success)
      partition_report.processed_blob_num += report.processed_blob_num

    print(
      'Blobs in tenant bucket:' + str(partition_report.processed_blob_num))

    print('Write the tenant results to the output file.')
    with open("{0}-fails.txt".format(partition), 'w') as f:
      f.write("Ignored ids:\n")
      f.write("\n".join(partition_report.skipped))
    with open("{0}-success.txt".format(partition), 'w') as f:
      f.write("Saved ids:\n")
      f.write("\n".join(partition_report.success))

  def _process_page(self, datastore_client: DatastoreClient, blobs_page: Page,
                    partition: str):
    entity_keys = []
    skipped = []
    success = []
    blob_num = 0
    datastore_kind = self.config.datastore_kind
    datastore_common_namespace = self.config.common_namespace
    for blob_num, blob in enumerate(blobs_page, start=1):
      filename = blob.name.replace(".json", "")

      entity_key = datastore_client.key(datastore_kind, filename,
                                        namespace=datastore_common_namespace)
      entity_keys.append(entity_key)

    self._process_datastore_batches(datastore_client, entity_keys, partition,
                                    skipped, success)
    print("{0} blobs processed.".format(blob_num))
    return Report(skipped, success, blob_num)

  def _process_datastore_batches(self, datastore_client: DatastoreClient,
                                 entity_keys: list, partition: str,
                                 skipped: list, success: list):
    # split keys by chunks, 400 is limit for datastore client
    for chunk in self._chunked(entity_keys, 400):
      # get schemas in dataecosystem namespace
      batch_entities = datastore_client.get_multi(chunk)
      batch_entities_dict = {entity.key.name: entity for entity in
                             batch_entities}

      diff_keys = self._update_keys(batch_entities, datastore_client, partition)

      existing_entities_dict = self._get_dict_with_existing_schemas(
        datastore_client,
        diff_keys)
      entities_to_copy = self._get_entities_to_copy(batch_entities_dict,
                                                    existing_entities_dict,
                                                    skipped)
      # put entities in batch
      if entities_to_copy:
        try:
          datastore_client.put_multi(entities_to_copy)
          for entity in entities_to_copy:
            success.append("Saved entity:" + str(entity.key))
        except Exception as err:
          skipped.append("Failed: " + str(err))
        print("{0} entities transferred.".format(len(entities_to_copy)))

  # collect entities that not present in tenant namespace
  def _get_entities_to_copy(self, batch_entities_dict: dict,
                            existing_entities_dict: dict, skipped: list):

    entities_to_copy = []
    for key, value in batch_entities_dict.items():
      if key not in existing_entities_dict:
        entities_to_copy.append(value)
      else:
        skipped.append("Skipped, already present in tenant: " + key)
    return entities_to_copy

  # modify keys to tenant namespace
  def _update_keys(self, batch_entities: list,
                   datastore_client: DatastoreClient, partition: str):
    diff_keys = set()
    datastore_kind = self.config.datastore_kind
    for entity in batch_entities:
      dst_key = datastore_client.key(datastore_kind + "_test_mig",
                                     entity.key.name,
                                     namespace=partition)
      diff_keys.add(dst_key)
      entity.key = dst_key
    return diff_keys

  # check if schemas already present in tenant namespace
  def _get_dict_with_existing_schemas(self, datastore_client: DatastoreClient,
                                      diff_keys: set):
    existing_entities = datastore_client.get_multi(diff_keys)
    existing_entities_dict = {entity.key.name: entity for entity in
                              existing_entities}
    return existing_entities_dict

  def _chunked(self, sequence: Iterable[Key], size: int) -> Iterator[list[Key]]:
    sequence = iter(sequence)
    while chunk := list(islice(sequence, size)):
      yield chunk