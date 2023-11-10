from pathlib import Path
from enum import Enum
import os
from typing import Union

class FileType(Enum):
    YAML=1,
    JSON=2

class TextFileType:

    # Supported file types and their extension
    SUPPORTED_TYPES = { FileType.YAML:".yaml", FileType.JSON:".json"}

    @classmethod
    def from_file(cls, file_name: str) -> Union[FileType, None]:
        """
        Get the type of the file from the extension of the passed file name

        Args:
            file_name: the file name
        Returns:
            The type of the file (belongs to FileType) 
            or None if the type has not been recognized
        """
        if not file_name:
            raise ValueError("Invalid empty file name")
        
        # Get the extension of the file
        pos = file_name.rfind('.')
        if pos == -1:
            raise ValueError("File has no extension: " + file_name)

        ext = file_name[pos:].lower()
        if ext == TextFileType.SUPPORTED_TYPES[FileType.JSON]:
            return FileType.JSON
        elif ext == TextFileType.SUPPORTED_TYPES[FileType.YAML]:
            return FileType.YAML
        else:
            return None

    @classmethod
    def get_cdb_type(cls, folder: str) -> Union[FileType, None]:
        """
        Get the type of the CDB from the passed folder.
        It checks the content of the CDB folder looking for a file named ias.ext

        Args:
            folder: The parent folder of the CDB
        Returns:
            the type of the text files in the CDB folder,
            or empty if cannot recognize the type of the CDB of the text files
        """
        if not folder:
            raise ValueError(f"Invalid empty folder")

        cdb_folder = folder+"/CDB"
        if not os.path.isdir(cdb_folder):
            raise ValueError(f"The folder {cdb_folder} is unreadable or does not exist")

        # Look for ias.*: there should be only one
        files = [f.name for f in cdb_folder.iterdir() if f.name.startswith("ias.")]
        if len(files) != 1:
            return None
        else:
            return cls.from_file(files[0])