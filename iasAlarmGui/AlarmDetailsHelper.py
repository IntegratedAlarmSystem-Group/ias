# This Python file uses the following encoding: utf-8
from PySide6.QtWidgets import QListWidget, QListWidgetItem, QLabel, QWidget, QHBoxLayout, QLayout
from PySide6.QtCore import Qt

from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.IasValue import IasValue

class AlarmDetailsHelper:
    def __init__(self, alarm_list: QListWidget):
        self. details_list = alarm_list

    def format_and_add(self, key: str, value: str)-> None:
        widgitItem = QListWidgetItem()
        widget = QWidget()
        if value is not None:
            widgetText =  QLabel(f"<B>{key}</B>: {value}")
        else:
            widgetText =  QLabel(f"<B>{key}</B>: -")
        widgetText.setWordWrap(True)
        widgetLayout = QHBoxLayout()
        widgetLayout.addWidget(widgetText)
        widgetLayout.setSizeConstraint(QLayout.SetFixedSize)
        widget.setLayout(widgetLayout)
        widgitItem.setSizeHint(widget.sizeHint())
        self.details_list.addItem(widgitItem)
        self.details_list.setItemWidget(widgitItem, widget)

    def format_frid(self, frid: str)->str:
        return frid.replace("@", " @ ")

    def format_frids(self, frids: list[str])->str:
        ret = "<UL>"
        for frid in frids:
            ret+=f"<LI>{self.format_frid(frid)}</LI>"
        return "</UL>"+ret

    def update(self, ias_value: IasValue) -> None:
        """
        Fills the details in the right side of the GUI
        with the details of the IasValue

        Args:
            ias_value: the IasValue whose fields will be shown in the details
        """
        self.details_list.clear()
        alarm=Alarm.fromString(ias_value.value)

        self.format_and_add("ID", ias_value.id)
        self.format_and_add("Set", alarm.is_set())
        self.format_and_add("Ack", alarm.is_acked())
        self.format_and_add("Priority", alarm.priority)
        self.format_and_add("Validity", ias_value.iasValidityStr)
        self.format_and_add("Mode", ias_value.modeStr)
        self.format_and_add("Type", ias_value.valueTypeStr)
        self.format_and_add("FRID", self.format_frid(ias_value.fullRunningId))
        self.format_and_add("Dependencies", self.format_frids(ias_value.dependentsFullRuningIds))
        self.format_and_add("Plugin prod. stamp", ias_value.pluginProductionTStampStr)
        self.format_and_add("Sent to Converter tstamp", ias_value.sentToConverterTStampStr)
        self.format_and_add("Recv from plugin tstamp", ias_value.receivedFromPluginTStampStr)
        self.format_and_add("Converted tstamp", ias_value.convertedProductionTStampStr)
        self.format_and_add("Converted tstamp", ias_value.convertedProductionTStampStr)
        self.format_and_add("Sent to BSDB tstamp", ias_value.sentToBsdbTStampStr)
        self.format_and_add("Read from BSDB tstamp", ias_value.readFromBsdbTStampStr)
        self.format_and_add("DASU prod. tstamp", ias_value.dasuProductionTStampStr)
