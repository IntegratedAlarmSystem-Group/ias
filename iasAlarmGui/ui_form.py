# -*- coding: utf-8 -*-

################################################################################
## Form generated from reading UI file 'form.ui'
##
## Created by: Qt User Interface Compiler version 6.5.3
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
from PySide6.QtWidgets import (QApplication, QHBoxLayout, QHeaderView, QLabel,
    QLayout, QListWidget, QListWidgetItem, QMainWindow,
    QMenu, QMenuBar, QSizePolicy, QStatusBar,
    QTableView, QVBoxLayout, QWidget)

class Ui_AlarmGui(object):
    def setupUi(self, AlarmGui):
        if not AlarmGui.objectName():
            AlarmGui.setObjectName(u"AlarmGui")
        AlarmGui.resize(800, 600)
        self.action_Connect = QAction(AlarmGui)
        self.action_Connect.setObjectName(u"action_Connect")
        self.actionE_xit = QAction(AlarmGui)
        self.actionE_xit.setObjectName(u"actionE_xit")
        self.centralwidget = QWidget(AlarmGui)
        self.centralwidget.setObjectName(u"centralwidget")
        self.horizontalLayoutWidget = QWidget(self.centralwidget)
        self.horizontalLayoutWidget.setObjectName(u"horizontalLayoutWidget")
        self.horizontalLayoutWidget.setGeometry(QRect(-1, 9, 791, 541))
        self.horizontalLayout = QHBoxLayout(self.horizontalLayoutWidget)
        self.horizontalLayout.setObjectName(u"horizontalLayout")
        self.horizontalLayout.setSizeConstraint(QLayout.SetMinimumSize)
        self.horizontalLayout.setContentsMargins(10, 10, 10, 10)
        self.verticalLayout = QVBoxLayout()
        self.verticalLayout.setObjectName(u"verticalLayout")
        self.label = QLabel(self.horizontalLayoutWidget)
        self.label.setObjectName(u"label")

        self.verticalLayout.addWidget(self.label)

        self.tableView = QTableView(self.horizontalLayoutWidget)
        self.tableView.setObjectName(u"tableView")

        self.verticalLayout.addWidget(self.tableView)


        self.horizontalLayout.addLayout(self.verticalLayout)

        self.verticalLayout_2 = QVBoxLayout()
        self.verticalLayout_2.setObjectName(u"verticalLayout_2")
        self.label_2 = QLabel(self.horizontalLayoutWidget)
        self.label_2.setObjectName(u"label_2")

        self.verticalLayout_2.addWidget(self.label_2)

        self.listWidget = QListWidget(self.horizontalLayoutWidget)
        self.listWidget.setObjectName(u"listWidget")

        self.verticalLayout_2.addWidget(self.listWidget)


        self.horizontalLayout.addLayout(self.verticalLayout_2)

        self.horizontalLayout.setStretch(0, 50)
        self.horizontalLayout.setStretch(1, 50)
        AlarmGui.setCentralWidget(self.centralwidget)
        self.menubar = QMenuBar(AlarmGui)
        self.menubar.setObjectName(u"menubar")
        self.menubar.setGeometry(QRect(0, 0, 800, 22))
        self.menubar.setDefaultUp(False)
        self.menubar.setNativeMenuBar(True)
        self.menu_File = QMenu(self.menubar)
        self.menu_File.setObjectName(u"menu_File")
        AlarmGui.setMenuBar(self.menubar)
        self.statusbar = QStatusBar(AlarmGui)
        self.statusbar.setObjectName(u"statusbar")
        AlarmGui.setStatusBar(self.statusbar)

        self.menubar.addAction(self.menu_File.menuAction())
        self.menu_File.addAction(self.action_Connect)
        self.menu_File.addAction(self.actionE_xit)

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
        self.label.setText(QCoreApplication.translate("AlarmGui", u"Alarms", None))
        self.label_2.setText(QCoreApplication.translate("AlarmGui", u"Details", None))
        self.menu_File.setTitle(QCoreApplication.translate("AlarmGui", u"&File", None))
    # retranslateUi

