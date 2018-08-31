import logging

from IasCdb.SqlFile import SqlFile

class SqlRunner(object):
    '''
    Runs SQL scripts and statements.

    By default, statements and script uses the crusor
    passed in the constructor
    '''

    def __init__(self, cursor):
        if cursor is None:
            raise ValueError("The cursor can't be None")

        self.cursor = cursor
        self.logger = logging.getLogger('SqlRunner')

        logging.debug("SQLRunner built")

    def __getCursor(self,alternateCursor):
        '''
        Get and return the cursor to use between the default and the pased one
        :param alternateCursor: the alternate cursor to use
        :return: the cursor to use
        '''
        if alternateCursor is None:
            return self.cursor
        else:
            return alternateCursor

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
        logging.debug("Executing SQL [%s]",cmd)
        cursorToUse.execute(cmd)
        logging.debug("SQL statement executed")
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
        logging.debug("Executing SQL script")
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
                    logging.warning("Cought (and ignored) error runnig SQL [%s]: %s",sqlStatement,str(e))

        return n

    def getColumnNames(self,tableName,alternateCursor=None):
        '''
        Get the names of the columns of the passed table

        :param tableName:
        :param alternateCursor:
        :return:
        '''
        if not tableName:
           raise ValueError("Invalid name of TABLE")

        logging.debug("Getting columns of [%s]",tableName)

        sqlCommand = "SELECT table_name FROM all_tables WHERE nvl(tablespace_name, 'no tablespace') " \
                     "NOT IN ('SYSTEM', 'SYSAUX') AND OWNER = :owner AND IOT_NAME IS NULL"
        self.executeSqlStatement(sqlCommand,alternateCursor)