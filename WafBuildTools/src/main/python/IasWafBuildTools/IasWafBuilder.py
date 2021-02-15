from waflib import Logs

def install(bld):
    assert bld
    foldersToInstall = [ 'bin', 'lib', 'extTools', 'config']
    src = bld.env.BLDNODE
    dst = bld.env.PREFIX

    Logs.info("Installer: Installing files from %s ---to---> %s", src, dst)

    for folder in foldersToInstall:

        dstFolder = "%s/%s" % (dst, folder)
        srcNode = src.find_node(folder)

        if srcNode is None:
            Logs.info("%s is empty: nothing to install", folder)

        bld.install_files(
            dstFolder,
            srcNode.ant_glob("**/*"),
            relative_trick=True,
            cwd=srcNode)
