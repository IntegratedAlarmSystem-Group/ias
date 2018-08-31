class SqlFile(object):
    '''
    The object to read SQL script for a file

    SqlFile
    * remove inline comments (starting with --)
    * remove lulti line comments /*...*/
    * assumes that ';' appears only as separator of SQL commands
    '''

    def __init__(self, fileName):
        if not fileName:
            raise ValueError("Invalid file name")
        self.fileName = fileName

        with open(fileName) as f:
            lines = f.readlines()

        # Clean the lines
        lines = self.__cleanFile(lines)

        print("Cleaned file:")
        for line in lines:
            print('[',line,']')

        self.__sqlStatements = self.__buildSQlStatements(lines)
        print("SQL statements:")
        for line in self.__sqlStatements:
            print('['+line+']')

    def getSQLStatements(self):
        '''
        :return: The SQL statements read from the SQL file
        '''
        return self.__sqlStatements

    def __buildSQlStatements(self,lines):
        '''
        Parse the lines to build valid SQL commands

        SQL commands terminate with ';' but can split in more lines.

        :param lines: the (cleaned0 lines read from the file
        :return: the SQL cstatements
        '''
        ret = []
        temp = ''
        for line in lines:
            temp = temp+' '+line
        temp = temp.split(';')

        for line in temp:
            t = line.strip()
            if t:
                ret.append(t)

        return ret


    def __cleanFile(self, lines):
        '''
        Clean the lines of the SQL file from comments
        :param lines: the lines of the SQL file
        :return: the lines of the SQL file without comments
        '''
        ret = self.__removeMultilineComments(lines)
        print("Cleaned file from multine comments:")
        for line in lines:
            print(line)
        ret = self.__removeInlineComments(ret)
        return ret

    def __removeMultilineComments(self,lines):
        '''
        Remove multi-line comments (*/...*/) from the
        passed lines

        :param lines: the lines to remove comments from
        :return: the lines of the file without comments
        '''
        if not lines:
            raise ValueError("Invalid lines to clean from inline comments")

        commentPrefix="/*"
        commentSuffix="*/"
        commentFound = False

        ret =[]

        for line in lines:
            line = line.strip()
            if not line:
                continue
            if not commentFound:
                # Is there a comment in this line
                if not commentPrefix in line:
                    ret.append(line.strip())
                else:
                    commentFound=True
                    pos = line.index(commentPrefix)

                    if commentSuffix in line:
                        # Is there also a close comment in the same line
                        commentFound = False
                        endPos = line.index(commentSuffix)
                        newline = (line[:pos]+line[endPos+2:]).strip()
                        if newline:
                            ret.append(newline)
                    else:
                        # No close comment  */ in the same line
                        newLine = (line[:pos]).strip()
                        if newLine:
                            ret.append(newLine)
            else:
                if commentSuffix in line:
                    newLine = (line[:line.index(commentSuffix)]).strip()
                    commentFound = False
                    if newLine:
                        ret.append(newLine)
        return ret

    def __removeInlineComments(self,lines):
        '''
        Remove the inline comments (--) from the lines
        of the SQL file

        :param lines: the lines to clean from comments
        :return: the cleaned line (can be empty)
        '''
        if not lines:
            raise ValueError("Invalid lines to clean from inline comments")

        ret = []

        for line in lines:
            line = line.strip()
            if not line:
                # remove empty lines
                continue
            if not "--" in line:
                ret.append(line.strip())
            else:
                pos = line.index('--')
                ret.append(line[:pos].strip())
        return ret
