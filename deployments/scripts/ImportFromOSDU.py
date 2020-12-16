import argparse
import os
from Utility import Utility


class ImportFromOSDU(object):
    SCHEMA_INFO = 'schema-version-info'
    LOAD_SEQUENCE_FILE = 'load_sequence.{}.{}.{}.json'

    def __init__(self):
        parser = argparse.ArgumentParser(
            description="Given a path to an OSDU schema sub-folder release, move and rename schemas for deployment.")
        parser.add_argument('-f', type=str,
                            help='The folder path relative to "deployments"',
                            default='osdu-source/R3-json-schema')
        arguments = parser.parse_args()

        self.info = None
        self.schema_files = dict()
        self.dependencies = list()
        deployments = Utility.path_to_deployments()
        self.target_path = os.path.join(deployments, 'shared-schemas', 'osdu')
        self.discover_schemas(deployments, self.__get_sub_folder(arguments.f))
        self.copy_and_record_dependencies()
        self.order_dependencies()
        self.write_load_sequence(deployments)
        pass

    def discover_schemas(self, deployments, folder_parts):
        files = Utility.find_files(folder_parts, deployments)
        for file in files:
            entity, folders = Utility.get_entity_folder_from_file(file, folder_parts)
            if entity == self.SCHEMA_INFO:
                self.info = Utility.load_json(file)
            else:
                target = os.path.join(self.target_path, *folders, entity)
                schema_file = {
                    'entity': entity,
                    'source': file,
                    'target': target
                }
                self.schema_files[entity] = schema_file

    def copy_and_record_dependencies(self):
        remove_me = list()
        if self.__status() == 'DEVELOPMENT':
            extension = '.dev.json'
        else:
            extension = '.json'
        to_be_excluded = self.__to_be_excluded()
        for key, schema_file in self.schema_files.items():
            if schema_file['entity'] not in to_be_excluded:
                kind_file = self.__make_kind(schema_file['entity'], is_file=True) + extension
                os.makedirs(schema_file['target'], exist_ok=True)
                path = os.path.join(schema_file['target'], kind_file)
                schema = Utility.load_json(schema_file['source'])
                schema_file['dependencies'] = self.find_references(schema)
                schema_info = self.__make_schema_info(schema_file['entity'])
                to_load = {'schemaInfo': schema_info, 'schema': schema}
                Utility.save_json(to_load, path)
            else:
                remove_me.append(key)
                print('Excluded {}'.format(schema_file['entity']))
        if len(remove_me) > 0:
            for r in remove_me:
                self.schema_files.pop(r)
        pass

    def find_references(self, schema):
        my_dependencies = list()
        self.__process(schema, my_dependencies)
        return my_dependencies

    def order_dependencies(self):
        for key, schema_file in self.schema_files.items():
            append_this = True
            insert_at = len(self.dependencies)
            this_kind = self.__make_kind(schema_file['entity'], is_file=False)
            if this_kind in self.dependencies:
                insert_at = self.dependencies.index(this_kind)
                append_this = False

            for dep in schema_file['dependencies']:
                if dep not in self.dependencies:
                    self.dependencies.insert(insert_at, dep)
                    insert_at += 1
            if append_this:
                self.dependencies.append(this_kind)
        # next round: re-order dependencies
        cycle = 0
        swapped = True
        while cycle < 5 and swapped:
            cycle += 1  # recursion limit
            swapped = False
            for key, schema_file in self.schema_files.items():
                this_kind = self.__make_kind(schema_file['entity'], is_file=False)
                this_idx = self.dependencies.index(this_kind)
                for dep in schema_file['dependencies']:
                    other_idx = self.dependencies.index(dep)
                    if other_idx > this_idx:
                        other = self.dependencies.pop(other_idx)
                        this_idx = self.dependencies.index(this_kind)
                        self.dependencies.insert(this_idx, other)
                        # print('Round {}: Moved {} in front of {}'.format(cycle, other, this_kind))
                        swapped = True

    def write_load_sequence(self, base_path):
        sequence = list()
        for dep in self.dependencies:
            dep_as_file = dep.replace(':', '..')
            abs_path = Utility.find_file(dep_as_file + '*.json', root=base_path)
            if abs_path is not None:
                rel_path = Utility.get_relative_path(base_path, abs_path)
                sequence.append({'kind': dep, 'relativePath': rel_path})
            else:
                print('Error: Reference to {} schema not found.'.format(dep))
        path = os.path.join(self.target_path,
                            self.LOAD_SEQUENCE_FILE.format(self.__major(), self.__minor(), self.__patch()))
        Utility.save_json(sequence, path)
        pass

    def __process(self, raw, my_dependencies):
        if isinstance(raw, dict):
            if '$ref' in raw:
                value = raw['$ref']
                if not value.startswith('#/definitions/'):  # ignore internal references
                    parts = value.split('/')
                    if len(parts) >= 2:  # roll up from the back
                        entity = parts[-2].replace('.json', '')   # should not happen
                        kind = self.__make_kind(entity, is_file=False)
                        if kind not in my_dependencies:
                            my_dependencies.append(kind)
                        # print('replaced reference {}'.format(kind))
                        raw['$ref'] = kind
            else:
                for key, value in raw.items():
                    self.__process(value, my_dependencies)
        elif isinstance(raw, list):
            for item in raw:
                self.__process(item, my_dependencies)

    def __make_schema_info(self, entity):
        kind = self.__make_kind(entity, is_file=False)
        schema_info = {
            "schemaIdentity": {
                "authority": self.__authority(),
                "source": self.__source(),
                "entityType": entity,
                "schemaVersionMajor": int(self.__major()),
                "schemaVersionMinor": int(self.__minor()),
                "schemaVersionPatch": int(self.__patch()),
                "id": kind
            },
            "createdBy": self.__created_by(),
            "scope": self.__scope(),
            "status": self.__status()
        }
        return schema_info

    def __make_kind(self, entity, is_file=True):
        version = '.'.join([self.__major(), self.__minor(), self.__patch()])
        if is_file:
            sep = '..'
        else:
            sep = ':'
        return sep.join([self.__authority(), self.__source(), entity, version])

    def __authority(self):
        return self.info['authority']

    def __source(self):
        return self.info['source']

    def __major(self):
        return str(self.info['majorVersion'])

    def __minor(self):
        return str(self.info['minorVersion'])

    def __patch(self):
        return str(self.info['patchVersion'])

    def __created_by(self):
        return self.info['createdBy']

    def __status(self):
        return self.info['status']

    def __scope(self):
        return self.info['scope']

    def __to_be_excluded(self):
        if 'exclude' in self.info:
            return self.info['exclude']
        else:
            return []

    @staticmethod
    def __get_sub_folder(folder_path):
        if '/' in folder_path:
            return folder_path.split('/')
        elif '\\' in folder_path:
            return folder_path.split('\\')
        return [folder_path]


if __name__ == '__main__':
    ImportFromOSDU()