curl -H 'Content-Type: application/json' -X DELETE -u $IBM_QA_DB_USER:$IBM_QA_DB_PASSWORD $IBM_QA_DB_URL/oc-cpd-dataecosystem-opendes-schema2
curl -H 'Content-Type: application/json' -X DELETE -u $IBM_QA_DB_USER:$IBM_QA_DB_PASSWORD $IBM_QA_DB_URL/oc-cpd-dataecosystem-common-schema2
export IBM_SCHEMA_URL=$IBM_SCHEMA_HOST/api/schema-service/v1/schemas/system
BEARER_TOKEN=`python $IBM_DEPLOYMENTS_SUBDIR/Token.py`
export BEARER_TOKEN=$BEARER_TOKEN
python $IBM_DEPLOYMENTS_SCRIPTS_SUBDIR/DeploySharedSchemas.py -u $IBM_SCHEMA_URL
