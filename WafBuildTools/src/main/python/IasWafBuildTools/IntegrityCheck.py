"""
Integrity checks for building the IAS
"""
from waflib import Logs

def checkIasRoot(conf):
    """
    Check if the IAS_ROOT is defined and points to an existing folder
    :param conf: WAF configuration
    :return: False in case of Error, True otherwise
    """
    Logs.debug("IntegrityCheck: Checking $IAS_ROOT")
    conf.add_os_flags('IAS_ROOT')
    if conf.env.IAS_ROOT is None or not conf.env.IAS_ROOT:
        conf.fatal('IAS_ROOT not defined')

    folder = conf.env.IAS_ROOT[0]
    if folder:
        import os.path
        # Check if the path pointed by IAS_ROOT exists and is a folder
        if not os.path.exists(folder):
            Logs.info("IntegrityCheck: %s does not exists. Creating...", folder)
            os.mkdir(folder)

        if not os.path.isdir(folder):
            conf.fatal("IAS_ROOT "+folder+" is NOT a folder")
    else:
        conf.fatal("Invalid IAS_ROOT definition")


def checkTools(conf):
    """
    Check if all the tools for building are installed

    :param conf: Waf configuration
    :return:
    """
    conf.find_program('tar', var='TAR')
    conf.find_program('unzip', var='UNZIP')
    conf.find_program('scalac', var='SCALAC', mandatory=True)
    conf.find_program('javac', var='JAVAC', mandatory=True)

def checkIntegrity(conf):
    """
    Integrity checks: entry point that delegates to the other methods

    :param conf: WAF conf
    :return:
    """
    checkIasRoot(conf)
    checkTools(conf)
