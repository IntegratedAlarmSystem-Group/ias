# This Python file uses the following encoding: utf-8
from PySide6.QtWidgets import QListWidget, QListWidgetItem, QLabel, QWidget, QHBoxLayout, QLayout, QTextEdit
from PySide6.QtCore import Qt

from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.IasValue import IasValue

class AlarmDetailsHelper:
    def __init__(self, alarm_list: QTextEdit):
        self.details_text = alarm_list
        self.details_text.setLineWrapMode(QTextEdit.WidgetWidth)
        self.details_text.clear()

    def format_and_add(self, key: str, value: str)-> str:
        if value is not None:
            return f"**{key}**: {value}\n\n"
        else:
            return f"**{key}**: -\n\n"


    def format_frid(self, frid: str)->str:
        return frid.replace("@", " @ ")

    def format_frids(self, frids: list[str])->str:
        ret = ""
        for frid in frids:
            ret+=f"   - {self.format_frid(frid)}\n"
        return ret

    def update(self, ias_value: IasValue) -> None:
        """
        Fills the details in the right side of the GUI
        with the details of the IasValue

        Args:
            ias_value: the IasValue whose fields will be shown in the details
        """
        self.details_text.clear()

        md = ""

        alarm=Alarm.fromString(ias_value.value)

        md = md + self.format_and_add("ID", ias_value.id)
        md = md + self.format_and_add("Set", alarm.is_set())
        md = md + self.format_and_add("Ack", alarm.is_acked())
        md = md + self.format_and_add("Priority", alarm.priority)
        md = md + self.format_and_add("Validity", ias_value.iasValidityStr)
        md = md + self.format_and_add("Mode", ias_value.modeStr)
        md = md + self.format_and_add("Type", ias_value.valueTypeStr)
        md = md + self.format_and_add("FRID", "\n"+self.format_frid(ias_value.fullRunningId))
        md = md + self.format_and_add("Dependencies", "\n"+self.format_frids(ias_value.dependentsFullRuningIds))

        self.details_text.setMarkdown(md)
