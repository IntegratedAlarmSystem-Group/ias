import logging
import atexit
import oracledb

from IasCdb.SqlFile import SqlFile

class SqlRunner(object):
    '''
    Runs SQL scripts and statements after connecting to the oracle
    database.

    Requires oracledb from oracle (https://python-oracledb.readthedocs.io/en/latest/index.html)
    '''

    def __init__(self, user, pswd, dbUrl):
        '''
        Constructor

        :param user: the user name to login to the RDBMS
        :param pswd: the password
        :param dbUrl: the URL to connect to the RDBMS
        '''
        self.logger = logging.getLogger('SqlRunner')

        if user is None:
            raise ValueError("Invalid null/empty user name")
        if pswd is None:
            raise ValueError("Invalid null/empty password")
        if dbUrl is None:
            raise ValueError("Invalid null/empty URL to connectr to the DB")

        conStr = '%s/%s@%s' % (user,pswd,dbUrl)
        self.connection = oracledb.connect(conStr)
        self.logger.debug('Oracle DB %s connected (user %s)',dbUrl,user)

        self.cursor = self.connection.cursor()

        # Register the handler to close the connection with the RDB
        atexit.register(self.close)

        self.logger.debug("SQLRunner built")

    def __getCursor(self,alternateCursor):
        '''
        Get and return the cursor to use between the default and the pased one
        :param alternateCursor: the alternate cursor to use
        :return: the cursor to use
        '''
        if alternateCursor is None:
            ret = self.cursor
        else:
            ret = alternateCursor
        if ret is None:
            raise ValueError('Invalid cursor: already closed?')
        return ret

    def executeSqlStatement(self,cmd,alternateCursor=None):
        '''
        Execute the SQL statement in the parameter

        :param cmd: the SQL statement to run
        :param alternateCursor: the cursor to use instead of the one passed in the constructor
        :return: the number of rows that have currently been fetched from the cursor (
                 for select statements) or that have been affected by the operation
                 (for insert, update and delete statements).
        '''
        if not cmd:
            raise ValueError("Invalid empty/None statement to execute")
        cursorToUse = self.__getCursor(alternateCursor)
        self.logger.debug("Executing SQL [%s]",cmd)
        cursorToUse.execute(cmd)
        self.logger.debug("SQL statement executed")
        return cursorToUse.rowcount

    def executeSqlScript(self,script,alternateCursor=None):
        '''
        Execute the passed SQL script.

        Note: it asumes that
        a) SQL staments are separated by  ';'
        b) the script does not contain ';' other than those for separating SQL statements

        :param script: the script to sun
        :param alternateCursor:  the cursor to use instead of the one passed in the constructor
        :return: the total number of rows that have currently been fetched from the cursor (
                 for select statements) or that have been affected by the operation
                 (for insert, update and delete statements)
        '''
        if not script:
            raise ValueError("Invalid empty/None script to execute")
        cursorToUse = self.__getCursor(alternateCursor)
        n = 0
        self.logger.debug("Executing SQL script")
        sqlStatements = script.split(';')
        for sqlCommand in sqlStatements:
            n = n + self.executeSqlStatement(sqlCommand,cursorToUse)
        return n

    def executeSqlFromFile(self,fileName,alternateCursor=None, ignoreErrors=False):
        """
        Execute the script read from the passed file

        :param fileName: the name of the file with the SQL script
        :param alternateCursor: the cursor to use instead of the one passed in the constructor
        :param ignoreErrors: if True ignore the errors returned by runnng a SQL comamnd
                             otherwise terminates immediately if an error occurs
        :return: the total number of rows that have currently been fetched from the cursor (
                 for select statements) or that have been affected by the operation
                 (for insert, update and delete statements
        """
        if not fileName:
           raise ValueError("Invalid file name")

        sqlFile = SqlFile(fileName)
        sqlStatements = sqlFile.getSQLStatements()

        n = 0
        for sqlStatement in sqlStatements:
            try:
                n = n + self.executeSqlStatement(sqlStatement,alternateCursor)
            except Exception as e:
                if not ignoreErrors:
                    raise e
                else:
                    self.logger.warning("Caught (and ignored) error runnig SQL [%s]: %s",sqlStatement,str(e))

        return n


    def close(self):
        '''
        Close the connection and the cursor.
        Normally invoked by atexit but can be invoked anytime.

        The SqlRunner object can't be used after closing, unless an alternate
        cursor is passed to the methods.

        :return:
        '''
        if self.cursor is not None:
            self.cursor.close()
            self.cursor=None
        if self.connection is not None:
            self.connection.close()
            self.connection=None
        self.logger.debug('SQL runner closed')
