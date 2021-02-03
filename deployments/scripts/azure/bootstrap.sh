export AZURE_SCHEMA_URL=https://$AZURE_DNS_NAME/api/schema-service/v1/schema
BEARER_TOKEN=`python $AZURE_DEPLOYMENTS_SUBDIR/Token.py`
export BEARER_TOKEN=$BEARER_TOKEN

python $AZURE_DEPLOYMENTS_SCRIPTS_SUBDIR/DeploySharedSchemas.py -l load_sequence.1.0.0.json -u $AZURE_SCHEMA_URL

