import os

from IasCdb.TextFileType import FileType, TextFileType
from IasCdb.CdbTxtFiles import CdbTxtFiles
from IasCdb.Dao.IasDao import IasDao
from IasCdb.Dao.IasioDao import IasioDao

class CdbReader:
    """
    The class to read the CDB in YAML or JSON
    """
    def __init__(self, parent_folder: str) -> None:
        """
        Constructor.

        Args:
            parent_folder: the folder that contains CDB
        """
        self.parent_folder=parent_folder
        if not os.path.isdir(parent_folder):
            raise ValueError(f"Invalid parent folder {parent_folder}")
        self.cdbTxtFiles = CdbTxtFiles.from_folder(parent_folder)
        self.files_type = self.cdbTxtFiles.files_type

    def get_ias(self) -> IasDao:
        return IasDao.from_file(self.cdbTxtFiles.get_ias_file_path(),self.files_type)
	
    def get_iasios(self) -> list[IasioDao]:
        iasios_file = self.cdbTxtFiles.get_iasio_file_path()
        if not os.path.isfile(iasios_file):
            raise ValueError(f"Cannot read {iasios_file}")
        if self.files_type==FileType.YAML:
            import yaml
            with open(iasios_file) as f:
                data=yaml.safe_load(f)
        elif self.files_type==FileType.JSON:
            import json
            with open(iasios_file) as f:
                data = json.load(f)
        else :
            raise ValueError(f"Unrecognized file type {self.files_type}")
        iasios: list[IasioDao] = []
        for iasio_data in data:
            iasio = IasioDao.from_dict(iasio_data)
            iasios.append(iasio)
        return iasios

    def get_iasio(self, id: str) -> IasioDao|None:
        if not id or id.strip()=='':
            raise ValueError("Invalid null or empty IASIO ID")
        iasios = self.get_iasios()
        for iasio in iasios:
            if iasio.id==id:
                return iasio
        return None
    
    def get_supervisor(self, id: str):
        """
		Read the supervisor configuration from the CDB
		Args
            id: The not empty supervisor identifier
		"""
        raise NotImplementedError("Method not yet implemented")
	

    def get_transfer_function(self, id: str):
        """
        Read the transfer function configuration from the CDB.
        Args:
            id: The not empty transfer function identifier
        """
        raise NotImplementedError("Method not yet implemented")

    def get_transfer_functions(self):
        """
        Get the transfer functions
        """
        raise NotImplementedError("Method not yet implemented")

    def get_template(self, id: str):
        """
        Read the ttemplate configuration from the CDB.
        Args:
            id: The not empty identifier of the template
        """
        raise NotImplementedError("Method not yet implemented")

    def get_templates(self):
        """
        Get the templates.
        """
        raise NotImplementedError("Method not yet implemented")

    def get_asce(self, id: str):
        """
        Read the ASCE configuration from the CDB.

        Args:
            id: The not empty ASCE identifier
        """
        raise NotImplementedError("Method not yet implemented")

    def get_dasu(self, id: str):
        """
        Read the DASU configuration from the CDB.

        Args:
            id: The not empty DASU identifier
        """
        raise NotImplementedError("Method not yet implemented")
	
    def get_Dasus_to_deploy_in_supervisor(self, id: str):
        """
        Return the DASUs to deploy in the Supervisor with the given identifie

        Args:
            id: The not empty identifier of the supervisor
        """
        raise NotImplementedError("Method not yet implemented")

    def get_asces_for_dasu(self, id: str):
        """
        Return the ASCEs belonging to the given DASU.

        Args:
            id: The not empty identifier of the DASU
        """
        raise NotImplementedError("Method not yet implemented")

    def get_iasios_for_asce(self, id: str):
        """
        Return the IASIOs in input to the given ASCE.

        Args:
            id: The not empty identifier of the ASCE
        """
        raise NotImplementedError("Method not yet implemented")

    def getSupervisorIds(self):
       """
       Get the IDs of the Supervisors.
       """
       raise NotImplementedError("Method not yet implemented")

    def get_dasu_ids(self):
        """
        Get the IDs of the DASUs.
        """
        raise NotImplementedError("Method not yet implemented")

    def get_asce_ids(self):
        """
        Get the IDs of the ASCEs.
        """
        raise NotImplementedError("Method not yet implemented")

    def get_template_instances_iasios_for_asce(self, id: str):
        """
        Return the templated IASIOs in input to the given ASCE.

        Args:
            id: The not empty identifier of the ASCE
        """
        raise NotImplementedError("Method not yet implemented")

    def get_client_config(self, id: str):
        """
        Get the configuration of the client with the passed identifier.
        Args:
            id: The not empty ID of the IAS clien
        """
        raise NotImplementedError("Method not yet implemented")

    def get_plugin(self, id: str):
        """
        Get the configuration of the plugin with the passed identifier.

        The configuration of the plugin can be read from a file or from the CDB.
        In both cases, the configuration is returned as PluginConfigDao
        This method returns the configuration from the CDB; reading from file is
        not implemented.

        Args:
            id: he not empty ID of the IAS plugin
        """
        raise NotImplementedError("Method not yet implemented")

    def getPluginIds(self):
        """
        Get the IDs of all the plugins in the CDB
        """
        raise NotImplementedError("Method not yet implemented")

    def get_client_ids(self):
        """
        Get the IDs of all the plugins in the CDB
        """
        raise NotImplementedError("Method not yet implemented")
