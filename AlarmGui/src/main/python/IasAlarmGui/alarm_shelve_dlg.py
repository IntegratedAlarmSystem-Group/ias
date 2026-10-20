from PySide6.QtWidgets import QDialog
from PySide6.QtCore import QTime
from IasAlarmGui.ui_shelve_alarm_dlg import Ui_ShelveAlarmDlg

class AlarmShelveDlg(QDialog, Ui_ShelveAlarmDlg):
    """
    The dialog to shelve an alarm
    """

    def __init__(self, parent=None):
        """
        Params:
            alarm_id: the ID of the alarm to shelve
        """
        super().__init__(parent)

        self.ui = Ui_ShelveAlarmDlg()
        self.ui.setupUi(self)

        self.alarm_id = ""


    def setAlarmId(self, alarm_id: str):
        self.alarm_id = alarm_id
        self.ui.alrarmid_lbl.setText(f"<H2>{self.alarm_id}</H2>")

    def getAlarmId(self) -> str:
        return self.alarm_id

    def getShelveTime(self) -> QTime:
        return self.ui.time_te.time()
    