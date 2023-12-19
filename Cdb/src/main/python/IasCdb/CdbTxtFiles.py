from pathlib import Path
import os
import shutil

from IasCdb.CdbFolders import CdbFolders, Folders
from IasCdb.TextFileType import FileType, TextFileType
class CdbTxtFiles():

    confFileExtension = ".conf"

    @classmethod
    def from_folder(cls, parent_folder: str):
        """
        Build the the CdbTxtFiles from the contento of the passed folder
        i.e. it guess the type of the CDB (YAML, JSON) by looking at
        the content of the foder
        """
        cdb_type = TextFileType.get_cdb_type(parent_folder)
        if cdb_type is None:
            raise ValueError(f"{parent_folder} is not a parent folder of a CDB")
        return CdbTxtFiles(parent_folder, cdb_type)

    def __init__(self, parent_folder: str, files_type: FileType) -> None:
        self.cdb_parent_folder = parent_folder
        self.files_type = files_type
        self.file_extension = TextFileType.getExtension(files_type)
        self.ias_file_name = "ias" + self.file_extension
        self.templates_file_name = "templates" + self.file_extension
        self.iasios_file_name = "iasios" + self.file_extension
        self.transfer_funs_file_name = "tfs" + self.file_extension

        if self._check_parent_folder(parent_folder):
            self.cdb_parent_folder = parent_folder
        else:
            raise IOError(f"Error accessing {self.cdb_parent_folder}: check permission")

    def _check_parent_folder(self, parent_folder):
        return os.path.exists(parent_folder) and \
            os.path.isdir(parent_folder) \
            and os.access(parent_folder, os.W_OK)

    def get_ias_file_path(self) -> str:
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.ROOT, False), self.ias_file_name)

    def get_supervisor_file_path(self, supervisor_id) -> str:
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.SUPERVISOR, False), f"{supervisor_id}{self.file_extension}")

    def get_dasu_file_path(self, dasu_id: str) -> str:
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.DASU, False), f"{dasu_id}{self.file_extension}")

    def get_asce_file_path(self, asce_id: str) -> str:
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.ASCE, False), f"{asce_id}{self.file_extension}")

    def get_iasio_file_path(self) -> str:
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.IASIO, False), self.iasios_file_name)

    def get_tf_file_path(self) -> str:
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.TF, False), self.transfer_funs_file_name)

    def get_template_file_path(self, template_id: str)-> str:
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.TEMPLATE, False), self.templates_file_name)

    def get_client_file_path(self, client_id):
        if client_id is None or not client_id:
            raise ValueError("Invalid null or empty client ID")
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.CLIENT, False), f"{client_id}{self.confFileExtension}")

    def get_plugin_file_path(self, plugin_id):
        if plugin_id is None or not plugin_id:
            raise ValueError("Invalid null or empty plugin ID")
        return os.path.join(CdbFolders.get_subfolder(self.cdb_parent_folder, Folders.PLUGIN, False), f"{plugin_id}{self.file_extension}")
