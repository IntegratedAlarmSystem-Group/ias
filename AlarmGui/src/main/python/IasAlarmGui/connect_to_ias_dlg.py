# This Python file uses the following encoding: utf-8
import re

from PySide6.QtCore import Slot
from PySide6.QtWidgets import QDialog
from PySide6.QtWidgets import QDialogButtonBox
from PySide6.QtWidgets import QErrorMessage, QFileDialog


from IasAlarmGui.ui_connect_dialog import Ui_ConnectToIas

from IasCdb.CdbReader import CdbReader

class ConnectToIasDlg(QDialog,Ui_ConnectToIas):
    def __init__(self, bsdb_url: str|None,parent=None):
        """
        Constructor

        Params:
            bsdb_url: The URL of the kafka brokers in format server:port, server:
                      or None if not available
        """
        super().__init__(parent)
        self.ui = Ui_ConnectToIas()
        self.ui.setupUi(self)
        self.okBtn = self.ui.buttonBox.button(QDialogButtonBox.Ok)

        self._bsdb_url = bsdb_url
        self.ui.lineEdit.setText(bsdb_url if bsdb_url else "")
        self.brokers: str|None = self._bsdb_url

        # The regular expression to check the correctness of the
        # connection string (i.e. the BSDB URL)
        self.pattern = r'^[a-zA-Z0-9.-]+:\d+$'

        self.setupPanelWidgets(self._bsdb_url)

    def setupPanelWidgets(self, brokers: str|None) -> None:
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

    def checkServersString(self, txt: str) -> bool:
        """
        Check if the txt is in the form "server:port, server:port..."
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

        Params:
            ias_cdb The parent folder of the CDB
        Returns:
            str: the kafka brokers if the user pressed the Ok button
                 None otherwise
        """
        return self.brokers
    
    def readUrlFromCdb(self, ias_cdb) -> str|None:
        """
        Read and return the URL from the CDB

        Returns:
            The URL of the kafka brokers read from the CDB
        Raises:
            ValueError: if the CDB cannot be read
        """
        if not ias_cdb:
            raise ValueError("IAS CDB path is not available")
        reader = CdbReader(ias_cdb)
        ias = reader.get_ias()
        return ias.bsdb_url

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

        # Reading the ias.yaml|json from CDB could be slow: think to do it in a thread
        try:
            brokers = self.readUrlFromCdb(self.ias_cdb)
        except ValueError as ve:
            err_dlg = QErrorMessage(self)
            err_dlg.showMessage(f"Cannot read the CDB from {self.ias_cdb}: {ve}")
            return
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
