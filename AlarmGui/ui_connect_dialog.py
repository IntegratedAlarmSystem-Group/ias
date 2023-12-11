# -*- coding: utf-8 -*-

################################################################################
## Form generated from reading UI file 'connect_dialog.ui'
##
## Created by: Qt User Interface Compiler version 6.6.0
##
## WARNING! All changes made in this file will be lost when recompiling UI file!
################################################################################

from PySide6.QtCore import (QCoreApplication, QDate, QDateTime, QLocale,
    QMetaObject, QObject, QPoint, QRect,
    QSize, QTime, QUrl, Qt)
from PySide6.QtGui import (QBrush, QColor, QConicalGradient, QCursor,
    QFont, QFontDatabase, QGradient, QIcon,
    QImage, QKeySequence, QLinearGradient, QPainter,
    QPalette, QPixmap, QRadialGradient, QTransform)
from PySide6.QtWidgets import (QAbstractButton, QApplication, QDialog, QDialogButtonBox,
    QHBoxLayout, QLabel, QLineEdit, QPushButton,
    QSizePolicy, QVBoxLayout, QWidget)

class Ui_ConnectToIas(object):
    def setupUi(self, ConnectToIas):
        if not ConnectToIas.objectName():
            ConnectToIas.setObjectName(u"ConnectToIas")
        ConnectToIas.resize(320, 189)
        self.buttonBox = QDialogButtonBox(ConnectToIas)
        self.buttonBox.setObjectName(u"buttonBox")
        self.buttonBox.setGeometry(QRect(10, 150, 301, 32))
        self.buttonBox.setOrientation(Qt.Horizontal)
        self.buttonBox.setStandardButtons(QDialogButtonBox.Cancel|QDialogButtonBox.Ok)
        self.verticalLayoutWidget = QWidget(ConnectToIas)
        self.verticalLayoutWidget.setObjectName(u"verticalLayoutWidget")
        self.verticalLayoutWidget.setGeometry(QRect(10, 30, 303, 121))
        self.verticalLayout = QVBoxLayout(self.verticalLayoutWidget)
        self.verticalLayout.setObjectName(u"verticalLayout")
        self.verticalLayout.setContentsMargins(0, 0, 0, 0)
        self.fetchFromCdbBtn = QPushButton(ConnectToIas)
        self.fetchFromCdbBtn.setObjectName(u"fetchFromCdbBtn")
        self.fetchFromCdbBtn.setGeometry(QRect(10, 10, 301, 25))
        self.layoutWidget = QWidget(ConnectToIas)
        self.layoutWidget.setObjectName(u"layoutWidget")
        self.layoutWidget.setGeometry(QRect(10, 40, 301, 111))
        self.horizontalLayout = QHBoxLayout(self.layoutWidget)
        self.horizontalLayout.setObjectName(u"horizontalLayout")
        self.horizontalLayout.setContentsMargins(0, 0, 0, 0)
        self.label = QLabel(self.layoutWidget)
        self.label.setObjectName(u"label")

        self.horizontalLayout.addWidget(self.label)

        self.lineEdit = QLineEdit(self.layoutWidget)
        self.lineEdit.setObjectName(u"lineEdit")
        self.lineEdit.setText(u"localhost:9092")
        self.lineEdit.setDragEnabled(False)

        self.horizontalLayout.addWidget(self.lineEdit)


        self.retranslateUi(ConnectToIas)
        self.buttonBox.rejected.connect(ConnectToIas.reject)
        self.fetchFromCdbBtn.clicked.connect(ConnectToIas.onFetchBtnClicked)
        self.lineEdit.textChanged.connect(ConnectToIas.onLineEditTxtChanged)
        self.buttonBox.accepted.connect(ConnectToIas.onOkBtnClicked)

        QMetaObject.connectSlotsByName(ConnectToIas)
    # setupUi

    def retranslateUi(self, ConnectToIas):
        ConnectToIas.setWindowTitle(QCoreApplication.translate("ConnectToIas", u"Connect to IAS...  ", None))
#if QT_CONFIG(tooltip)
        self.fetchFromCdbBtn.setToolTip(QCoreApplication.translate("ConnectToIas", u"Select the path of the CDB", None))
#endif // QT_CONFIG(tooltip)
        self.fetchFromCdbBtn.setText(QCoreApplication.translate("ConnectToIas", u"Fetch from CDB", None))
        self.label.setText(QCoreApplication.translate("ConnectToIas", u"Kafka brokers:", None))
#if QT_CONFIG(tooltip)
        self.lineEdit.setToolTip(QCoreApplication.translate("ConnectToIas", u"list of comma separated server:port to connect to IAS", None))
#endif // QT_CONFIG(tooltip)
    # retranslateUi

