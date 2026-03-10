"""
The dialog to ACK an alarm
"""
import asyncio
import logging
from PySide6.QtWidgets import QDialog
from PySide6.QtWidgets import QLabel
from PySide6.QtGui import QIcon

from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Identifier import Identifier
from IasBasicTypes.IdentifierType import IdentifierType
from IasBasicTypes.Alarm import Alarm
from IasExtras.AlarmAck import AlarmAck


from IasAlarmGui.ui_ack_alarm_dlg import Ui_AckAlarmDlg

class AckAlarmDlg(QDialog, Ui_AckAlarmDlg):

     def __init__(self, ias_value: IasValue, alarm_ack: AlarmAck, parent=None):
         """
         Constructor

         Params:
            ias_value: the IasValue (Alarm) to ACK
            alarm_ack: The AlarmAck to ACK the alarm by sending a command to the supervisor
         """
         super().__init__(parent)
         if not alarm_ack:
              raise ValueError("The AlarmAck can't be None")
         if not ias_value:
              raise ValueError("The IasValue can't be None")
         self.logger= logging.getLogger(__name__)
         self.ias_value: IasValue = ias_value
         self.deps_values = self.ias_value.dependentsFullRuningIds
         self.alarm = Alarm.fromString(ias_value.value)
         self.alarm_ack: AlarmAck = alarm_ack

         ias_value_identifier = Identifier.from_string(self.ias_value.fullRunningId)
         self.supervisor_id = ias_value_identifier.get_id_of_type(IdentifierType.SUPERVISOR)
         self.ui = Ui_AckAlarmDlg()
         self.ui.setupUi(self)

         self.ui.ack_btn.setIcon(QIcon(":/icons/tick.png"))

         self.ui.comment_te.textChanged.connect(self.ratio_widgets)

         self.ui.ack_btn.clicked.connect(self.on_ack_btn_clicked)

         self.fill_fields()
      
     def ratio_widgets(self):
          """
          Enable/disable the UI elements depending on the content of the widgets
          """
          self.ui.ack_btn.setEnabled(len(self.ui.comment_te.toPlainText())>0)
          self.ui.dep_alarms_list.setEnabled(self.ui.dep_alarms_list.count()>0)

     def fill_fields(self):
          self.ui.alarm_id_te.setText(self.ias_value.id)
          if not self.deps_values:
               self.ui.dep_alarms_list.clear()
          else:
               ids = [Identifier.from_string(id).get_id_of_type(IdentifierType.IASIO) for id in self.deps_values]
               self.ui.dep_alarms_list.addItems(ids)
          self.ui.comment_te.clear()

     async def ack_thread(self, user_comment:str):
          """
          ACK the alarm
          
          Params:
               user_comment: the comment taken from the text field
          """
          self.logger.info("Acknowledging", self.ias_value.fullRunningId)
          self.alarm_ack.ack(
               alarm_id=self.ias_value.fullRunningId,
               supervisor_id=self.supervisor_id,
               comment=user_comment,
               timeout=0.0)
          print("DONE3")

     def on_ack_btn_clicked(self):
          print("HERE")
          comment = self.ui.comment_te.toPlainText()
          asyncio.run(self.ack_thread(comment))


