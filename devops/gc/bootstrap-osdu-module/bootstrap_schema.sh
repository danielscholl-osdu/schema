#!/usr/bin/env bash
#
# Script that bootstraps schema service using Python scripts, that make requests to schema service
# Contains logic for both Reference and Google Cloud version
#
# Expected environment variables:
# (both environments):
# - DATA_PARTITION
# - SCHEMA_URL
# - ENTITLEMENTS_HOST
# (for Google Cloud):
# - AUDIENCES
# (for Reference):
# - OPENID_PROVIDER_URL
# - OPENID_PROVIDER_CLIENT_ID
# - OPENID_PROVIDER_CLIENT_SECRET

set -e

source ./validate-env.sh "SCHEMA_URL"
source ./validate-env.sh "ENTITLEMENTS_HOST"

bootstrap_schema_gettoken_gc() {

  BEARER_TOKEN=$(gcloud auth print-identity-token)

  export BEARER_TOKEN
}

bootstrap_schema_prechek_env() {

  status_code=$(curl --retry 1 --location -globoff --request GET "${ENTITLEMENTS_HOST}/api/entitlements/v2/groups" \
  --write-out "%{http_code}" --silent --output "/dev/null" \
  --header 'Content-Type: application/json' \
  --header "data-partition-id: ${DATA_PARTITION}" \
  --header "Authorization: ${BEARER_TOKEN}")

  if [ "$status_code" == 200 ]
  then
    echo "$status_code: Entitlements provisioning completed successfully!"
  else
    echo "$status_code: Entitlements provisioning is in progress or failed!"
    exit 1
  fi
}

bootstrap_schema_deploy_shared_schemas() {
  python3 ./scripts/DeploySharedSchemas.py -e -u "${SCHEMA_URL}"/api/schema-service/v1/schemas/system
}

# Specifying "system" partition for GC installation 
export DATA_PARTITION="system"

# Get credentials for Google Cloud
bootstrap_schema_gettoken_gc

# Precheck entitlements
bootstrap_schema_prechek_env

# Deploy shared schemas
bootstrap_schema_deploy_shared_schemas

touch /tmp/bootstrap_ready
