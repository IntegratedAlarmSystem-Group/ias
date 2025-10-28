#! /usr/bin/env python3

import sys, logging, string, random, threading

from PySide6.QtCore import Slot, QCommandLineOption, QCommandLineParser, QTimer
from PySide6.QtWidgets import QApplication, QMainWindow, QLabel
from PySide6.QtGui import QPixmap
# Important:
# You need to run the following command to generate the ui_form.py file
#     pyside6-uic form.ui -o ui_form.py, or
#     pyside2-uic form.ui -o ui_form.py
from IasAlarmGui.ui_alarm_gui import Ui_AlarmGui
from IasAlarmGui.AlarmTableModel import AlarmTableModel
from IasAlarmGui.connect_to_ias_dlg import ConnectToIasDlg
from IasAlarmGui.about_dlg import AboutDlg

from IasKafkaUtils.KafkaValueConsumer import KafkaValueConsumer
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

from IasBasicTypes.IasValue import IasValue
from IASTools.DefaultPaths import DefaultPaths
from IASLogging.logConf import Log

from IasAlarmGui.AlarmDetailsHelper import AlarmDetailsHelper
from IasAlarmGui.config import Config

class MainWindow(QMainWindow, Ui_AlarmGui):
    def __init__(self, bsdb_url: str|None, parent=None):
        """
        Constructor

        Params:
            bsdb_url: The URL of the kafka brokers in format server:port, server:port...
                      or None if not available
        """
        super().__init__(parent)
        self.ui = Ui_AlarmGui()
        self.ui.setupUi(self)
        self.bsdb_url = bsdb_url

        # The logger
        self.logger = Log.getLogger(__name__)

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

        # Adds the icon with the beating heart in the status bar
        self.status_icon_lbl = QLabel()
        self.heart_ok = QPixmap(":/icons/heart.png").scaled(16, 16)  
        self.heart_nop = QPixmap(":/icons/heart-empty.png").scaled(16, 16)
        self.status_icon_lbl.setPixmap(self.heart_nop)
        self.showing_ok_icon = False # To blink the icon
        self.ui.statusbar.addPermanentWidget(self.status_icon_lbl)

        
        # The timer to change icon every 2 seconds
        self.timer = QTimer(self)
        self.timer.timeout.connect(self.update_icon)
        self.timer.start(1000)

    def update_icon(self):
        """
        Update the icon in the status bar according to the
        status of the kafka consumer
        """
        if self.value_consumer is not None and self.value_consumer.isSubscribed():
            # Getting events
            if self.showing_ok_icon:
                self.status_icon_lbl.setPixmap( self.heart_nop)
                self.showing_ok_icon = False
            else:
                self.status_icon_lbl.setPixmap( self.heart_ok)
                self.showing_ok_icon = True
            self.status_icon_lbl.setToolTip("Connected to BSDB")
        else:
            # Not getting events
            self.status_icon_lbl.setPixmap( self.heart_nop)
            self.status_icon_lbl.setToolTip("Not connected to BSDB")
            self.showing_ok_icon = False

    @Slot()
    def on_action_Disconnect_triggered(self):
        print("Disconnecting from the BSDB...")
        # Start the thread to connect
        disconnect_thread = threading.Thread(target=self.disconnectFromIas)
        disconnect_thread.start()

    @Slot()
    def on_action_Connect_triggered(self):
        self.connectDlg = ConnectToIasDlg(self.bsdb_url, self)
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
            self.ui.action_Disconnect.setEnabled(True)
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
            self.ui.action_Disconnect.setEnabled(True)
        except Exception as e:
            logging.error("Error connecting to the BSDB: %s",str(e))
            self.ui.action_Connect.setEnabled(True)
            self.ui.action_Disconnect.setEnabled(False)

    def disconnectFromIas(self) -> None:
        """
        Disconnect from the IAS
        """
        if self.value_consumer is not None:
            logging.info("Disconnecting from the BSDB...")
            self.value_consumer.close()
            self.value_consumer = None
        self.ui.action_Connect.setEnabled(True)
        self.ui.action_Disconnect.setEnabled(False)

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

def parse(app) -> dict[str, str]:
    """
    Parse the command line arguments

    Returns:
        A dictionary with the params set in the command line:
            - 'ias_cdb': if the parent folder of the IAS CDB if set in the command line
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

    bsdb_option = QCommandLineOption(
            ["b", "bsdburl"],
            "The URL of the BSDB (kafka) in format server:port, server:port...",
            "bsdb_url"
        )
    parser.addOption(bsdb_option)

    do_not_autoconnect = QCommandLineOption(
        [ "n", "donotconnect"],
        "Do not connect automatically to the BSDB at startup"
    )
    parser.addOption(do_not_autoconnect)

    parser.process(app)

    ret = {}
    if parser.isSet(cdb_option):
        ret["ias_cdb"] = parser.value(cdb_option)
    if parser.isSet(bsdb_option):
        ret["bsdb_url"] = parser.value(bsdb_option)
    if parser.isSet(do_not_autoconnect):
        ret["connect_to_bsdb"] = False
    else:
        ret["connect_to_bsdb"] = True
    
    return ret

if __name__ == "__main__":
    logger = Log.getLogger(__file__)
    logger.debug("IAS Alarm GUI started")
    if not DefaultPaths.check_ias_folders():
        logger.error("IAS folders not set!")
        sys.exit(1)
    app = QApplication(sys.argv)
    app.setApplicationName("IasAlarmGui")
    app.setApplicationVersion("1.0")

    cmd_line_args = parse(app)

    cdb_parent_folder= cmd_line_args.get("ias_cdb", None)
    logger.debug("IAS CDB parent path from command line is %s",cdb_parent_folder)
    bsdb_url_from_cdmline = cmd_line_args.get("bsdb_url", None)
    logger.debug("BSDB URL from command line is %s",bsdb_url_from_cdmline)

    config = Config(ias_cdb_cmd_line=cdb_parent_folder)
    bsdb = config.get_bsdb_url(
        url_from_cmd_line=bsdb_url_from_cdmline,
        default_bsdb=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
    
    connect_to_bsdb = cmd_line_args.get("connect_to_bsdb")
    
    widget = MainWindow(bsdb_url=bsdb)
    widget.show()
    if connect_to_bsdb and bsdb is not None:
        # Start the thread to connect
        connect_thread = threading.Thread(target=widget.connectToIas, args=(bsdb,))
        connect_thread.start()
    sys.exit(app.exec())
