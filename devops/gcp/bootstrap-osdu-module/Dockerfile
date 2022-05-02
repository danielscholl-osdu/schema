FROM google/cloud-sdk:slim

WORKDIR /opt

COPY ./devops/gcp/bootstrap-osdu-module/*.sh ./
COPY ./deployments ./

RUN apt-get update && apt-get install jq -y
RUN pip3 install --upgrade pip && pip3 install -r ./scripts/requirements.txt && pip3 install -r ./scripts/gcp-deployment-requirements.txt

CMD ["/bin/bash", "-c", "source ./bootstrap_schema.sh && sleep 365d"]
