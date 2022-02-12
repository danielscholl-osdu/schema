# Cleanup function
cleanup() {
  echo "Terminating istio sidecar"
  curl -X POST "http://localhost:15020/quitquitquit"
}

# Function to check istio sidecar readiness
checkIstioSidecarReadiness() {
	echo "Wait for istio sidecar to be ready..."
	
	max_retry_count=18
	current_retry_count=0
	sidecar_ready_status=0
	
	while [ ${current_retry_count} -lt ${max_retry_count} ];
	do
		status_code=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost:15021/healthz/ready)
		if [[ "$status_code" -ne 200 ]] ; then
		sleep 10s
		echo "Istio sidecar not ready yet. Sleeping for 10s..."
		else
		sidecar_ready_status=1
		break
		fi
		current_retry_count=$(expr $current_retry_count + 1)
	done
	
	if [[ ${sidecar_ready_status} -ne 1 ]]; then
		echo "Timed out waiting for istio sidecar to be ready. Exiting..."
		exit 1
	fi
	
	echo "Istio sidecar is ready..."
}

trap cleanup 0 1 2 3 6

checkIstioSidecarReadiness

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