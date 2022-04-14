FROM google/cloud-sdk:slim

WORKDIR /opt

COPY ./devops/gcp/bootstrap-osdu-module/*.sh /opt/
COPY ./deployments /opt/

RUN apt-get update && apt-get install jq -y
RUN pip3 install --upgrade pip && pip3 install -r /opt/scripts/requirements.txt && pip3 install -r /opt/scripts/gcp-deployment-requirements.txt

CMD ["/bin/bash", "-c", "source /opt/bootstrap_schema.sh"]
