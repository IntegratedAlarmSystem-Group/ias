from PySide6.QtWidgets import QTableView
from PySide6.QtCore import Qt

from IasAlarmGui.AlarmTableModel import TableMode

class AlarmGuiTableView(QTableView):
    """"
    The QTableView subclass to handle the right and left click on the table rows
    for the active and shelved alarm tables.
    """

    def __init__(self, parent, events_listener, mode: TableMode):
        super().__init__(parent)
        self.events_listener = events_listener
        if not self.events_listener:
            raise ValueError("The events_listener must not be None")
        
        self.mode = mode
        
        self.setSelectionBehavior(QTableView.SelectRows)
        self.setSelectionMode(QTableView.SingleSelection)
    
    def setModel(self, model):
        super().setModel(model)
        # reconnect selectionChanged every time model changes
        if self.selectionModel():
            self.selectionModel().selectionChanged.connect(
                self.onTableSelectionChanged)

    def mousePressEvent(self, event):
        index = self.indexAt(event.position().toPoint())

        if index.isValid():
            if event.button() == Qt.LeftButton:
                self.events_listener.onLeftClick(index, self.mode)

            elif event.button() == Qt.RightButton:
                self.events_listener.onRightClick(index, self.mode)

        super().mousePressEvent(event)

    def onTableSelectionChanged(self, selected, deselected):
        self.events_listener.onTableSelectionChanged( selected, deselected, self.mode)