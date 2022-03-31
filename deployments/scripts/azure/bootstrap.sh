# Cleanup function
cleanup() {
  echo "Terminating istio sidecar"
  curl -X POST "http://localhost:15020/quitquitquit"
  exit
}

trap cleanup EXIT

if [[ -z "${NAMESPACE}" ]]; then
  NAMESPACE="osdu-azure"
fi

export AZURE_SCHEMA_URL="http://schema.${NAMESPACE}.svc.cluster.local/api/schema-service/v1/schema/"

currentStatus="success"
currentMessage="All schemas uploaded successfully"
BEARER_TOKEN=`python $AZURE_DEPLOYMENTS_SUBDIR/Token.py`
export BEARER_TOKEN=$BEARER_TOKEN
python $AZURE_DEPLOYMENTS_SCRIPTS_SUBDIR/DeploySharedSchemas.py -u $AZURE_SCHEMA_URL
if [ $ret -ne 0 ]; then
	currentStatus="failure"
	currentMessage="Schema loading failed. Please check error logs for more details."
fi
if [ ! -z "$CONFIG_MAP_NAME" -a "$CONFIG_MAP_NAME" != " " ]; then
  az login --identity --username $OSDU_IDENTITY_ID
  ENV_AKS=$(az aks list --resource-group $RESOURCE_GROUP_NAME --query [].name -otsv)
  az aks get-credentials --resource-group $RESOURCE_GROUP_NAME --name $ENV_AKS
  kubectl config set-context $RESOURCE_GROUP_NAME --cluster $ENV_AKS

  Status=$(kubectl get configmap $CONFIG_MAP_NAME -o jsonpath='{.data.status}')
  Message=$(kubectl get configmap $CONFIG_MAP_NAME -o jsonpath='{.data.message}')

  Message="${Message}Schema load Message: ${currentMessage}. "

  ## Update ConfigMap
  kubectl create configmap $CONFIG_MAP_NAME \
	--from-literal=status="$currentStatus" \
	--from-literal=message="$Message" \
	-o yaml --dry-run=client | kubectl replace -f -
fi

if [[ ${currentStatus} == "success" ]]; then
  exit 0
else
  exit 1
fi
