"""
Integrity checks for building the IAS
"""


def checkIasRoot(conf):
    """
    Check if the IAS_ROOT is defined and points to an existing folder
    :param conf: WAF configuration
    :return: False in case of Error, True otherwise
    """
    print("Checking $IAS_ROOT")
    conf.add_os_flags('IAS_ROOT')
    if conf.env.IAS_ROOT is None or not conf.env.IAS_ROOT:
        conf.fatal('IAS_ROOT not defined')

    folder = conf.env.IAS_ROOT[0]
    if folder:
        import os.path
        print("IAS_ROOT=", folder)
        # Check if the path pointed by IAS_ROOT exists and is a folder
        if not os.path.exists(folder):
            print(folder, "does not exists. Creating...")
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

def checkIntegrity(conf):
    """
    Integrity checks: entry point that delegates to the other methods

    :param conf: WAF conf
    :return:
    """
    checkIasRoot(conf)
    checkTools(conf)
