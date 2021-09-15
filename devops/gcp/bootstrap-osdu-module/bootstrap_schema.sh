#!/usr/bin/env bash

set -ex

export BEARER_TOKEN=`gcloud auth print-identity-token --audiences=${AUDIENCES}`

echo "Clean-up for Datastore schemas"
python3 /opt/scripts/GcpDatastoreCleanUp.py

sleep 5

echo "Bootstrap Schema Service"
python3 /opt/scripts/DeploySharedSchemas.py -u ${SCHEMA_URL}/api/schema-service/v1/schema
