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
        widgetText =  QLabel(f"<B>{key}</B>: {value}")
        widgetLayout = QHBoxLayout()
        widgetLayout.addWidget(widgetText)
        widgetLayout.setSizeConstraint(QLayout.SetFixedSize)
        widget.setLayout(widgetLayout)
        widgitItem.setSizeHint(widget.sizeHint())
        self.details_list.addItem(widgitItem)
        self.details_list.setItemWidget(widgitItem, widget)

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
        self.format_and_add("FRID", ias_value.fullRunningId)
