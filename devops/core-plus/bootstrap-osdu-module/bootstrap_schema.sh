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
# (with datastore cleanup)
# - SCHEMA_BUCKET
# - DATASTORE_NAMESPACE
# - DATASTORE_KIND
# - ENABLE_CLEANUP
#

set -e

source ./validate-env.sh "DATA_PARTITION"
source ./validate-env.sh "SCHEMA_URL"
source ./validate-env.sh "ENTITLEMENTS_HOST"
source ./validate-env.sh "ENABLE_CLEANUP"

bootstrap_schema_gettoken_onprem() {

  ID_TOKEN="$(curl --location --request POST "${OPENID_PROVIDER_URL}/protocol/openid-connect/token" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "scope=openid" \
  --data-urlencode "client_id=${OPENID_PROVIDER_CLIENT_ID}" \
  --data-urlencode "client_secret=${OPENID_PROVIDER_CLIENT_SECRET}" | jq -r ".id_token")"

  export BEARER_TOKEN="Bearer ${ID_TOKEN}"
}

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

if [ "${ONPREM_ENABLED}" == "true" ]
then
  source ./validate-env.sh "OPENID_PROVIDER_URL"
  source ./validate-env.sh "OPENID_PROVIDER_CLIENT_ID"
  source ./validate-env.sh "OPENID_PROVIDER_CLIENT_SECRET"

  # Get credentials for onprem
  bootstrap_schema_gettoken_onprem

else
  if [ "${ENABLE_CLEANUP}" == "true" ]
  then
    source ./validate-env.sh "SCHEMA_BUCKET"
    source ./validate-env.sh "DATASTORE_NAMESPACE"
    source ./validate-env.sh "DATASTORE_KIND"
    echo "Started schema cleanup"
    python3 ./scripts/schema-cleaner/main.py -u "${SCHEMA_URL}"/api/schema-service/v1/schemas/system
    echo "Finished schema cleanup"
  fi

  # Get credentials for Google Cloud
  bootstrap_schema_gettoken_gc

fi

# Precheck entitlements
bootstrap_schema_prechek_env

# Deploy shared schemas
bootstrap_schema_deploy_shared_schemas

touch /tmp/bootstrap_ready
