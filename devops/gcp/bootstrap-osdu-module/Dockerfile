FROM google/cloud-sdk:alpine

WORKDIR /opt

COPY ./devops/gcp/bootstrap-osdu-module/*.sh ./
COPY ./deployments ./

RUN apk update && apk add jq bash py3-pip
RUN pip3 install --upgrade pip && pip3 install -r ./scripts/requirements.txt && pip3 install -r ./scripts/gcp-deployment-requirements.txt && pip3 install -r ./scripts/schema-cleaner/requirements.txt
RUN chmod +x /opt/bootstrap_schema.sh

CMD ["/bin/bash", "-c", "/opt/bootstrap_schema.sh && sleep 365d"]
