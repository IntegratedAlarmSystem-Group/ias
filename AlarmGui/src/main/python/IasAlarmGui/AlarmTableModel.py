# This Python file uses the following encoding: utf-8

import threading, logging
from enum import Enum

from PySide6.QtCore import QAbstractTableModel, QTimer
from PySide6.QtCore import Qt, QModelIndex
from PySide6.QtWidgets import QTableView
from PySide6.QtGui import QColor

from IasKafkaUtils.KafkaValueConsumer import IasValueListener
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.IasType import IASType
from IasBasicTypes.Priority import Priority
from IasBasicTypes.AlarmState import AlarmState
from IasBasicTypes.Alarm import Alarm

from IasAlarmGui.AlarmShelfManager import AlarmShelfManager

class TableMode(Enum):
    ACTIVE = "active"
    SHELVED = "shelved"

class AlarmTableModel(QAbstractTableModel, IasValueListener):
    """
    The table model of active and shelved alarms

    Alarms are collected for 1 second then they are flushed in table
    to avoid refreshing too often.
    Flushing of alarms is done in flush_alarms that is run by a QTimer.
    """
    def __init__(self, view: QTableView, shelf_manager: AlarmShelfManager, mode: TableMode):
        """
        Constructor
        """
        super().__init__()

        self._logger = logging.getLogger(self.__class__.__name__)

        # The table view widget that display the alarms
        self.view = view

        # The shelf manager
        self.shelf_manager = shelf_manager

        # The mode of the table model
        self.table_mode = mode

        # The mutex to protect critical section
        self.lock = threading.RLock()

        # The alarms received from the BSDB but not yet added to the model
        # i.e. not yet displaied in the table
        self.received_alarms: list[IasValue] = []


        # The alarms to display in the table
        # one alarm in one row
        #
        # The widget gets the value of the cells from this variable
        # in self.data
        self.alarms: list[IasValue] = []

        # the header of the col in the table
        self.header = [ "State", "Priority", "Identifier" ]
        if self.table_mode == TableMode.SHELVED:
            self.header.append("Remaining")

        # Set to True when the GUI is paused i.e. the table must not be update
        # and the alarms saved in a temporary buffer until resumed
        self.paused = False

        # The temporary buffer to store alarms when paused
        self.paused_buffer: list[IasValue] = []

        # True if the model automatically removes cleared alarms the table
        self.autoremove_cleared=False

        # Start the timer to update the table
        
        self.timer = QTimer()
        self.timer.setInterval(1000)
        self.timer.timeout.connect(self.flush_alarms)
        self.timer.start()


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
            elif index.column()==2:
                return ias_value_in_row.id
            else: # Remaining time for shelved mode
                    return self.shelf_manager.get_remaining_seconds(alarm_id=ias_value_in_row.id)
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

    def iasValueFromBsdb(self, iasValue: IasValue):
        """
        Gets alarms from the BSDB and add them to the model
        """
        # Discard non alarms IasValues
        if not iasValue or iasValue.valueType!=IASType.ALARM:
            return
        alarm_shelved = self.shelf_manager.is_shelved(iasValue.id)
        if (alarm_shelved and self.table_mode == TableMode.ACTIVE) or \
           (not alarm_shelved and self.table_mode == TableMode.SHELVED):
           return
        # Add the alarm to the model
        with self.lock:
            if self.paused:
                self.add_received_alarm(iasValue, self.paused_buffer)
            else:
                self.add_received_alarm(iasValue, self.received_alarms)

    def add_received_alarm(self, alarm: IasValue, alarm_list: list[IasValue]) -> None:
        """
        Adds the alarm to the list, replacing an old alarm
        if it is already in the list.

        Depending on the passed list, this function adds the alarm to
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

    def shelve(self, alarm_id: str):
        """
        Slot executed when the user shelves an alarm

        Remove the alarm from the table
        """
        self._logger.info("Shelving %s", alarm_id)
        self.remove_alarm_by_id(alarm_id=alarm_id)

    def unshelve(self, alarm_id: str):
        """
        Slot executed when the user or the time unshelve an alarm

        Remove the alarm from the table
        """
        self._logger.info("Unshelving %s", alarm_id)
        self.remove_alarm_by_id(alarm_id=alarm_id)

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
        so that they are displayed in the table

        New alarms are inserted in the head so they move on top of the table
        """
        self._logger.debug("Flushing alarms in table")
        with self.lock:
            if len(self.received_alarms)==0:
                return
            for alarm in self.received_alarms:
                pos = self.get_index_of_alarm(alarm)
                if pos==-1:
                    # Alarm not already in the list: inserted in the head of the list
                    # unless set and acked plus autoremove is set in the toolbar
                    if not (self.autoremove_cleared and self.cleared_and_acked(alarm)):
                        self.beginInsertRows(QModelIndex(), 0, 0)
                        self.alarms.insert(0,alarm)
                        self.endInsertRows()
                else:
                    # The alarm is already in the list so its state must be updated
                    # or removed if autoremove has been selected in the toolbar
                    # and the alarm is acked and clear
                    if self.autoremove_cleared and self.cleared_and_acked(alarm):
                        self.remove_rows([pos])
                    else:
                        self.alarms[pos]=alarm
                        self.setData(self.createIndex(pos, 0),alarm)
                        self.setData(self.createIndex(pos, 1),alarm)
                        self.setData(self.createIndex(pos, 2),alarm)
                        if self.table_mode == TableMode.SHELVED:
                            self.setData(self.createIndex(pos, 3), alarm)

            self.received_alarms.clear()
            if self.table_mode == TableMode.SHELVED and self.alarms:
                # This emits dataChanged for all rows' column 3 every second 
                # (since flush_alarms runs every second), causing the view to re-call data() for the "Remaining"
                # column and display the decremented counter.
                top = self.createIndex(0, 3)
                bottom = self.createIndex(len(self.alarms) - 1, 3)
                self.dataChanged.emit(top, bottom)
        self._logger.debug("Alarms flushed in the table")

    def pause(self, enable: bool) -> None:
        """
        Pause resume the update of the table

        Args:
            enable: if True pause the update otherwise resume
        """
        with self.lock:
            self.paused=enable
            if not self.paused:
                for alarm in self.paused_buffer:
                    self.add_received_alarm(alarm, self.received_alarms)
                self.paused_buffer = []

    def cleared_and_acked(self, alarm: IasValue)-> bool:
        """
        Check if the alarm is cleared and acked.
        Cleared and acked alarms must be removed from the table
        if the auto-remove has been enabled in the tool bar
        Args:
            alarm the alarm whose state must be checked
        Returns:
            True if the alarm is acked and clear, False otherwise
        """
        state=self.get_state(alarm)
        return state.is_acked() and not state.is_set()

    def remove_cleared(self, enable: bool)-> None:
        """
        Set the property to auto remove cleared alarms
        Args:
            enable: if True auto-remove of cleared alarms is enabled
                    otherwise is disabled
        """
        with self.lock:
            self.autoremove_cleared=enable
            print("Alarms in table",len(self.alarms),len(self.received_alarms))
            # index of the rows o remove
            rowsToRemove=[]
            if enable:
                for index, ias_value in enumerate(self.alarms):
                    if self.cleared_and_acked(ias_value):
                        rowsToRemove.insert(0,index)
                self.remove_rows(rowsToRemove)

    def remove_rows(self, rows: list[int])->None:
        """
        Removes the rows from the table
        Args:
            rows: the rows to remove
        """
        # Ensure the rows is a list ordered from highest index to lowest index
        rows.sort(reverse=True)
        for row in rows:
            print("Removing row",row)
            index = QModelIndex()
            self.beginRemoveRows(index, row, row)
            del self.alarms[row]
            self.endRemoveRows()

    def remove_alarm_by_id(self, alarm_id: str) -> None:
        """
        Remove an alarm from the table by its ID.
        Does nothing if the alarm is not found.
        """
        with self.lock:
            for i, ias_value in enumerate(self.alarms):
                if ias_value.id == alarm_id:
                    self.beginRemoveRows(QModelIndex(), i, i)
                    del self.alarms[i]
                    self.endRemoveRows()
                    return

    def get_row_content(self, index: int)->IasValue:
        with self.lock:
            return self.alarms[index]

    def get_active_alarms(self):
        """
        Return the number of active (set) alarms i.e. the number of the alarms
               that are SET_ACK ad SET_UNACK                
        """
        with self.lock:
            return sum(
                self.get_state(alarm).is_set()
                for alarm in self.alarms
            )
