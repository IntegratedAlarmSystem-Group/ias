import os
from IasCdb.TextFileType import FileType, TextFileType
from IasCdb.Dao.LogLevelDao import LogLevelDao

class IasDao:
    """
    Global configuration for the IAS
    """

    @classmethod
    def from_file(cls, file_name: str, file_type: FileType): # -> IasDao:
        """
        Factory method taht builds and return a IasDao parsing 
        the passed file

        Args:
            file_name: the name of the file to parse
        Returns:
            The IasDao built parsing the file
        """
        if not os.path.isfile(file_name):
            raise ValueError(f"Cannot read {file_name}")
        if file_type==FileType.YAML:
            cls.read_from_yaml(file_name)
        elif file_type==FileType.JSON:
            return cls.read_from_json(file_name)
        else :
            raise ValueError(f"Unrecognized file {file_name}")
        
    
    @classmethod
    def read_from_yaml(cls, file_name: str):
        pass

    @classmethod
    def read_from_json(cls, file_name: str):
        """
        Read the ias configuration from a JSON file
        Args:
            file_name: the json file containing the IAS configuratio
        Return:
            The IasDao built parsing the file
        """
        import json
        with open(file_name) as f:
                data = json.load(f)
        log_lvl = LogLevelDao.from_string(data['logLevel'])
        refresh_rate=int(data['refreshRate'])
        validity_threshold = int(data['validityThreshold'])
        hb_frequency=int(data['hbFrequency'])
        bsdb_url = data['bsdbUrl']
        try:
            smtp = data['smtp']
        except:
            smtp = None
        props = {}
        for p in data['props']:
            name = p['name']
            value=p['value']
            props[name]=value
        
        return IasDao(log_lvl, props, refresh_rate,validity_threshold,hb_frequency,bsdb_url,smtp)

    def __init__(self, \
                 log_level: LogLevelDao,
                 props: dict[str, str],\
                 refresh_rate: int,
                 validity_threshold: int,
                 hb_frequency: int,
                 bsdb_url: str,
                 smtp: str|None, #Optional
                 ):
        """
        Constructor
        Reads the ias.json or ias.yaml from the CDB
        """
        self.log_level: LogLevelDao = log_level
        self.props: dict[str, str] = props
        self.refresh_rate: int = refresh_rate
        self.validity_threshold: int = validity_threshold
        self.hb_frequency: int = hb_frequency
        self.bsdb_url: str = bsdb_url
        self.smtp: str|None = smtp

    def __str__(self):
        """
        A string representation of the IasDao
        """
        ret = f"IAS=[logLevel={self.log_level.to_string()}, refreshRate={self.refresh_rate}"
        ret = ret + f", validityThreshold={self.validity_threshold}, heartebeat frequency={self.hb_frequency}"
        ret = ret + f", BSDB URL='{self.bsdb_url}'"
        if self.smtp is not None:
            ret = ret + f", SMTP={self.smtp}"
        ret = ret + ", props={"
        if len(self.props)>0:
            first = True
            for k in self.props.keys():
                if first:
                    first = False
                else:
                    ret = ret +', '
                ret = ret + f"<{k}, {self.props[k]}>"
        ret = ret + '}'
        return ret
