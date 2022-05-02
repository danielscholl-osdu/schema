set -e

ENV_VAR_NAME=$1

if [ "${!ENV_VAR_NAME}" = "" ]
then
    echo "Missing environment variable '$ENV_VAR_NAME'. Please provide all variables and try again"
    exit 1
fi
