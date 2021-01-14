import argparse
import os
import copy
import pathlib
from Utility import Utility


class ImportFromOSDU(object):
    SCHEMA_INFO = 'schema-version-info'
    LOAD_SEQUENCE_FILE = 'load_sequence.{}.{}.{}.json'
    IGNORE_GROUP_TYPES = ['abstract', 'data-collection', 'manifest']

    def __init__(self):
        parser = argparse.ArgumentParser(
            description="Given a path to an LOAD_SEQUENCE_FOLDERS/SLB schema sub-folder release, "
                        "move and rename schemas for deployment.")
        parser.add_argument('-f', type=str,
                            help='The Generated folder path relative to "deployments"',
                            default='osdu-source/R3-json-schema/Generated')
        parser.add_argument('-u', type=str,
                            help='The kind of Universe - OSDU', default='OSDU')
        arguments = parser.parse_args()

        self.info = None
        self.schema_files = dict()
        self.dependencies = list()
        deployments = Utility.path_to_deployments()
        self.target_path = os.path.join(deployments, 'shared-schemas', arguments.u.lower())
        self.load_sequence_path = self.target_path
        if arguments.u.lower() == 'osdu':
            self.__get_info_file(deployments, self.__get_sub_folder(arguments.f))
            self.discover_schemas(deployments, self.__get_sub_folder(arguments.f))
            self.copy_and_record_dependencies()
            self.order_dependencies()
            self.write_load_sequence(deployments)
        else:
            print('Unrecognized universe: {}'.format(arguments.u))

    @staticmethod
    def __generated_target(file):
        if file.endswith('abstractResources.json'):
            return None
        return file.replace('Authoring', 'Generated')

    def discover_schemas(self, deployments, folder_parts):
        files = Utility.find_files(folder_parts, deployments)
        files = sorted(files)  # this brings abstract to the front, less work for dependency chasing.
        default_version = '0.0.0'
        if self.info is not None:
            default_version = '{}.{}.{}'.format(self.info['majorVersion'],
                                                self.info['minorVersion'],
                                                self.info['patchVersion'])
        for file in files:
            group_type, entity, version, folders = Utility.get_entity_folder_from_file(file, folder_parts)
            if entity == self.SCHEMA_INFO:
                self.info = Utility.load_json(file)
            else:
                target = os.path.join(self.target_path, *folders, entity)
                schema_file = {
                    'entity': entity,
                    'source': file,
                    'target': target
                }
                if group_type is not None and group_type not in self.IGNORE_GROUP_TYPES:
                    schema_file['group-type'] = group_type
                if version is not None:
                    schema_file['version'] = version
                else:
                    schema_file['version'] = default_version
                self.schema_files[entity] = schema_file

    def __get_info_file(self, deployments, folder_parts):
        n = len(folder_parts)
        i = 0
        infos = Utility.find_files(folder_parts, deployments, self.SCHEMA_INFO + '*')
        while len(infos) == 0:
            i -= 1
            infos = Utility.find_files(folder_parts[:-i], deployments, self.SCHEMA_INFO + '*')
            if n + i <= 0:
                break
        if len(infos) > 0:
            self.info = Utility.load_json(infos[-1])

    def copy_and_record_dependencies(self):
        remove_me = list()
        if self.__status() == 'DEVELOPMENT':
            extension = '.dev.json'
        else:
            extension = '.json'
        to_be_excluded = self.__to_be_excluded()
        for key, schema_file in self.schema_files.items():
            if schema_file['entity'] not in to_be_excluded:
                kind_file = self.__make_kind(schema_file, is_file=True) + extension
                os.makedirs(schema_file['target'], exist_ok=True)
                path = os.path.join(schema_file['target'], kind_file)
                schema = Utility.load_json(schema_file['source'])
                schema_file['dependencies'] = self.find_references(schema)
                schema_info = self.__make_schema_info(schema_file, schema)
                to_load = {'schemaInfo': schema_info, 'schema': schema}
                Utility.save_json(to_load, path)
            else:
                remove_me.append(key)
                print('Excluded {}'.format(schema_file['entity']))
        if len(remove_me) > 0:
            for r in remove_me:
                self.schema_files.pop(r)

    def find_references(self, schema):
        my_dependencies = list()
        self.__process(schema, my_dependencies)
        return my_dependencies

    def order_dependencies(self):
        self.__build_dependencies_list()
        # next round: re-order dependencies
        cycle = 0
        swapped = True
        while cycle < 5 and swapped:
            cycle += 1  # recursion limit
            swapped = False
            for key, schema_file in self.schema_files.items():
                this_kind = self.__make_kind(schema_file, is_file=False)
                this_idx = self.dependencies.index(this_kind)
                for dep in schema_file['dependencies']:
                    other_idx = self.dependencies.index(dep)
                    if other_idx > this_idx:
                        other = self.dependencies.pop(other_idx)
                        this_idx = self.dependencies.index(this_kind)
                        self.dependencies.insert(this_idx, other)
                        # print('Round {}: Moved {} in front of {}'.format(cycle, other, this_kind))
                        swapped = True

    def __build_dependencies_list(self):
        for key, schema_file in self.schema_files.items():
            append_this = True
            insert_at = len(self.dependencies)
            this_kind = self.__make_kind(schema_file, is_file=False)
            if this_kind in self.dependencies:
                insert_at = self.dependencies.index(this_kind)
                append_this = False

            for dep in schema_file['dependencies']:
                if dep not in self.dependencies:
                    self.dependencies.insert(insert_at, dep)
                    insert_at += 1
            if append_this:
                self.dependencies.append(this_kind)

    def write_load_sequence(self, base_path):
        sequence = list()
        for dep in self.dependencies:
            parts = dep.split(':')
            parts[2] = parts[2].split('/')[-1]
            dep_without_group_type = ':'.join(parts)
            dep_as_file = dep_without_group_type.replace(':', '..')
            abs_path = Utility.find_file(dep_as_file + '*.json', root=self.target_path)
            if abs_path is not None:
                rel_path = Utility.get_relative_path(base_path, abs_path)
                sequence.append({'kind': dep, 'relativePath': rel_path})
            else:
                print('Error: Reference to {} schema not found.'.format(dep))
        path = os.path.join(self.load_sequence_path,
                            self.LOAD_SEQUENCE_FILE.format(self.__major(), self.__minor(), self.__patch()))
        Utility.save_json(sequence, path)

    def __process(self, raw, my_dependencies):
        if isinstance(raw, dict):
            self.__process_d_ref(my_dependencies, raw)
        elif isinstance(raw, list):
            for item in raw:
                self.__process(item, my_dependencies)

    def __process_d_ref(self, my_dependencies, raw):
        if '$ref' in raw:
            value = raw['$ref']
            if not value.startswith('#/definitions/'):  # ignore internal references
                if value.startswith('https://schema.osdu.opengroup') or value.startswith('https://schema.sdu'):
                    self.__swap_record_r2_reference(my_dependencies, raw, value)
                else:  # standard R3++
                    self.__swap_record_r3_reference(my_dependencies, raw, value)
        else:
            for key, value in raw.items():
                self.__process(value, my_dependencies)

    def __swap_record_r3_reference(self, my_dependencies, raw, value):
        kind = self.__make_kind_from_file(value)
        if kind not in my_dependencies:
            my_dependencies.append(kind)
        raw['$ref'] = kind

    def __make_kind_from_file(self, file_name):
        file_name = pathlib.Path(file_name).as_posix()
        parts = file_name.split('.')
        if len(parts) < 5:
            exit('Error: unexpected $ref: {}'.format(file_name))
        entity = parts[-5].split('/')[-1]
        group_type = self.schema_files.get(entity, dict()).get('group-type')
        name_parts = list()
        if group_type is not None:
            name_parts.append(group_type)
        name_parts.append(entity)
        entity = '/'.join(name_parts)
        entity_info = {'entity': entity, 'version': '.'.join(parts[-4:-1])}
        kind = self.__make_kind(entity_info, is_file=False)
        return kind

    def __swap_record_r2_reference(self, my_dependencies, raw, value):
        parts = value.split('/')
        if len(parts) >= 2:  # roll up from the back
            entity = parts[-2].replace('.json', '')  # should not happen
            kind = self.__make_kind(entity, is_file=False)
            if kind not in my_dependencies:
                my_dependencies.append(kind)
            # print('replaced reference {}'.format(kind))
            raw['$ref'] = kind

    def __make_schema_info(self, entity_info, schema=None):
        if isinstance(schema, dict):
            kind = schema.get('x-osdu-schema-source', 'error')
            entity_info['authority'] = kind.split(':')[0]
            entity_info['source'] = kind.split(':')[1]
            entity_info['entity'] = kind.split(':')[2]
            entity_info['version'] = kind.split(':')[3]
        else:
            kind = self.__make_kind(entity_info, is_file=False)
        entity = entity_info['entity']
        version = entity_info['version']
        major = version.split('.')[0]
        minor = version.split('.')[1]
        patch = version.split('.')[2]
        schema_info = {
            "schemaIdentity": {
                "authority": self.__authority(),
                "source": self.__source(),
                "entityType": entity,
                "schemaVersionMajor": int(major),
                "schemaVersionMinor": int(minor),
                "schemaVersionPatch": int(patch),
                "id": kind
            },
            "createdBy": self.__created_by(),
            "scope": self.__scope(),
            "status": self.__status()
        }
        return schema_info

    def __make_kind(self, entity_file, is_file=True):
        if isinstance(entity_file, dict):
            version = entity_file['version']
            entity = entity_file['entity']
        else:
            version = '.'.join([self.__major(), self.__minor(), self.__patch()])
            entity = entity_file
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