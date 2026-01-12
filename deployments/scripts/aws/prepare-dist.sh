# Copyright © 2021 Amazon Web Services
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# This script prepares the dist directory for the integration tests.
# Must be run from the root of the repostiory

# THIS SCRIPT MUST BE RUN FROM THE ROOT FOLDER OF THE SCHEMA SERVICE 

set -e

OUTPUT_DIR="${OUTPUT_DIR:-dist}"

echo "--Copying Schema Boostrap Scripts to ${OUTPUT_DIR}--"

rm -rf "${OUTPUT_DIR}/deployments"

mkdir -p "${OUTPUT_DIR}/deployments"

rsync deployments/* "${OUTPUT_DIR}/deployments/"

cp -r deployments/shared-schemas/ "${OUTPUT_DIR}/deployments/shared-schemas/"

mkdir -p "${OUTPUT_DIR}/deployments/scripts/"
rsync deployments/scripts/* "${OUTPUT_DIR}/deployments/scripts/"
cp -r deployments/scripts/templates/ "${OUTPUT_DIR}/deployments/scripts/templates/"
cp -r deployments/scripts/aws/ "${OUTPUT_DIR}/deployments/scripts/aws/"