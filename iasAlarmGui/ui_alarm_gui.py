# -*- coding: utf-8 -*-

################################################################################
## Form generated from reading UI file 'alarm_gui.ui'
##
## Created by: Qt User Interface Compiler version 6.6.0
##
## WARNING! All changes made in this file will be lost when recompiling UI file!
################################################################################

from PySide6.QtCore import (QCoreApplication, QDate, QDateTime, QLocale,
    QMetaObject, QObject, QPoint, QRect,
    QSize, QTime, QUrl, Qt)
from PySide6.QtGui import (QAction, QBrush, QColor, QConicalGradient,
    QCursor, QFont, QFontDatabase, QGradient,
    QIcon, QImage, QKeySequence, QLinearGradient,
    QPainter, QPalette, QPixmap, QRadialGradient,
    QTransform)
from PySide6.QtWidgets import (QAbstractItemView, QApplication, QHBoxLayout, QHeaderView,
    QLabel, QLayout, QMainWindow, QMenu,
    QMenuBar, QSizePolicy, QStatusBar, QTableView,
    QTextEdit, QToolBar, QVBoxLayout, QWidget)
import rc_resources

class Ui_AlarmGui(object):
    def setupUi(self, AlarmGui):
        if not AlarmGui.objectName():
            AlarmGui.setObjectName(u"AlarmGui")
        AlarmGui.resize(800, 600)
        self.action_Connect = QAction(AlarmGui)
        self.action_Connect.setObjectName(u"action_Connect")
        icon = QIcon()
        icon.addFile(u":/icons/connect.png", QSize(), QIcon.Normal, QIcon.Off)
        self.action_Connect.setIcon(icon)
        self.actionE_xit = QAction(AlarmGui)
        self.actionE_xit.setObjectName(u"actionE_xit")
        self.action_Pause = QAction(AlarmGui)
        self.action_Pause.setObjectName(u"action_Pause")
        self.action_Pause.setCheckable(True)
        icon1 = QIcon()
        icon1.addFile(u":/icons/pause.png", QSize(), QIcon.Normal, QIcon.Off)
        icon1.addFile(u":/icons/play.png", QSize(), QIcon.Normal, QIcon.On)
        self.action_Pause.setIcon(icon1)
        self.action_Pause.setMenuRole(QAction.TextHeuristicRole)
        self.action_Remove_cleared = QAction(AlarmGui)
        self.action_Remove_cleared.setObjectName(u"action_Remove_cleared")
        self.action_Remove_cleared.setCheckable(True)
        icon2 = QIcon()
        icon2.addFile(u":/icons/remove.png", QSize(), QIcon.Normal, QIcon.Off)
        icon2.addFile(u":/icons/remove.png", QSize(), QIcon.Normal, QIcon.On)
        self.action_Remove_cleared.setIcon(icon2)
        self.action_Remove_cleared.setMenuRole(QAction.NoRole)
        self.action_About = QAction(AlarmGui)
        self.action_About.setObjectName(u"action_About")
        self.centralwidget = QWidget(AlarmGui)
        self.centralwidget.setObjectName(u"centralwidget")
        self.horizontalLayoutWidget = QWidget(self.centralwidget)
        self.horizontalLayoutWidget.setObjectName(u"horizontalLayoutWidget")
        self.horizontalLayoutWidget.setGeometry(QRect(-1, 9, 801, 541))
        self.horizontalLayout = QHBoxLayout(self.horizontalLayoutWidget)
        self.horizontalLayout.setObjectName(u"horizontalLayout")
        self.horizontalLayout.setSizeConstraint(QLayout.SetMinimumSize)
        self.horizontalLayout.setContentsMargins(10, 10, 10, 10)
        self.verticalLayout = QVBoxLayout()
        self.verticalLayout.setObjectName(u"verticalLayout")
        self.label = QLabel(self.horizontalLayoutWidget)
        self.label.setObjectName(u"label")

        self.verticalLayout.addWidget(self.label)

        self.alarmTable = QTableView(self.horizontalLayoutWidget)
        self.alarmTable.setObjectName(u"alarmTable")
        self.alarmTable.setAlternatingRowColors(True)
        self.alarmTable.setSelectionBehavior(QAbstractItemView.SelectRows)
        self.alarmTable.verticalHeader().setVisible(False)

        self.verticalLayout.addWidget(self.alarmTable)


        self.horizontalLayout.addLayout(self.verticalLayout)

        self.verticalLayout_2 = QVBoxLayout()
        self.verticalLayout_2.setObjectName(u"verticalLayout_2")
        self.label_2 = QLabel(self.horizontalLayoutWidget)
        self.label_2.setObjectName(u"label_2")

        self.verticalLayout_2.addWidget(self.label_2)

        self.alarmDetailsTE = QTextEdit(self.horizontalLayoutWidget)
        self.alarmDetailsTE.setObjectName(u"alarmDetailsTE")
        self.alarmDetailsTE.setReadOnly(True)

        self.verticalLayout_2.addWidget(self.alarmDetailsTE)


        self.horizontalLayout.addLayout(self.verticalLayout_2)

        self.horizontalLayout.setStretch(0, 60)
        self.horizontalLayout.setStretch(1, 40)
        AlarmGui.setCentralWidget(self.centralwidget)
        self.menubar = QMenuBar(AlarmGui)
        self.menubar.setObjectName(u"menubar")
        self.menubar.setGeometry(QRect(0, 0, 800, 22))
        self.menubar.setDefaultUp(False)
        self.menubar.setNativeMenuBar(True)
        self.menu_File = QMenu(self.menubar)
        self.menu_File.setObjectName(u"menu_File")
        self.menu_Help = QMenu(self.menubar)
        self.menu_Help.setObjectName(u"menu_Help")
        AlarmGui.setMenuBar(self.menubar)
        self.statusbar = QStatusBar(AlarmGui)
        self.statusbar.setObjectName(u"statusbar")
        AlarmGui.setStatusBar(self.statusbar)
        self.toolBar = QToolBar(AlarmGui)
        self.toolBar.setObjectName(u"toolBar")
        AlarmGui.addToolBar(Qt.TopToolBarArea, self.toolBar)

        self.menubar.addAction(self.menu_File.menuAction())
        self.menubar.addAction(self.menu_Help.menuAction())
        self.menu_File.addAction(self.action_Connect)
        self.menu_File.addAction(self.actionE_xit)
        self.menu_Help.addAction(self.action_About)
        self.toolBar.addAction(self.action_Pause)
        self.toolBar.addAction(self.action_Remove_cleared)

        self.retranslateUi(AlarmGui)
        self.actionE_xit.triggered.connect(AlarmGui.close)

        QMetaObject.connectSlotsByName(AlarmGui)
    # setupUi

    def retranslateUi(self, AlarmGui):
        AlarmGui.setWindowTitle(QCoreApplication.translate("AlarmGui", u"IAS Alarm GUI", None))
        self.action_Connect.setText(QCoreApplication.translate("AlarmGui", u"&Connect...", None))
#if QT_CONFIG(tooltip)
        self.action_Connect.setToolTip(QCoreApplication.translate("AlarmGui", u"Connect to IAS", None))
#endif // QT_CONFIG(tooltip)
        self.actionE_xit.setText(QCoreApplication.translate("AlarmGui", u"E&xit", None))
        self.action_Pause.setText(QCoreApplication.translate("AlarmGui", u"&Pause", None))
#if QT_CONFIG(tooltip)
        self.action_Pause.setToolTip(QCoreApplication.translate("AlarmGui", u"Pause/Resume", None))
#endif // QT_CONFIG(tooltip)
        self.action_Remove_cleared.setText(QCoreApplication.translate("AlarmGui", u"&Remove cleared", None))
#if QT_CONFIG(tooltip)
        self.action_Remove_cleared.setToolTip(QCoreApplication.translate("AlarmGui", u"Automatically remove cleared alarms", None))
#endif // QT_CONFIG(tooltip)
        self.action_About.setText(QCoreApplication.translate("AlarmGui", u"&About", None))
        self.label.setText(QCoreApplication.translate("AlarmGui", u"Alarms", None))
        self.label_2.setText(QCoreApplication.translate("AlarmGui", u"Details", None))
        self.menu_File.setTitle(QCoreApplication.translate("AlarmGui", u"&File", None))
        self.menu_Help.setTitle(QCoreApplication.translate("AlarmGui", u"&Help", None))
        self.toolBar.setWindowTitle(QCoreApplication.translate("AlarmGui", u"toolBar", None))
    # retranslateUi

