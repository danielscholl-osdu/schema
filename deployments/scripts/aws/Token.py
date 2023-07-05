# Copyright © 2020 Amazon Web Services
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import os
import base64
import boto3
import requests
import json
from botocore.exceptions import ClientError

class AwsToken(object):

    def _get_ssm_parameter(self, ssm_path, region):
            ssm_client = boto3.client('ssm', region_name=region)
            ssm_response = ssm_client.get_parameter(Name=ssm_path)
            return ssm_response['Parameter']['Value']

    def _get_secret(self, secret_name, secret_dict_key, region):
        # Create a Secrets Manager client
        client = boto3.client(
            service_name='secretsmanager',
            region_name=region
        )

        # In this sample we only handle the specific exceptions for the 'GetSecretValue' API.
        # See https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
        # We rethrow the exception by default.

        try:
            get_secret_value_response = client.get_secret_value(
                SecretId=secret_name
            )
        except ClientError as e:
            print("Could not get client secret from secrets manager")
            raise e
        else:
            # Decrypts secret using the associated KMS CMK.
            # Depending on whether the secret is a string or binary, one of these fields will be populated.
            if 'SecretString' in get_secret_value_response:
                secret = get_secret_value_response['SecretString']
            else:
                decoded_binary_secret = base64.b64decode(get_secret_value_response['SecretBinary'])

        return_secret_serialized = secret
        if return_secret_serialized == None:
            return_secret_serialized = decoded_binary_secret

        return_secret = json.loads(return_secret_serialized)[secret_dict_key]

        return return_secret


    def get_service_principal_token(self):
        if os.getenv("AWS_COGNITO_REGION") is not None:
            region = os.environ["AWS_COGNITO_REGION"]
        else:
            region = os.environ["AWS_REGION"]

        cognito_name = os.environ["COGNITO_NAME"]

        token_url_ssm_path=f"/osdu/cognito/{cognito_name}/oauth/token-uri"
        oauth_custom_scope_ssm_path=f"/osdu/cognito/{cognito_name}/oauth/custom-scope"
        client_id_ssm_path=f"/osdu/cognito/{cognito_name}/client/client-credentials/id"
        client_secret_name=f"/osdu/cognito/{cognito_name}/client-credentials-secret"
        client_secret_dict_key='client_credentials_client_secret'

        # session = boto3.session.Session()
        client_id = self._get_ssm_parameter(client_id_ssm_path, region)
        client_secret = self._get_secret(client_secret_name, client_secret_dict_key, region)
        token_url = self._get_ssm_parameter(token_url_ssm_path, region)
        oauth_custom_scope = self._get_ssm_parameter(oauth_custom_scope_ssm_path, region)

        auth = '{}:{}'.format(client_id, client_secret)
        encoded_auth = base64.b64encode(str.encode(auth))

        headers = {}
        headers['Authorization'] = 'Basic ' + encoded_auth.decode()
        headers['Content-Type'] = 'application/x-www-form-urlencoded'
        form_params={'grant_type': "client_credentials"}

        token_url = '{}?client_id={}&scope={}'.format(token_url, client_id, oauth_custom_scope)


        response = requests.post(url=token_url, headers=headers, data = form_params)
        token = 'Bearer ' + json.loads(response.content.decode())['access_token']
        print(token)
        return token

if __name__ == '__main__':
    AwsToken().get_service_principal_token()