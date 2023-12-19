from PySide6.QtWidgets import QDialog
from IasAlarmGui.ui_about_dialog import Ui_AboutDlg

class AboutDlg(QDialog, Ui_AboutDlg):
    def __init__(self,parent=None):
        super().__init__(parent)
        self.ui = Ui_AboutDlg()
        self.ui.setupUi(self)

        aboutMdStr = '''# About

## IAS
The [Integrated Alarm System](https://integratedalarmsystem-group.github.io) is licensed under
GNU LESSER GENERAL PUBLIC LICENSE ([LGPL v3](https://www.gnu.org/licenses/lgpl-3.0.en.html)).

The source code is in [github](https://github.com/IntegratedAlarmSystem-Group/ias).

## iasAlarmGui
The iasAlarmGui has been developed with PySide6 to show a table of alarms read from the
BSDB core topic.

### iasAlarmGUI icons
The icons of the iasAlarmGui have been downloaded from [https://p.yusukekamiyamane.com](https://p.yusukekamiyamane.com)
and are distrubuted under the  [Common Creative Attribution 3.0](https://creativecommons.org/licenses/by/3.0/)
license.

We are thankful to _Yusuke Kamiyamane_ for distributing his work with such license.
        '''

        self.ui.aboutText.setMarkdown(aboutMdStr)
