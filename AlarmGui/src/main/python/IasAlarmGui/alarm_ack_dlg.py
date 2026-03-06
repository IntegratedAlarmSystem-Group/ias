"""
The dialog to ACK an alarm
"""
from PySide6.QtWidgets import QDialog
from PySide6.QtWidgets import QLabel
from PySide6.QtGui import QIcon

from IasAlarmGui.ui_ack_alarm_dlg import Ui_AckAlarmDlg

class AckAlarmDlg(QDialog, Ui_AckAlarmDlg):

      def __init__(self, parent=None):
         """
         Constructor
         """
         super().__init__(parent)
         self.ui = Ui_AckAlarmDlg()
         self.ui.setupUi(self)

         # Adds the icon to the ACK button
         ack_icon_lbl = QLabel()
         # ack_pixmap = QPixmap(":/icons/tick.png").scaled(16, 16)  

         # ack_icon_lbl.setPixmap(ack_pixmap)
         self.ui.ack_btn.setIcon(QIcon(":/icons/tick.png"))
