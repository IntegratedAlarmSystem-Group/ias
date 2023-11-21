# This Python file uses the following encoding: utf-8
import sys, os, logging

from PySide6.QtCore import Slot, QCommandLineOption, QCommandLineParser
from PySide6.QtWidgets import QApplication, QMainWindow


# Important:
# You need to run the following command to generate the ui_form.py file
#     pyside6-uic form.ui -o ui_form.py, or
#     pyside2-uic form.ui -o ui_form.py
from ui_form import Ui_AlarmGui
from connect_to_ias_dlg import ConnectToIasDlg

class MainWindow(QMainWindow, Ui_AlarmGui):
    def __init__(self, ias_cdb, parent=None):
        super().__init__(parent)
        self.ui = Ui_AlarmGui()
        self.ui.setupUi(self)
        self.ias_cdb=ias_cdb
        # the dialog to connect to the IAS
        self.connectDlg = None

    @Slot()
    def on_action_Connect_triggered(self):
        self.connectDlg = ConnectToIasDlg(self.ias_cdb, self)
        self.connectDlg.open()
        self.connectDlg.finished.connect(self.on_ConnectDialog_finished)

    # @Slot()
    def on_ConnectDialog_finished(self):
        print("on_ConnectDialog_finished")
        dlg_ret_code = self.connectDlg.result()
        if dlg_ret_code==1:
            # The user pressed the Ok button ==> Connect!
            print("URL",self.connectDlg.getBrokers())
        self.connectDlg = None

    @Slot()
    def on_action_Pause_toggled(self):
        print(f"Pause/Resume check status {self.ui.action_Pause.isChecked()}")

def parse(app):
    """
    Parse the command line arguments
    """
    parser = QCommandLineParser()
    parser.addHelpOption()
    parser.addVersionOption()

    cdb_option = QCommandLineOption(
            ["c", "jcdb"],
            "The parent folder of the IAS CDB",
            "ias_cdb"
        )
    parser.addOption(cdb_option)
    parser.process(app)

    if parser.isSet(cdb_option):
        return parser.value(cdb_option)
    else:
        return None


if __name__ == "__main__":
    logging.debug("IAS Alarm GUI started")
    try:
        os.environ["IAS_ROOT"]
    except:
        logging.error("Environment not set: IAS_ROOT undefined!")
        sys.exit(1)
    app = QApplication(sys.argv)
    app.setApplicationName("IasAlarmGui")
    app.setApplicationVersion("1.0")
    cdb_parent_folder=parse(app)
    logging.info("IAS CDB parent path is %s",cdb_parent_folder)
    widget = MainWindow(ias_cdb=cdb_parent_folder)
    widget.show()
    sys.exit(app.exec())
