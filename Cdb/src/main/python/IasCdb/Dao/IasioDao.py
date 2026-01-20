"""
The DAO of the IASIOs read from the CDB.
The python equivalent of IasioDao.java.
"""

from enum import EnumType
from enum import EnumType
import os
from IasCdb.TextFileType import FileType
from IasCdb.Dao.IasTypeDao import IasTypeDao
from IasCdb.Dao.SoundTypeDao import SoundTypeDao

class IasioDao:
    """
    The DAO of the IASIOs read from the CDB.
    """

    # By default an alarm can't be shelved
    canSheveDefault = False

    @staticmethod
    def str_to_bool(value: str) -> bool:
        return str(value).lower() in ("true", "1", "yes")

    @classmethod
    def from_dict(cls, data): # -> IasioDao:
        """
        Factory method taht builds and return a IasioDao from the 
        passed dictionary

        Args:
            data: the dictionary (from YAML or JSON) describing the IASIO
        Returns:
            The IasioDao built from the dictionary
        """
        id = data['id']
        shortDesc = data.get('shortDesc', None)
        iasType: IasTypeDao = IasTypeDao.from_string(data['iasType'])
        templateId = data.get('templateId', None)
        docUrl = data.get('docUrl', None)

        if data.get('canShelve') is not None:
            canShelve: bool = cls.str_to_bool(data.get('canShelve'))
        else:
            canShelve: bool = IasioDao.canSheveDefault

        sound: SoundTypeDao = SoundTypeDao.from_string(data.get('sound', 'NONE'))
        emails = data.get('emails', None)
        
        return IasioDao(id, iasType, shortDesc, docUrl, templateId, canShelve, sound, emails)

    def __init__(self, 
                 id: str, 
                 iasType: IasTypeDao,
                 shortDesc: str|None,
                 docUrl: str|None,
                 templateId: str|None,
                 canShelve: bool,
                 sound: SoundTypeDao,
                 emails: str|None):
        """
        Constructor.

        Args:
            iasios: The dictionary of IASIOs read from the CDB
        """
        # The identifier of the IASIO	    self.id: str = id
        self.id: str = id

	    #  Human readable description of the IASIO (optional)
        self.shortDesc: str|None = shortDesc
	
	    # The type of the IASIO
        self.iasType: IasTypeDao = iasType
	
	    # The (optional) URL with the documentation
        self.docUrl: str|None = docUrl
	
	    # The (optional) ID of the template for implementing replication
        self.templateId: str|None = templateId
	
	    # The (optional) attribute saying if a IASIO can be shelved,
	    # initialized with the default value {@link #canSheveDefault}
        self.canShelve: bool = canShelve
	
	    # The (optional) sound to play when a given alarm becomes SET
        # This attribute is ignored for non alarm IASIOs
        self.sound: SoundTypeDao = sound
	
	    # The (optional)  email addresses to send emails when an alarm changes
	    # it state SET/CLEAR
	    # This attribute is ignored for non alarm IASIOs
        self.emails : str|None = emails
    
    def __str__(self):
        """
        A string representation of the IasioDao
        """
        ret = f"IASIO=[id={self.id}, iasType={self.iasType.to_string()}]"
        if self.shortDesc:
            ret += f", shortDesc={self.shortDesc}"
        if self.docUrl:
            ret += f", docUrl={self.docUrl}"
        if self.templateId:
            ret += f", templateId={self.templateId}"
        ret += f", canShelve={self.canShelve}"
        if self.sound!=SoundTypeDao.NONE:
            ret += f", sound={self.sound.to_string()}"
        if self.emails:
            ret += f", emails={self.emails}"
        return ret