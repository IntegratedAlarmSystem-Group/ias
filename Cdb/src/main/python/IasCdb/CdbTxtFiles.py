from pathlib import Path
import os
import shutil

from CdbFolders import CdbFolders

class CdbTxtFiles():

    confFileExtension = ".conf"

    def __init__(self, parent_folder, files_type):
        self.files_type = files_type
        self.file_extension = files_type.ext
        self.ias_file_name = "ias" + self.file_extension
        self.templates_file_name = "templates" + self.file_extension
        self.iasios_file_name = "iasios" + self.file_extension
        self.transfer_funs_file_name = "tfs" + self.file_extension

        if self._check_parent_folder(parent_folder):
            self.cdb_parent_folder = parent_folder
        else:
            raise IOError(f"Check folder permission {parent_folder.resolve()}")

    def _check_parent_folder(self, p):
        return p.exists() and p.is_dir() and os.access(p, os.W_OK)

    def _get_subfolder_path(self, subfolder_enum, file_name):
        return CdbFolders.get_subfolder(self.cdb_parent_folder, subfolder_enum, True).resolve(file_name)

    def get_ias_file_path(self):
        return self._get_subfolder_path(CdbFolders.ROOT, self.ias_file_name)

    def get_supervisor_file_path(self, supervisor_id):
        return self._get_subfolder_path(CdbFolders.SUPERVISOR, f"{supervisor_id}{self.file_extension}")

    def get_dasu_file_path(self, dasu_id):
        return self._get_subfolder_path(CdbFolders.DASU, f"{dasu_id}{self.file_extension}")

    def get_asce_file_path(self, asce_id):
        return self._get_subfolder_path(CdbFolders.ASCE, f"{asce_id}{self.file_extension}")

    def get_iasio_file_path(self, iasio_id):
        return self._get_subfolder_path(CdbFolders.IASIO, self.iasios_file_name)

    def get_tf_file_path(self, tf_id):
        return self._get_subfolder_path(CdbFolders.TF, self.transfer_funs_file_name)

    def get_template_file_path(self, template_id):
        return self._get_subfolder_path(CdbFolders.TEMPLATE, self.templates_file_name)

    def get_client_file_path(self, client_id):
        if client_id is None or not client_id:
            raise ValueError("Invalid null or empty client ID")
        return self._get_subfolder_path(CdbFolders.CLIENT, f"{client_id}{self.confFileExtension}")

    def get_plugin_file_path(self, plugin_id):
        if plugin_id is None or not plugin_id:
            raise ValueError("Invalid null or empty plugin ID")
        return self._get_subfolder_path(CdbFolders.PLUGIN, f"{plugin_id}{self.file_extension}")
