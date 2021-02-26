# Copyright © 2020 Amazon Web Services
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http:#www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os;
import boto3;
import jwt;

class AwsToken(object):

    def get_aws_id_token(self):
      if os.getenv("AWS_COGNITO_REGION") is not None:      
          region = os.environ["AWS_COGNITO_REGION"]
      else:
          region = os.environ["AWS_REGION"]

      client = boto3.client('cognito-idp', region_name=region)

      userAuth = client.initiate_auth(
          ClientId= os.environ['AWS_COGNITO_CLIENT_ID'],

          AuthFlow= os.environ['AWS_COGNITO_AUTH_FLOW'],
          AuthParameters= {
              "USERNAME": os.environ['AWS_COGNITO_AUTH_PARAMS_USER'],
              "PASSWORD": os.environ['AWS_COGNITO_AUTH_PARAMS_PASSWORD']
          })


      token = 'Bearer ' + userAuth['AuthenticationResult']['AccessToken']
      print(token)
      return token

if __name__ == '__main__':
    AwsToken().get_aws_id_token()