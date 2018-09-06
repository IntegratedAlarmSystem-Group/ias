import atexit

from IasCdb import AlchemyMapping
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

class AlchemyDb(object):
    '''
    AlchemyDb is the RDB abstraction through Alchemy
    '''

    def __init__(self, userName, pswd, server):
        '''
        Connect to the RDBMS and initializes the
        engine and the session

        :param userName: the name of the user o connect to the RDBMS
        :param pswd: the password
        :param server: the URL of the server
        '''
        connStr = 'oracle://%s:%s@%s' % (userName,pswd,server)
        print('Conencting to',connStr)
        #self._engine = create_engine('oracle://iastest:test@192.168.120.130:1521/XE')
        self._engine = create_engine(connStr)
        Session = sessionmaker(bind=self._engine)
        self._session = Session()
        atexit.register(self.close)

    def getSession(self):
        '''
        :return: the alackemy session
        '''
        return self._session

    def close(self):
        '''
        Closes the session upon termination
        :return:
        '''
        if self._session is not None:
            self._session.close()
            self.session = None

        print('Closed')

