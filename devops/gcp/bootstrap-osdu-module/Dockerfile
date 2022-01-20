FROM google/cloud-sdk:slim

COPY ./devops/gcp/bootstrap-osdu-module/bootstrap_schema.sh /opt 
COPY ./deployments /opt/

RUN chmod 775 /opt/bootstrap_schema.sh

RUN pip3 install --upgrade pip && pip3 install -r /opt/scripts/requirements.txt && pip3 install -r /opt/scripts/gcp-deployment-requirements.txt

CMD ["/bin/bash", "-c", "source /opt/bootstrap_schema.sh"]
