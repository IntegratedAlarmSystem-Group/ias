import os

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
            from IASLogging.logConf import Log
            _logger = Log.getLogger(__file__)
            _logger.warning("The IAS_ROOT environment variable is not set. Using default: %s", cls._default_ias_root_folder)
        return os.getenv(cls._ias_root_env_var, cls._default_ias_root_folder)

    @classmethod
    def get_ias_logs_folder(cls) -> str:
        """
        Get the IAS logs folder from the environment or default.
        
        :return: The IAS logs folder path from the environment or the default
        """
        if not cls._ias_logs_env_var in os.environ:
            print(f"WARN: The {cls._ias_logs_env_var} environment variable is not set. Using default: {cls._default_ias_logs_folder}")
            log_folder = cls._default_ias_logs_folder
        else:
            log_folder = os.getenv(cls._ias_logs_env_var)

        # Check if the log folder is writable
        if os.path.isdir(log_folder) and os.access(log_folder, os.W_OK):
            return log_folder
        else:
            print(f"WARN: log folder {log_folder} does not exists or not writable: using {os.getcwd()} folder for storing logs")
            return os.getcwd()
    
    @classmethod
    def get_ias_tmp_folder(cls) -> str:
        """
        Get the IAS temporary folder from the environment or default.
        
        :return: The IAS temporary folder path from the environment or the default
        """
        if not cls._ias_tmp_env_var in os.environ:
            from IASLogging.logConf import Log
            _logger = Log.getLogger(__file__)
            _logger.warning("The IAS_TMP_FOLDER environment variable is not set. Using default: %s", cls._default_ias_tmp_folder)
        return os.getenv(cls._ias_tmp_env_var, cls._default_ias_tmp_folder)

    @classmethod
    def get_ias_config_folder(cls) -> str:
        """
        Get the IAS configuration folder from the environment or default.
        
        :return: The IAS configuration folder path from the environment or the default
        """
        if not cls._ias_config_env_var in os.environ:
            from IASLogging.logConf import Log
            _logger = Log.getLogger(__file__)
            _logger.warning("The IAS_CONFIG_FOLDER environment variable is not set. Using default: %s", cls._default_ias_config_folder)
        return os.getenv(cls._ias_config_env_var, cls._default_ias_config_folder)

    @classmethod
    def get_kafka_home_folder(cls) -> str:
        """
        Get the Kafka home folder from the environment or default.
        
        :return: The Kafka home folder path from the environment or the default
        """
        if not cls._kafka_home_env_var in os.environ:
            from IASLogging.logConf import Log
            _logger = Log.getLogger(__file__)
            _logger.warning("The KAFKA_HOME environment variable is not set. Using default: %s", cls._default_kafka_home_folder)
        return os.getenv(cls._kafka_home_env_var, cls._default_kafka_home_folder)

    @classmethod
    def check_and_create_folder(cls, folder_path: str) -> None:
        """
        Check if a folder exists and create it if it does not.
        
        :param folder_path: The path of the folder to check/create.
        :raises OSError: If the folder cannot be created or is not a directory.
        """
        from IASLogging.logConf import Log
        _logger = Log.getLogger(__file__)
        _logger.debug("Checking and creating folder: %s", folder_path)
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
        from IASLogging.logConf import Log
        _logger = Log.getLogger(__file__)
        try:
            if not os.path.exists(cls.get_ias_root_folder()):
                raise OSError(f"IAS root folder {cls.get_ias_root_folder()} does not exist")
            _logger.debug("IAS root folder exists")
            cls.check_and_create_folder(cls.get_ias_logs_folder())
            _logger.debug("IAS logs folder ready")
            cls.check_and_create_folder(cls.get_ias_tmp_folder())
            _logger.debug("IAS tmp folder ready")
            return True
        except Exception as e:
            _logger.error("Error checking IAS folders: %s", e)
            return False

