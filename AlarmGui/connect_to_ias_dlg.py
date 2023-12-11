# This Python file uses the following encoding: utf-8
import re, logging

from PySide6.QtCore import Slot
from PySide6.QtWidgets import QDialog
from PySide6.QtWidgets import QDialogButtonBox
from PySide6.QtWidgets import QErrorMessage, QFileDialog


from ui_connect_dialog import Ui_ConnectToIas

from IasCdb.CdbReader import CdbReader

class ConnectToIasDlg(QDialog,Ui_ConnectToIas):
    def __init__(self, ias_cdb,parent=None):
        super().__init__(parent)
        self.ui = Ui_ConnectToIas()
        self.ui.setupUi(self)
        self.okBtn = self.ui.buttonBox.button(QDialogButtonBox.Ok)

        # The regular expression to check the correctness of the
        # connection string (i.e. the BSDB URL)
        self.pattern = r'^[a-zA-Z0-9.-]+:\d+$'

        self.ias_cdb=ias_cdb
        if self.ias_cdb is not None:
            url = self.readUrlFromCdb()
            self.setupPanelWidgets(url)

    def setupPanelWidgets(self, brokers: str) -> None:
        """
        Setup the content of the text field and enable/disable the Ok button
        depending on the brokers string

        Args:
            brokers: the Kafka brokkers server:pot,server:port...
        """
        if brokers is None:
            self.ui.lineEdit.clear()
            self.okBtn.setEnabled(False)
        elif self.checkServersString(brokers):
            self.ui.lineEdit.setText(brokers)
            self.okBtn.setEnabled(True)
        else:
            self.okBtn.setEnabled(False)

    def readUrlFromCdb(self) -> str|None:
        """
        Read and return the URL from the CDB

        Returns:
            The URL of the kafka brokers read from the CDB
            or None if the path to the cdb is not available
        """
        if not self.ias_cdb:
            return None
        try:
            reader = CdbReader(self.ias_cdb)
        except ValueError as e:
            self.ui.lineEdit.clear()
            self.okBtn.setEnabled(False)
            logging.error(f"Error reading CDB: {e}")
            error = QErrorMessage(self)
            error.setWindowTitle("Error reading IAS CDB")
            error.raise_()
            error.showMessage(str(e))

            return None
        ias = reader.get_ias()
        return ias.bsdb_url

    def checkServersString(self, txt: str) -> bool:
        """
        Check if the txt is in the form "server:ip, server:ip..."
        Args:
            txt: the string containing the URL of the kafka brokers
        Returns:
            True if the string is a valid connection string for kafka
            False otherwise
        """
        if not txt:
            return False
        address_list = txt.split(',')
        for address in address_list:
            if re.match(self.pattern, address.strip()) is None:
                return False
        return True

    def getBrokers(self) -> str:
        """
        Return the kafka brokers
        Returns:
            str: the kafka brokers if the user pressed the Ok button
                 None otherwise
        """
        return self.brokers

    @Slot()
    def onOkBtnClicked(self):
        self.brokers = self.ui.lineEdit.text()
        super().accept()

    @Slot()
    def onFetchBtnClicked(self):
        """
        Called when the user clicks the fecth from CDB button:
        reads the kafka brokers string from the configuration
        """
        self.ias_cdb = QFileDialog.getExistingDirectory(self, "Select CDB parent folder",
                                                        ".",
                                                        QFileDialog.ShowDirsOnly | QFileDialog.DontResolveSymlinks)

        brokers = self.readUrlFromCdb()
        self.setupPanelWidgets(brokers)

    @Slot()
    def onLineEditTxtChanged(self):
        """
        Invoked when the text in the line edit changed:
        - checks the correctness of the string
        - enable/disable the OK button
        """
        txt = self.ui.lineEdit.text()
        self.setupPanelWidgets(txt)
