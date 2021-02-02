echo $AWS_BASE_URL
export AWS_SCHEMA_SERVICE_URL=$AWS_BASE_URL/api/schema-service/v1/schema
BEARER_TOKEN=`python $AWS_DEPLOYMENTS_SUBDIR/Token.py`
echo $BEARER_TOKEN
export BEARER_TOKEN=$BEARER_TOKEN
export APP_KEY=""
export DATA_PARTITION=common
python deployments/scripts/DeploySharedSchemas.py -l load_sequence.1.0.0.json -u $AWS_SCHEMA_SERVICE_URL