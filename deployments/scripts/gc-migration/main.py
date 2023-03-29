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
import argparse
import logging
from enum import StrEnum

from gc_migration import GCMigration
from ref_migration import RefMigration
from utils import get_gc_schema_config_from_env, get_ref_schema_config_from_env

logger = logging.getLogger()


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('-u', help='Migration mode, GC or REF.',
                      choices=[MigrationMode.GC, MigrationMode.REF])
  arguments = parser.parse_args()

  mig_mode = arguments.u

  match mig_mode:
    case MigrationMode.GC:
      print("GC migration configured.")
      gc_schema_conf = get_gc_schema_config_from_env()
      gc_migration = GCMigration(gc_schema_conf)
      gc_migration.process_gc_migration()
    case MigrationMode.REF:
      print("REF migration configured.")
      ref_schema_conf = get_ref_schema_config_from_env()
      ref_migration = RefMigration(ref_schema_conf)
      ref_migration.process_migration()
    case _:
      logger.error(
        "Only 'GC' or 'REF' can be used.")
      raise Exception("Arg misconfigured, system exit.")


class MigrationMode(StrEnum):
  GC = "GC"
  REF = "REF"


if __name__ == "__main__":
  main()
