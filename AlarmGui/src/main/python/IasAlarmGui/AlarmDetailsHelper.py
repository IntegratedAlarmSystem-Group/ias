# This Python file uses the following encoding: utf-8
from PySide6.QtWidgets import QListWidget, QListWidgetItem, QLabel, QWidget, QHBoxLayout, QLayout, QTextEdit
from PySide6.QtCore import Qt

from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.IasValue import IasValue

from IasCdb.Dao.IasioDao import IasioDao

class AlarmDetailsHelper:
    def __init__(self, alarm_list: QTextEdit, iasios: dict[str, IasioDao] | None = None):
        """
        Constructor

        Args:
            alarm_list: The QTextEdit widget for displaying details
            iasios: Optional mapping of IASIO id to IasioDao from the CDB
        """
        self.details_text = alarm_list
        self.details_text.setLineWrapMode(QTextEdit.WidgetWidth)
        self.details_text.clear()
        self.iasios: dict[str, IasioDao] | None = iasios

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
        iasio = self.iasios.get(ias_value.id) if self.iasios else None

        if iasio is not None:
            md = md + self.format_and_add("Display Tag", iasio.get_tag_to_display())

        md = md + self.format_and_add("ID", ias_value.id)
        md = md + self.format_and_add("Set", alarm.is_set())
        md = md + self.format_and_add("Ack", alarm.is_acked())
        md = md + self.format_and_add("Priority", alarm.priority)
        md = md + self.format_and_add("Validity", ias_value.iasValidityStr)
        md = md + self.format_and_add("Mode", ias_value.modeStr)
        md = md + self.format_and_add("Type", ias_value.valueTypeStr)

        if iasio is not None:
            md = md + self.format_and_add("Tag", iasio.get_tag_to_display())
            if iasio.shortDesc:
                md = md + self.format_and_add("Short Desc", iasio.shortDesc)
            if iasio.docUrl:
                md = md + self.format_and_add("Doc URL", iasio.docUrl)
            if iasio.templateId:
                md = md + self.format_and_add("Template ID", iasio.templateId)
            if iasio.canShelve:
                md = md + self.format_and_add("Can Shelve", str(iasio.canShelve))
            if iasio.autoAck:
                md = md + self.format_and_add("Auto Ack", str(iasio.autoAck))
            sound_str = iasio.sound.to_string()
            if sound_str and sound_str != "NONE":
                md = md + self.format_and_add("Sound", sound_str)
            if iasio.emails:
                md = md + self.format_and_add("Emails", iasio.emails)
        md = md + self.format_and_add("FRID", "\n"+self.format_frid(ias_value.fullRunningId))
        md = md + self.format_and_add("Dependencies", "\n"+self.format_frids(ias_value.dependentsFullRuningIds))

        self.details_text.setMarkdown(md)
