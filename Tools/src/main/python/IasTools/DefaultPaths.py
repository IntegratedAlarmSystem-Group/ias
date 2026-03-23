import os
from pathlib import Path
import uuid
class DefaultPaths:
    """
    Class to hold default paths for IAS
    """

    # IAS_ROOT
    _ias_root_env_var: str = "IAS_ROOT"
    _default_ias_root_folder: str = "/opt/IasRoot"

    # IAS_LOGS_FOLDER
    _ias_logs_env_var: str = "IAS_LOGS_FOLDER"
    _default_ias_logs_folder: str = _default_ias_root_folder + "/logs"

    # IAS_TMP_FOLDER
    _ias_tmp_env_var: str = "IAS_TMP_FOLDER"
    _default_ias_tmp_folder: str = _default_ias_root_folder + "/tmp"

    # IAS_CONFIG_FOLDER
    _ias_config_env_var: str = "IAS_CONFIG_FOLDER"
    _default_ias_config_folder: str = _default_ias_root_folder + "/config"

    # KAFKA_HOME
    _kafka_home_env_var: str = "KAFKA_HOME"
    _default_kafka_home_folder: str = "/opt/kafka"

    # Logs go to $IAS_LOGS_FOLDER (default $IAS_ROOT/logs) but some python tools run 
    # outside of the IAS like for example the UdpPlugin.
    # In this case $IAS_LOGS_FOLDER or $IAS_ROOT my not be defined: 
    # one of the following default folders must be chosen 
    # (picking up the first one where a file can be written).
    # If the folder exists then it must be writable
    # if it does not exist, the tool tries to create it
    _alternative_logs_folders: list[str] = [
        "/var/log/ias",
        " /opt/IasRoot/logs",
        "/var/log",
        os.getenv('HOME','.'),
        "."
    ]

    @classmethod
    def get_ias_root_var_name(cls) -> str:
        """
         Get the IAS root environment variable name.
        
        :return: The IAS root environment variable name.
        """
        return cls._ias_root_env_var

    @classmethod
    def get_ias_logs_var_name(cls) -> str:
        """
         Get the IAS logs environment variable name.
        
        :return: The IAS logs folder path.
        """
        return cls._default_ias_logs_folder

    @classmethod
    def get_ias_tmp_var_name(cls) -> str:
        """
        Get the IAS temporary environment variable name.
        
        :return: The IAS tmp environment variable name.
        """
        return cls._ias_tmp_env_var

    @classmethod
    def get_ias_config_var_name(cls) -> str:
        """
        Get the IAS configuration environment variable name.
        
        :return: The IAS configuration environment variable name.
        """
        return cls._ias_config_env_var
    
    @classmethod
    def get_kafka_home_var_name(cls) -> str:
        """
        Get the Kafka home environment variable name.
        
        :return: The Kafka home environment variable name.
        """
        return cls._kafka_home_env_var

    @classmethod
    def get_ias_root_folder(cls) -> str:
        """
        Get the IAS root folder from the environment or default.
        
        :return: The IAS root folder path from the environment or the default
        """
        if not cls._ias_root_env_var in os.environ:
            print("The IAS_ROOT environment variable is not set. Using default: %s", cls._default_ias_root_folder)
        return os.getenv(cls._ias_root_env_var, cls._default_ias_root_folder)

    @classmethod
    def _check_log_folder_candidate(cls, folder: str) -> bool:
        path = Path(folder)
        # Does the folder exist?
        if not path.is_dir():
            # Try to create it
            try:
                path.mkdir(parents=True, exist_ok=True)
            except Exception:
                return False

        if not path.is_dir():
            # Does no exists even after having tried to create
            return False
        
        # The folder exists, if writable ==> Ok
        try:
            test_file = path / f".__writability_test__{str(uuid.uuid4())}"
            test_file.touch()
            test_file.unlink(missing_ok=True)
            return True
        except Exception as e:
            return False

    @classmethod
    def get_ias_logs_folder(cls) -> str:
        """
        Get the IAS logs folder from the environment or one of the default
        folders in cls._alternative_log_folder
        
        :return: The IAS logs folder path from the environment or the default
        """
        logs_folder_env_var: str|None = os.getenv(cls._ias_logs_env_var)
        ias_root_env_var: str|None = os.getenv(cls._ias_root_env_var)
        folders: list[str] = []
        if logs_folder_env_var is not None:
            folders.append(logs_folder_env_var)
        if ias_root_env_var is not None:
            folders.append(ias_root_env_var+"/logs")
        # Get the IAS_LOGS_LODER
        folders = folders + cls._alternative_logs_folders

        # Return the first folder in folders where a log file can be written
        # Tries to create the folder, if that does not exist
        for candidate_folder in folders:
            if cls._check_log_folder_candidate(candidate_folder):
                folder = Path(candidate_folder).absolute()
                return str(folder)
    
    @classmethod
    def get_ias_tmp_folder(cls) -> str:
        """
        Get the IAS temporary folder from the environment or default.
        
        :return: The IAS temporary folder path from the environment or the default
        """
        if not cls._ias_tmp_env_var in os.environ:
            print("The IAS_TMP_FOLDER environment variable is not set. Using default: %s", cls._default_ias_tmp_folder)
        return os.getenv(cls._ias_tmp_env_var, cls._default_ias_tmp_folder)

    @classmethod
    def get_ias_config_folder(cls) -> str:
        """
        Get the IAS configuration folder from the environment or default.
        
        :return: The IAS configuration folder path from the environment or the default
        """
        if not cls._ias_config_env_var in os.environ:
            print("The IAS_CONFIG_FOLDER environment variable is not set. Using default: %s", cls._default_ias_config_folder)
        return os.getenv(cls._ias_config_env_var, cls._default_ias_config_folder)

    @classmethod
    def get_kafka_home_folder(cls) -> str:
        """
        Get the Kafka home folder from the environment or default.
        
        :return: The Kafka home folder path from the environment or the default
        """
        if not cls._kafka_home_env_var in os.environ:
            print("The KAFKA_HOME environment variable is not set. Using default: %s", cls._default_kafka_home_folder)
        return os.getenv(cls._kafka_home_env_var, cls._default_kafka_home_folder)

    @classmethod
    def check_and_create_folder(cls, folder_path: str) -> None:
        """
        Check if a folder exists and create it if it does not.
        
        :param folder_path: The path of the folder to check/create.
        :raises OSError: If the folder cannot be created or is not a directory.
        """
        if not os.path.exists(folder_path):
            os.makedirs(folder_path)
        else:
            if not os.path.isdir(folder_path):
                raise OSError(f"{folder_path} is not a directory")
            # Check write permissions
            if not os.access(folder_path, os.W_OK | os.X_OK):
                raise OSError(f"{folder_path} is not writable")

    @classmethod
    def check_ias_folders(cls) -> bool:
        """
        Check and create the IAS folders if they do not exist.
        
        This includes logs, tmp, and config folders.

        :return: True if all folders are checked and created successfully.
        """
        try:
            if not os.path.exists(cls.get_ias_root_folder()):
                raise OSError(f"IAS root folder {cls.get_ias_root_folder()} does not exist")
            cls.check_and_create_folder(cls.get_ias_logs_folder())
            cls.check_and_create_folder(cls.get_ias_tmp_folder())
            return True
        except Exception as e:
            print("Error checking IAS folders: %s", e)
            return False

