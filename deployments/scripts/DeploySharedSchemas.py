import json
import os
import time
import requests
import argparse
from Utility import Utility, RunEnv


class DeploySharedSchemas:

    def __init__(self):
        parser = argparse.ArgumentParser(
            description="Given a path to an load sequence file, load/update the schemas "
                        "listed in the load sequence file.")
        parser.add_argument('-l', type=str,
                            help='The path to the load sequence file, e.g. load_sequence.?.?.?',
                            default=None)
        parser.add_argument('-u', help='The complete URL to the Schema Service.',
                            default=None)
        arguments = parser.parse_args()
        if arguments.l is not None:
            RunEnv.LOAD_SEQUENCE = arguments.l
        if arguments.u is not None:
            RunEnv.SCHEMA_SERVICE_URL = arguments.u
        if RunEnv.SCHEMA_SERVICE_URL is None:
            exit('The schema service URL is not specified')

        self.url = RunEnv.SCHEMA_SERVICE_URL
        self.schema_registered = None
        self.token = RunEnv.BEARER_TOKEN

        ok, error_mess = RunEnv().is_ok()
        if not ok:
            exit('Error: environment setting incomplete: {}'.format(error_mess))

    def create_schema(self):
        messages = list()
        tenant = RunEnv.DATA_PARTITION
        deployments = Utility.path_to_deployments()
        start = time.time()

        headers = {
            'data-partition-id': tenant,
            'Content-Type': 'application/json',
            'AppKey': RunEnv.APP_KEY,
            'Authorization': self.token
        }

        sequence = Utility.load_json(os.path.join(deployments, *RunEnv.OSDU, RunEnv.LOAD_SEQUENCE))
        for item in sequence:
            self.schema_registered = None
            schema_file = os.path.join(deployments, item['relativePath'])
            kind = item['kind']
            schema = open(schema_file, 'r').read()
            # attempt to load this kind
            exists = self.__does_kind_exist(kind, headers)
            method = 'POST'
            if exists:
                if schema_file.endswith('.dev.json'):
                    method = 'PUT'
                    response = requests.request(method, self.url, headers=headers, data=schema)
                else:
                    message = 'Error: The published kind {} cannot be updated; it already exists.'.format(kind)
                    print(message)
                    messages.append(message)
                    response = None
            else:
                response = requests.request(method, self.url, headers=headers, data=schema)

            if response is not None:
                print('Success: kind {} submitted with method {} schema.'.format(kind, method))
                code = response.status_code
                if code not in range(200,300):
                    # response_message = json.loads(response.text)
                    # if response_message.get('error'):
                    messages.append(
                            'Error with kind {}: Message: {}'.format(kind, response.text))

        elapsed = time.time() - start
        print('This update took {:.2f} seconds.'.format(elapsed))
        if len(messages) != 0:
            print('Following schemas failed:')
            print('\n'.join(messages))
            exit(1)
        else:
            print('All {} schemas registered or updated.'.format(str(len(sequence))))

    def __does_kind_exist(self, kind, headers):
        url = '{}/{}'.format(self.url, kind)
        response = requests.request("GET", url, headers=headers)
        if response.status_code in range(200,300):
            self.schema_registered = json.loads(response.text, encoding='utf-8')
        return response.status_code in range(200,300)


if __name__ == '__main__':
    DeploySharedSchemas().create_schema()
