#!/usr/bin/env bash

set -e

source ./validate-env.sh "DATA_PARTITION"
source ./validate-env.sh "SCHEMA_URL"

# FIXME find a better solution about a sidecar container readiness
echo "Waiting for a sidecar container is provisioned"
sleep 10

bootstrap_schema_onprem() {

  export BEARER_TOKEN="$(curl --location --request POST "${OPENID_PROVIDER_URL}/protocol/openid-connect/token" \
  --header "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=client_credentials" \
  --data-urlencode "scope=openid" \
  --data-urlencode "client_id=${OPENID_PROVIDER_CLIENT_ID}" \
  --data-urlencode "client_secret=${OPENID_PROVIDER_CLIENT_SECRET}" | jq -r ".id_token")"

  echo "Bootstrap Schema Service"
  python3 ./scripts/DeploySharedSchemas.py -u ${SCHEMA_URL}/api/schema-service/v1/schemas/system

}

bootstrap_schema_gcp() {

  export BEARER_TOKEN=`gcloud auth print-identity-token --audiences=${AUDIENCES}`

  echo "Clean-up for Datastore schemas"
  python3 ./scripts/GcpDatastoreCleanUp.py
  
  # FIXME find a better solution about datastore cleaning completion
  sleep 5

  echo "Bootstrap Schema Service"
  python3 ./scripts/DeploySharedSchemas.py -u ${SCHEMA_URL}/api/schema-service/v1/schemas/system

}

if [ "${ONPREM_ENABLED}" == "true" ]
then
  source ./validate-env.sh "OPENID_PROVIDER_URL"
  source ./validate-env.sh "OPENID_PROVIDER_CLIENT_ID"
  source ./validate-env.sh "OPENID_PROVIDER_CLIENT_SECRET"
  bootstrap_schema_onprem
else
  source ./validate-env.sh "AUDIENCES"
  bootstrap_schema_gcp
fi
