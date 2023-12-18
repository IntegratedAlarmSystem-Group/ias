package org.eso.ias.build.plugin

import java.io.File as JavaFile
import java.lang.ProcessBuilder

/**
 * GuiBuilder takes care of compiling PySide6 GUI source files into
 * pyton code
 * 
 * GUI sources are in src/main/gui/PkgName/: 
 * the class finds and builds the GUI files
 */
class GuiBuilder(val destFolder: String) {
    val guiFolder = "src/main/gui"
    val uiExt="ui"
    val resExt = "qrc"
    val packages = JavaFile(guiFolder).walk().filter { file -> file.isDirectory && file.getPath()!=guiFolder}

    val uiBuilder = "pyside6-uic"
    val resBuilder = "pyside6-rcc"

    fun build() {
        // Iterate over the folders (packages) and build the GUI files
        for (pkg in packages) {
            val uiFilesToBuild = JavaFile(pkg.getPath()).walk().filter{ file -> file.isFile && file.extension==uiExt}
            val resFilesToBuild = JavaFile(pkg.getPath()).walk().filter{ file -> file.isFile && file.extension==resExt}

            for (ui in uiFilesToBuild) {
                build_ui(ui, pkg)
            }

            for (qrc in resFilesToBuild) {
                build_qrc(qrc, pkg)
            }
        }
    }

    // Build the ui file with pyside6-uic
    fun build_ui(ui_file: JavaFile, pkg: JavaFile) {
        val fNameNoExt = ui_file.nameWithoutExtension
        val src = pkg.getPath()+"/"+fNameNoExt+"."+uiExt
        val dest = pkg.getPath()+"/ui_"+fNameNoExt+".py"
        ProcessBuilder(uiBuilder, src, "-o", dest, " --from-imports").start().waitFor()
    }

    // Build the ui file with pyside6-uic
    fun build_qrc(qrc_file: JavaFile, pkg: JavaFile) {
        val fNameNoExt = qrc_file.nameWithoutExtension
        val src = pkg.getPath()+"/"+fNameNoExt+"."+resExt
        val dest = pkg.getPath()+"/rc_"+fNameNoExt+".py"
        ProcessBuilder(resBuilder, src, "-o", dest).start().waitFor()        
    }

}
