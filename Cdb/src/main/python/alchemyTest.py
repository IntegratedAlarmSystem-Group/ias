#! /usr/bin/env python

import unittest, glob, sys

from IasCdb.SqlRunner import SqlRunner
from IasCdb.AlchemyDb import AlchemyDb
from IasCdb.AlchemyMapping import Property, Ias, Template_def, Iasio, Dasu,TransferFunction, Asce, Supervisor,DasuToDeploy
from IasBasicTypes.IasType import IASType
from IasCdb.SoundType import SoundType
from IasCdb.LogLevel import LogLevel
from IasCdb.TFLanguage import TFLanguage

class PyCdbTest(unittest.TestCase):
    '''
    Test read/write/update of the CDB by the python classes

    '''

    def deleteAndCreateTables(self,create):
        '''
        Delete and optionally create the tables of the CDB by running the
        SQL scripts in DropTables.sql
        :param create: if True create the tables
        :return:
        '''
        files = glob.glob('../**/DropTables.sql', recursive=True)
        self.assertEqual(len(files),1)
        sqlFile = files[0]

        sqlRunner = SqlRunner('iastest','test','192.168.120.130:1521/XE')
        sqlRunner.executeSqlFromFile(sqlFile,ignoreErrors=True)

        if create:
            files = glob.glob('../**/CreateTables.sql', recursive=True)
            self.assertEqual(len(files),1)
            sqlFile = files[0]

            sqlRunner = SqlRunner('iastest','test','192.168.120.130:1521/XE')
            sqlRunner.executeSqlFromFile(sqlFile,ignoreErrors=False)
        sqlRunner.close()


    def setUp(self):
        self.deleteAndCreateTables(True)
        self.db = AlchemyDb('iastest','test','192.168.120.130:1521/XE')
        self.session = self.db.getSession()

    def tearDown(self):
        self.db.close()
        self.deleteAndCreateTables(False)


    def testReadWrite(self):
        '''
        Write a configuration in the CDB than read it and check the correctness
        :return:
        '''

        #
        # --------- Write the configuration
        #

        p1 = Property(NAME="n1",VALUE="v1")
        p11 = Property(NAME="n11",VALUE="v11")
        p111 = Property(NAME="n111",VALUE="v111")

        ias = Ias(
            LOGLEVEL='debug',
            REFRESHRATE=12,
            TOLERANCE=2,
            HBFREQUENCY=7,
            BSDBURL='localhost:9092',
            SMTP='smtp.alma.cl',
            props=[p1,p11,p111])

        print("Adding: ",ias)

        self.session.add(ias)
        self.session.commit()

        p2 = Property(NAME="n2",VALUE="v2")
        p22 = Property(NAME="n22",VALUE="v22")
        p222 = Property(NAME="n222",VALUE="v222")

        ias2 = Ias(
            LOGLEVEL='debug',
            REFRESHRATE=12,
            TOLERANCE=2,
            HBFREQUENCY=7,
            BSDBURL='localhost:9092',
            SMTP='smtp.alma.cl',
            props=[p2,p22,p222])
        self.session.add(ias2)
        self.session.commit()
        print("Added: ",ias2)


        template= Template_def(TEMPLATE_ID='A template', MIN=1,MAX=66)
        print("Adding template",template)
        self.session.add(template)
        self.session.commit()



        i1 = Iasio(IO_ID="IASIO1", SHORTDESC = 'short desc',
            IASTYPE = IASType.ALARM.name,
            DOCURL = 'http:www.a.b.c.com',
            CANSHELVE = 1,
            TEMPLATE_ID = 'A template',
            EMAILS = 'acaproni@eso.org',
            SOUND = SoundType.TYPE2.name)
        self.session.add(i1)


        i2 = Iasio(IO_ID="IASIO2", SHORTDESC = 'another short desc',
            IASTYPE = IASType.DOUBLE.name,
            DOCURL = 'http:www.alma.cl',
            CANSHELVE = 0,
            TEMPLATE_ID = 'A template',
            EMAILS = 'acaproni@gmail.com',
            SOUND = SoundType.NONE.name)
        self.session.add(i2)

        i3 = Iasio(IO_ID="IASIO3", SHORTDESC = 'another short desc 3',
                   IASTYPE = IASType.INT.name,
                   DOCURL = 'http:www.eso.org',
                   CANSHELVE = 0,
                   TEMPLATE_ID = 'A template',
                   EMAILS = 'acaproni.eso@gmail.com',
                   SOUND = SoundType.TYPE4.name)
        self.session.add(i3)
        self.session.commit()
        print('Added 3 IASIOs:')
        print(i1)
        print(i2)
        print(i3)

        tf =TransferFunction(CLASSNAME_ID='org.eso.ias.tf.MinMax',IMPLLANG=TFLanguage.SCALA.name)
        print('Adding TF:',tf)
        self.session.add(tf)
        self.session.commit()

        dasu = Dasu(DASU_ID='DasuId',LOGLEVEL=LogLevel.ERROR.name, OUTPUT_ID='IASIO2',TEMPLATE_ID = 'A template')
        print('Adding DASU:',dasu)
        self.session.add(dasu)
        self.session.commit()

        superv = Supervisor(
            SUPERVISOR_ID="SupervisorId",
            HOSTNAME = "127.0.0.1",
            LOGLEVEL=LogLevel.WARN.name
        )
        self.session.add(superv)
        print('Adding Supervisor:',superv)
        self.session.commit()

        p2 = Property(NAME="n2",VALUE="v2")
        p3 = Property(NAME="n3",VALUE="v3")
        a = Asce(
            ASCE_ID = 'AsceId',
            TRANSF_FUN_ID = 'org.eso.ias.tf.MinMax',
            OUTPUT_ID = 'IASIO1',
            DASU_ID = 'DasuId',
            TEMPLATE_ID='A template',
            asceProps=[p2,p3]
        )
        self.session.add(a)
        print('Adding ASCE:',a)
        self.session.commit()

        dtd=DasuToDeploy(
            SUPERVISOR_ID = "SupervisorId",
            DASU_ID = 'DasuId',
            TEMPLATE_ID = 'A template',
            INSTANCE = 5)
        self.session.add(dtd)
        print('Adding a DASU to deploy:',dtd)
        self.session.commit()


        #
        # --------- Reads the configuration
        #
        for s in self.session.query(Supervisor).filter(Supervisor.SUPERVISOR_ID=='SupervisorId'):
            print(s)
            dasuToD=s.dasusToDeploy[0]
            print(dasuToD)
            print(dasuToD.dasu)
            print(dasuToD.template)
            asces = dasuToD.dasu.asces
            print("Output=",dasuToD.dasu.output)
            for asce in asces:
                print(asce)

if __name__ == '__main__':
    unittest.main()