# -*- coding: utf-8 -*-

################################################################################
## Form generated from reading UI file 'about_dialog.ui'
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
    QSizePolicy, QTextEdit, QVBoxLayout, QWidget)

class Ui_AboutDlg(object):
    def setupUi(self, AboutDlg):
        if not AboutDlg.objectName():
            AboutDlg.setObjectName(u"AboutDlg")
        self.verticalLayoutWidget = QWidget(AboutDlg)
        self.verticalLayoutWidget.setObjectName(u"verticalLayoutWidget")
        self.verticalLayoutWidget.setGeometry(QRect(9, 9, 461, 381))
        self.verticalLayout = QVBoxLayout(self.verticalLayoutWidget)
        self.verticalLayout.setObjectName(u"verticalLayout")
        self.verticalLayout.setContentsMargins(0, 0, 0, 0)
        self.aboutText = QTextEdit(self.verticalLayoutWidget)
        self.aboutText.setObjectName(u"aboutText")
        self.aboutText.setUndoRedoEnabled(False)
        self.aboutText.setReadOnly(True)

        self.verticalLayout.addWidget(self.aboutText)

        self.buttonBox = QDialogButtonBox(self.verticalLayoutWidget)
        self.buttonBox.setObjectName(u"buttonBox")
        self.buttonBox.setOrientation(Qt.Horizontal)
        self.buttonBox.setStandardButtons(QDialogButtonBox.Ok)
        self.buttonBox.setCenterButtons(True)

        self.verticalLayout.addWidget(self.buttonBox)


        self.retranslateUi(AboutDlg)
        self.buttonBox.accepted.connect(AboutDlg.accept)
        self.buttonBox.rejected.connect(AboutDlg.reject)

        QMetaObject.connectSlotsByName(AboutDlg)
    # setupUi

    def retranslateUi(self, AboutDlg):
        AboutDlg.setWindowTitle(QCoreApplication.translate("AboutDlg", u"About", None))
    # retranslateUi

