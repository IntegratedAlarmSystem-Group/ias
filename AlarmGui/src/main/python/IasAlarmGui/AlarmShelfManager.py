# This Python file uses the following encoding: utf-8

import threading
import logging

from PySide6.QtCore import QObject, Signal, QTimer


class AlarmShelfManager(QObject):
    """
    Centralised manager for alarm shelving state.

    Both the active and the shelved AlarmTableModel instances reference
    the same AlarmShelfManager so shelving / unshelving stays consistent.

    An alarm is added to this class (shelved) providing counter in seconds.
    At each iteration, a timer task decrements by one all the counters of the alarms
    and unshelves the alarms whose counter is zero.
    """

    # Emitted when an alarm is shelved (alarm_id)
    alarm_shelved = Signal(str)

    # Emitted when an alarm is unshelved (alarm_id, IasValue)
    alarm_unshelved = Signal(str)

    def __init__(self):
        super().__init__()
        self._logger = logging.getLogger(self.__class__.__name__)
        self._lock = threading.RLock()

        # The IDs of the shelved alarms
        self._shelved_ids: set[str] = set()
        self._shelved_counters: dict[str, int] = {}

        # Timer that runs timer_task once per second to decrement counters
        self._timer = QTimer(self)
        self._timer.setInterval(1000)
        self._timer.timeout.connect(self.timer_task)
        self._timer.start()

    def shelve(self, alarm_id: str, seconds: int):
        """
        Shelve an alarm for the given number of seconds.

        Returns True on success, False if seconds <= 0.
        """
        if seconds<=0:
            raise ValueError("Seconds to shelve must be greater than 0, got %d", seconds)
        with self._lock:
            self._shelved_ids.add(alarm_id)
            self._shelved_counters[alarm_id] = seconds
            self._logger.info("Shelved alarm %s for %ds", alarm_id, seconds)
        self.alarm_shelved.emit(alarm_id)

    def unshelve(self, alarm_id: str) -> bool:
        """
        Unshelve an alarm.  Returns the stored IasValue so the caller can
        re-insert it into the active table, or None if nothing was stored.
        """
        counter = None
        with self._lock:
            self._shelved_ids.discard(alarm_id)
            counter = self._shelved_counters.pop(alarm_id, None)

        if counter is not None:
            self._logger.info("Unshelved alarm %s", alarm_id)
            self.alarm_unshelved.emit(alarm_id)
        else:
            self._logger.error("Cannot unshelve %s: alarm not shelved", alarm_id)
        return counter is not None

    def timer_task(self) -> None:
        """
        Called once per second by the internal QTimer.
        Decrements counters for all shelved alarms and unshelves those that expire.
        """
        # The alarms whose counters reached 0 and must be unshelved
        expired: list[str] = []
        with self._lock:
            for alarm_id in list(self._shelved_counters.keys()):
                self._shelved_counters[alarm_id] -= 1
                if self._shelved_counters[alarm_id] <= 0:
                    expired.append(alarm_id)

        for alarm_id in expired:
            self.unshelve(alarm_id)

    def is_shelved(self, alarm_id: str) -> bool:
        with self._lock:
            return alarm_id in self._shelved_ids

    def get_shelved_count(self) -> int:
        with self._lock:
            return len(self._shelved_ids)

    def get_shelved_alarm_ids(self) -> list[str]:
        """
        Return the list of currently shelved alarm IDs.
        """
        with self._lock:
            return list(self._shelved_ids)

    def get_remaining_seconds(self, alarm_id: str) -> int:
        """
        Return the remaining shelved seconds for an alarm.
        Returns 0 if the alarm is not shelved.
        """
        with self._lock:
            return self._shelved_counters.get(alarm_id, 0)
