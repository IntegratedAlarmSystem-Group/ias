import os.path, glob, copy
from pathlib import Path

class JavaScalaCommBuilder(object):
    '''
    A class that groups common methods for the java and scala builder
    '''

    @classmethod
    def bldDictOfJars(cls, jars):
        '''
        Build a dictionary of jar files that associates the path with the name of the jar
        FOr example the key is File.jar and the value is /full/path/File.jar
        :param jars: The list of Paths of jar files
        :return: The dictionary of jars
        '''
        ret = {}

        for j in jars:
            jStr = str(j) # Convert Path to string
            pos = jStr.rfind(os.sep)
            ret[jStr[pos+1:]] = jStr
        return ret


    @classmethod
    def buildClasspath(cls, bldFolder, iasRootFolder):
        '''
        Build the classpath by adding jars from the build folder and IAS_ROOT

        The order of jars is:
         - build/lib
         - $IAS_ROOT/lib
         - build/extTools
         - $IAS_ROOT/extTools

        Jars are ordered lexicographically for each folder.
        Only one jar with a given name is added i.e. if for example there are two files
        build/test.jar and $IAS_ROOT/test.jar only the first one is added to the classpath.

        :param bldFolder: the build folder of the module (normally 'build')
        :param iasRootFolder: The folder pointed by $IAS_ROOT
        :return: a string with the classpath to be used by scala and java
        '''
        if bldFolder is None:
            raise ValueError('Invalid None build folder')
        if iasRootFolder is None:
            raise ValueError('Invalid IAS_ROOT folder')

        if not os.path.exists(bldFolder):
            raise ValueError('Build folder %s not found' % bldFolder)
        if not os.path.exists(bldFolder):
            raise ValueError('IAS_ROOT folder %s not found' % iasRootFolder)

        build_path = Path(bldFolder)
        build_path_lib = build_path / 'lib/'
        build_path_ext = build_path / 'extTools/'
        jars_of_module = JavaScalaCommBuilder.bldDictOfJars(list(build_path_lib.glob('**/*.jar')))
        extjars_of_module = JavaScalaCommBuilder.bldDictOfJars(list(build_path_ext.glob('**/*.jar')))
        ias_root_path = Path(iasRootFolder)
        ias_root_path_lib = ias_root_path / 'lib/'
        ias_root_path_ext = ias_root_path / 'extTools/'
        jars_of_iasroot = JavaScalaCommBuilder.bldDictOfJars(list(ias_root_path_lib.glob('**/*.jar')))
        extjars_of_iasroot = JavaScalaCommBuilder.bldDictOfJars(list(ias_root_path_ext.glob('**/*.jar')))

        # Includes all the jars of the module
        accepted_jars_dict = copy.deepcopy(jars_of_module)
        # iterate over the jars of the other folders and them if not present
        set_of_jars = [jars_of_iasroot, extjars_of_module, extjars_of_iasroot]
        for jar_set in set_of_jars:
            for j in jar_set:
                if j not in accepted_jars_dict:
                    accepted_jars_dict[j] = jar_set[j]

        cp = ""
        cpSep = ':'
        for j in accepted_jars_dict.keys():
            cp = cp + accepted_jars_dict[j] + cpSep

        ret = "-cp " + cp[:len(cp)-1]
        return ret
