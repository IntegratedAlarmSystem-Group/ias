#! /usr/bin/env python

import glob
import unittest

from IasBasicTypes.IasType import IASType
from IasCdb.AlchemyDb import AlchemyDb
from IasCdb.AlchemyMapping import Property, Ias, Template_def, Iasio, Dasu, TransferFunction, Asce, Supervisor, \
    DasuToDeploy, TemplatedInstance
from IasCdb.LogLevel import LogLevel
from IasCdb.SoundType import SoundType
from IasCdb.SqlRunner import SqlRunner
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

        sqlRunner = SqlRunner(self.userName,self.pswd, self.rdbmsUrl)
        sqlRunner.executeSqlFromFile(sqlFile,ignoreErrors=True)

        if create:
            files = glob.glob('../**/CreateTables.sql', recursive=True)
            self.assertEqual(len(files),1)
            sqlFile = files[0]

            sqlRunner = SqlRunner(self.userName,self.pswd,self.rdbmsUrl)
            sqlRunner.executeSqlFromFile(sqlFile,ignoreErrors=False)
        sqlRunner.close()


    def setUp(self):
        self.userName='iastest'
        self.pswd='test'
        self.rdbmsUrl='iasdevel.hq.eso.org:1521/XE'
        self.deleteAndCreateTables(True)
        self.db = AlchemyDb(self.userName,self.pswd,self.rdbmsUrl)
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
            VALIDITYTHRESHOLD=20,
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
            VALIDITYTHRESHOLD=20,
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

        template2= Template_def(TEMPLATE_ID='TemplateForAsceTInputs', MIN=2,MAX=5)
        print("Adding template",template2)
        self.session.add(template2)
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
            TEMPLATE_ID = None,
            EMAILS = 'acaproni@gmail.com',
            SOUND = SoundType.NONE.name)
        self.session.add(i2)

        i3 = Iasio(IO_ID="IASIO3", SHORTDESC = 'another short desc 3',
                   IASTYPE = IASType.INT.name,
                   DOCURL = 'http:www.eso.org',
                   CANSHELVE = 0,
                   TEMPLATE_ID = 'A template',
                   EMAILS = 'name.familyName@gmail.com',
                   SOUND = SoundType.TYPE4.name)
        self.session.add(i3)
        self.session.commit()
        print('Added 3 IASIOs:')
        print(i1)
        print(i2)
        print(i3)

        # The input for the templated inputs of the ASCE
        templInput = Iasio(IO_ID="TEMPLATED-IASIO", SHORTDESC = 'Templated input',
                   IASTYPE = IASType.ALARM.name,
                   DOCURL = 'http:www.eso.org',
                   CANSHELVE = 0,
                   TEMPLATE_ID = 'TemplateForAsceTInputs',
                   EMAILS = 'name.familyName@gmail.com',
                   SOUND = SoundType.TYPE1.name)
        self.session.add(templInput)
        self.session.commit()
        print('Added input for templated inputs of ASCE:')
        print(templInput)

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

        pa2 = Property(NAME="n2",VALUE="v2")
        pa3 = Property(NAME="n3",VALUE="v3")

        templatedInput1 = TemplatedInstance(TEMPLATE_ID="TemplateForAsceTInputs", IO_ID='TEMPLATED-IASIO', INSTANCE_NUM=3)
        templatedInput2 = TemplatedInstance(TEMPLATE_ID="TemplateForAsceTInputs", IO_ID='TEMPLATED-IASIO', INSTANCE_NUM=4)

        asce = Asce(
            ASCE_ID = 'AsceId',
            TRANSF_FUN_ID = 'org.eso.ias.tf.MinMax',
            OUTPUT_ID = 'IASIO1',
            DASU_ID = 'DasuId',
            TEMPLATE_ID='A template',
            asceProps=[pa2,pa3],
            asceTemplatedInputs=[templatedInput1,templatedInput2]
        )
        self.session.add(asce)
        print('Adding ASCE:',asce)
        self.session.commit()

        dtd=DasuToDeploy(
            SUPERVISOR_ID = "SupervisorId",
            DASU_ID = 'DasuId',
            TEMPLATE_ID = 'A template',
            INSTANCE = 5)
        self.session.add(dtd)
        print('Adding a DASU to deploy:',dtd)
        self.session.commit()

        print('Reading from RDB')
        #
        # --------- Reads back and check the configuration
        #
        s=self.session.query(Supervisor)
        self.assertEquals(s.count(),1)
        sup=s.first()
        self.assertTrue(sup==superv)

        d = self.session.query(DasuToDeploy)
        self.assertEquals(d.count(),1)
        self.assertTrue(d.first()==dtd)

        a = self.session.query(Asce)
        self.assertEquals(a.count(),1)
        self.assertTrue(a.first()==asce)

        dd = self.session.query(Dasu)
        self.assertEquals(dd.count(),1)
        self.assertTrue(dd.first()==dasu)

        i=self.session.query(Ias).order_by(Ias.ID).all()
        self.assertEquals(len(i),2)
        self.assertTrue(i[0]==ias)
        self.assertTrue(i[1]==ias2)

        p=self.session.query(Property).order_by(Property.ID).all()
        self.assertEquals(len(p),8)
        self.assertTrue(p[0]==p1)
        self.assertTrue(p[1]==p11)
        self.assertTrue(p[2]==p111)
        self.assertTrue(p[3]==p2)
        self.assertTrue(p[4]==p22)
        self.assertTrue(p[5]==p222)
        self.assertTrue(p[6]==pa2)
        self.assertTrue(p[7]==pa3)

        t=self.session.query(TransferFunction).order_by(TransferFunction.CLASSNAME_ID).all()
        self.assertEquals(len(t),1)
        self.assertTrue(t[0]==tf)

        ios = self.session.query(Iasio).order_by(Iasio.IO_ID).all()
        self.assertEquals(len(ios),4)
        self.assertTrue(ios[0],i1)
        self.assertTrue(ios[1],i2)
        self.assertTrue(ios[2],i3)

        templInputs = self.session.query(TemplatedInstance).order_by(TemplatedInstance.ID).all()
        self.assertEquals(len(templInputs),2)
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

    def testWriteModifyDeleteTF(self):
        print('testWriteModifyDeleteTF')

        tf =TransferFunction(CLASSNAME_ID='org.eso.ias.tf.Threshold',IMPLLANG=TFLanguage.SCALA.name)
        print('Writing TF:',tf)
        self.session.add(tf)
        self.session.commit()

        tf.IMPLLANG=TFLanguage.JAVA.name
        self.session.commit()


        t=self.session.query(TransferFunction).order_by(TransferFunction.CLASSNAME_ID).all()
        self.assertEquals(len(t),1)
        self.assertTrue(t[0]==tf)
        print('TF read from DB:',t)

        self.session.query(TransferFunction).filter(TransferFunction.CLASSNAME_ID=='org.eso.ias.tf.Threshold').delete()

        t=self.session.query(TransferFunction).all()
        self.assertEquals(len(t),0)

if __name__ == '__main__':
    unittest.main()