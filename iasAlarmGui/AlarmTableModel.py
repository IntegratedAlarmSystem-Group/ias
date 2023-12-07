# This Python file uses the following encoding: utf-8

import logging, threading, time

from PySide6.QtCore import QAbstractTableModel
from PySide6.QtCore import Qt
from PySide6.QtWidgets import QTableView
from PySide6.QtGui import QColor

from IasKafkaUtils.KafkaValueConsumer import IasValueListener
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.IasType import IASType
from IasBasicTypes.Priority import Priority
from IasBasicTypes.AlarmState import AlarmState
from IasBasicTypes.Alarm import Alarm



class AlarmTableModel(QAbstractTableModel, IasValueListener):
    """
    The table model of alarms

    Alarms are collected for 1 second then they are flushed in table
    to avoid refreshing too often.
    """
    def __init__(self, view: QTableView):
        """
        Constructor
        """
        super().__init__()

        # The table view widget that display the alarms
        self.view = view

        # The period to update the table
        self.timeout = 1

        # The mutex to protect critical section
        self.lock = threading.RLock()

        # The alarms received from the BSDB but not yet added to the model
        # i.e. not yet displaied in the table
        self.received_alarms: list[IasValue] = []


        # The alarms to display in the table
        # one alarm in one row
        #
        # the widget gets the value of teh cells from this variable
        # in self.data
        self.alarms: list[IasValue] = []

        # the header of the col in the table
        self.header = [ "State", "Priority", "Identifier" ]

        # The thread that update the table
        self.thread = threading.Thread(daemon=True, target=self.flush_alarms)
        self.thread.start()

        # Set to True when the GUI is paused i.e. the table must not be update
        # and the alarms saved in a temporary buffer until resumed
        self.pasued = False

        # The temporary buffer to store alarms when paused
        self.paused_buffer: list[IasValue] = []

    def get_priority(self, ias_value: IasValue) -> Priority:
        """
        returns:
            The priority of the alarm
        """
        alarm=Alarm.fromString(ias_value.value)
        return alarm.priority

    def get_state(self, ias_value: IasValue) -> AlarmState:
        """
        returns:
            The priority of the alarm
        """
        alarm=Alarm.fromString(ias_value.value)
        return alarm.alarmState

    def data(self, index, role=Qt.ItemDataRole.DisplayRole):
        if role == Qt.ItemDataRole.DisplayRole:

            # See below for the nested-list data structure.
            # .row() indexes into the outer list,
            # .column() indexes into the sub-list
            ias_value_in_row = self.alarms[index.row()]
            if index.column()==0:
                return str(self.get_state(ias_value_in_row))
            elif index.column()==1:
                return str(self.get_priority(ias_value_in_row))
            else:
                return ias_value_in_row.id
        elif role == Qt.BackgroundRole:
            ias_value_in_row = self.alarms[index.row()]
            alarmState = self.get_state(ias_value_in_row)
            # If the alarm is not set then colors do not depend on priority
            if not alarmState.is_set():
                if alarmState.is_acked():
                    # Acked and clear
                    return QColor.fromString('green')
                else:
                    # Clear but not yet acked
                    return QColor.fromString('darkseagreen')
            priority = self.get_priority(ias_value_in_row)
            if priority == Priority.CRITICAL:
                return QColor.fromString('darkred')
            elif priority==Priority.HIGH:
                return QColor.fromString('red')
            elif priority==Priority.MEDIUM:
                return QColor.fromString('orange')
            elif priority==Priority.LOW:
                return QColor.fromString('yellow')
            else:
                return QColor('white')
        elif role == Qt.TextAlignmentRole:
            return Qt.AlignVCenter + Qt.AlignHCenter
        elif role == Qt.ForegroundRole:
            return QColor.fromString('black')

    def rowCount(self, index):
        # The length of the outer list.
        return len(self.alarms)

    def columnCount(self, index):
        # The following takes the first sub-list, and returns
        # the length (only works if all rows are an equal length)
        return len(self.header)

    def headerData(self, section, orientation, role=Qt.ItemDataRole.DisplayRole):
        if role == Qt.ItemDataRole.DisplayRole and orientation==Qt.Orientation.Horizontal:
            return self.header[section]

    def iasValueReceived(self, iasValue):
        """
        Gets alarms from Kafka and add them the model
        """
        # Discard non alarms IasValues
        if not iasValue or iasValue.valueType!=IASType.ALARM:
            print(f"IasValue {iasValue.id} of type {iasValue.id,str(iasValue.value)} discarded")
            return
        # Add the alarm to the model
        with self.lock:
            if self.pasued:
                self.add_received_alarm(iasValue, self.paused_buffer)
            else:
                self.add_received_alarm(iasValue, self.received_alarms)

    def add_received_alarm(self, alarm: IasValue, alarm_list: list[IasValue]) -> None:
        """
        Adds the alarm to the list, replacing an old alarm
        if it is already in the list.

        Depending on the apassed list, this function adds the alarm to
        the alarm displayed by the view or to the list of alarms buffered when
        the view is paused

        Args:
            alarm: the alarm to add
            alarm_list: the list to add the alarm to
        """
        with self.lock:
            for index, ias_value in enumerate(alarm_list):
                if ias_value.id==alarm.id:
                    # alarm already in the list
                    alarm_list[index]=alarm
                    return
            alarm_list.append(alarm)

    def setData(self,index, value, role=Qt.EditRole):
        if role==Qt.EditRole:
            if index.isValid() and 0 <= index.row() < len(self.alarms):
                self.dataChanged.emit(index,index)
                return True
        return False

    def get_index_of_alarm(self, alarm: IasValue) -> int:
        """
        Get and return the position of an alarm in the list.
        An alarm is in the list if the list contains an alarm with the
        same fullRunningID

        Args:
            alarm: the alarm to which we want to get the position in th elist
        Returns:
            the index of the alarm in the list or -1 if the alarm is not
            in the list
        """
        id = alarm.fullRunningId
        for index, ias_value in enumerate(self.alarms):
            if ias_value.fullRunningId==id:
                return index
        return -1

    def flush_alarms(self) -> None:
        """
        Flush the alarms received in the last period in self.alarms
        so that they are displaied in the table

        New alarms are inserted in the head so they move on top of the table
        """
        print("Table updated thread started")
        while True:
            time.sleep(self.timeout)
            with self.lock:
                if len(self.received_alarms)==0:
                    continue
                print(f"Adding {len(self.received_alarms)} alarms to the table")
                for alarm in self.received_alarms:
                    pos = self.get_index_of_alarm(alarm)
                    if pos==-1:
                        # Alarm not already in the list: inserted in the head of the list
                        self.alarms.insert(0,alarm)
                        self.layoutChanged.emit()
                    else:
                        self.alarms[pos]=alarm
                        self.setData(self.createIndex(pos, 0),alarm)
                        self.setData(self.createIndex(pos, 1),alarm)
                        self.setData(self.createIndex(pos, 2),alarm)
                self.received_alarms.clear()
                print(f"{len(self.alarms)} alarms in table")

    def pause(self, enable: bool) -> None:
        """
        Pause resume the update of the table

        Args:
            enable: if True pause the update otherwise resume
        """
        with self.lock:
            self.pasued=enable
            if not self.paused:
                for alarm in self.paused_buffer:
                    self.add_received_alarm(alarm, self.received_alarms)
                self.paused_buffer = []






