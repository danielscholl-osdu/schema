#Required Env Variables

#AWS_BASE_URL
#AWS_DEPLOYMENTS_SUBDIR
#AWS_REGION
#RESOURCE_PREFIX

if [ -z "$DEPLOYMENTS_BASE_DIR" ];
then export DEPLOYMENTS_BASE_DIR=deployments;
fi

pip3 install -r $AWS_DEPLOYMENTS_SUBDIR/requirements.txt

echo $AWS_BASE_URL
export AWS_SCHEMA_SERVICE_URL=$AWS_BASE_URL/api/schema-service/v1/schemas/system

if [ -z "$BEARER_TOKEN" ];
then BEARER_TOKEN=`python3 $AWS_DEPLOYMENTS_SUBDIR/Token.py`;
export BEARER_TOKEN=$BEARER_TOKEN
fi
export APP_KEY=""
export DATA_PARTITION=common

python3 $DEPLOYMENTS_BASE_DIR/scripts/DeploySharedSchemas.py -u $AWS_SCHEMA_SERVICE_URL
