#! /usr/bin/env python3

import sys, os, logging, string, random, threading

from PySide6.QtCore import Slot, QCommandLineOption, QCommandLineParser
from PySide6.QtWidgets import QApplication, QMainWindow

# Important:
# You need to run the following command to generate the ui_form.py file
#     pyside6-uic form.ui -o ui_form.py, or
#     pyside2-uic form.ui -o ui_form.py
from ui_alarm_gui import Ui_AlarmGui
from AlarmTableModel import AlarmTableModel
from connect_to_ias_dlg import ConnectToIasDlg
from about_dlg import AboutDlg

from IasKafkaUtils.KafkaValueConsumer import KafkaValueConsumer
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

from IasBasicTypes.IasValue import IasValue
from AlarmDetailsHelper import AlarmDetailsHelper

class MainWindow(QMainWindow, Ui_AlarmGui):
    def __init__(self, ias_cdb, parent=None):
        super().__init__(parent)
        self.ui = Ui_AlarmGui()
        self.ui.setupUi(self)
        self.ias_cdb=ias_cdb

        self.alarm_details = AlarmDetailsHelper(self.ui.alarmDetailsTE)

        self.tableModel = AlarmTableModel(self.ui.alarmTable)
        self.ui.alarmTable.setModel(self.tableModel)
        self.ui.alarmTable.selectionModel().selectionChanged.connect(self.onTableSelectionChanged)
        self.ui.alarmTable.horizontalHeader().setStretchLastSection(True)

        self.ui.splitter.setSizes([250,100])
        self.ui.alarmDetailsTE.setText("Alarm details")

        # The consumer of alarms. The listener is the table model
        self.value_consumer: KafkaValueConsumer = None
        # The group must be unique to get all the alarms so we append a random part
        chars=string.ascii_uppercase + string.digits
        self.group_id: str  = "iasAlarmGui-".join(random.choice(chars) for _ in range(5))
        self.client_id: str = "iasAlarmGui"

        # the dialog to connect to the IAS
        self.connectDlg = None

    @Slot()
    def on_action_Connect_triggered(self):
        self.connectDlg = ConnectToIasDlg(self.ias_cdb, self)
        self.connectDlg.open()
        self.connectDlg.finished.connect(self.on_ConnectDialog_finished)

    @Slot()
    def on_action_About_triggered(self):
        self.aboutDlg = AboutDlg(self)
        self.aboutDlg.open()

    # @Slot()
    def on_ConnectDialog_finished(self):
        dlg_ret_code = self.connectDlg.result()
        if dlg_ret_code==1:
            # The user pressed the Ok button ==> Connect!
            self.ui.action_Connect.setEnabled(False)
            brokers = self.connectDlg.getBrokers()
            assert brokers is not None, "The dialog should not return an empty broker user presses Ok"
            logging.info("Connecting to the BSDB %s",self.connectDlg.getBrokers())
            # Start the thread to connect
            connect_thread = threading.Thread(target=self.connectToIas, args=(brokers,))
            connect_thread.start()
        self.connectDlg = None

    @Slot()
    def on_action_Pause_toggled(self):
        print(f"Pause/Resume check status {self.ui.action_Pause.isChecked()}")
        self.tableModel.pause(self.ui.action_Pause.isChecked())

    @Slot()
    def on_action_Remove_cleared_toggled(self):
        print(f"Auto remove cleared {self.ui.action_Remove_cleared.isChecked()}")
        self.tableModel.remove_cleared(self.ui.action_Remove_cleared.isChecked())

    def connectToIas(self, bsdb_brokers: str) -> None:
        """
        Connect to the IAS passing the table model as listener

        This function runs in a thread
        """
        try:
            logging.info("Building the value consumer with client id=%s and group_id=%s", self.client_id, self.group_id)
            self.value_consumer = KafkaValueConsumer(
                self.tableModel,
                bsdb_brokers,
                IasKafkaHelper.topics['core'],
                self.client_id,
                self.group_id)
            logging.info("Starting to get alarms from the BSDB...")
            self.value_consumer.start()
            self.ui.action_Connect.setDisabled(True)
        except Exception as e:
            logging.error("Error connecting to the BSDB: %s",str(e))
        self.ui.action_Connect.setEnabled(True)

    def onTableSelectionChanged(self, selected, deselected):
        """
        The user seleted one row of the table: fills the
        details in the right side of the GUI
        """
        for index in self.ui.alarmTable.selectionModel().selectedRows():
            ias_value = self.tableModel.get_row_content(index.row())
            self.fill_details(ias_value)

    def fill_details(self, ias_value: IasValue)-> None:
        """
        Fills the details in the right side of the GUI
        with the details of the IasValue

        Args:
            ias_value: the IasValue whose fields will be shown in the details
        """
        self.alarm_details.update(ias_value)

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
