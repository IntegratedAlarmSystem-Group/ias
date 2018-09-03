#! /usr/bin/env python

from enum import Enum

from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, ForeignKey, Table
from sqlalchemy.orm import validates, relationship

'''
Defines the mapping of the CDB with  alchemy
'''

Base = declarative_base()

ias_props_association_table = Table("IAS_PROPERTY", Base.metadata,
                                    Column('IAS_ID',Integer, ForeignKey('IAS.ID')),
                                    Column('PROPS_ID',Integer, ForeignKey('PROPERTY.ID'))
                                    )

class Property(Base):
    __tablename__ = "PROPERTY"

    ID = Column(Integer, primary_key=True)
    NAME = Column(String(255))
    VALUE = Column(String(255))

    @validates('NAME')
    def validate_name(self,key,name):
        assert(name is not None and name is not '')
        return name

    iass = relationship("Ias", secondary=ias_props_association_table,back_populates="props")

    def __repr__(self):
        return "<PROPERTY(id='%d', name='%s', value='%s')>" % (self.ID,self.NAME,self.VALUE)

class Ias(Base):

    __tablename__ = 'IAS'

    ID = Column(Integer, primary_key=True)
    LOGLEVEL = Column(String(10))
    REFRESHRATE = Column(Integer, nullable=False)
    TOLERANCE= Column(Integer, nullable=False)
    HBFREQUENCY = Column(Integer, nullable=False)
    BSDBURL = Column(String(255), nullable=False)
    SMTP = Column(String(64))

    props = relationship("Property", secondary=ias_props_association_table,back_populates="iass")

    def __repr__(self):
        return "<IAS(id=%d, logLevel='%s', refreshRate=%d, tolerance=%d, hbFrequency=%d, bsdbUrl='%s', smtp='%s', props=%s)>" % (
            self.ID, self.LOGLEVEL,self.REFRESHRATE,self.TOLERANCE,self.HBFREQUENCY, self.BSDBURL, self.SMTP, self.props)

class Template_def(Base):
    __tablename__ = 'TEMPLATE_DEF'

    TEMPLATE_ID = Column(String(64), primary_key=True)
    MIN = Column(Integer)
    MAX = Column(Integer)

    @validates('MIN', 'MAX')
    def validate_max(self,key,min,max):
        assert(min>=0 and max>min)

    def __repr__(self):
        return "<TEMPLATE_DEF(tempalte_id='%s', min=%d, max=%d)>" % (self.TEMPLATE_ID, self.MIN, self.MAX)

class Iasio(Base):
    __tablename__ = 'IASIO'

    IO_ID = Column(String(64), primary_key=True)
    SHORTDESC = Column(String(256),nullable=True)
    IASTYPE = Column(String(16),nullable=False)
    DOCURL = Column(String(256),nullable=True)
    CANSHELVE = Column(Integer)
    TEMPLATE_ID = Column(String(64),ForeignKey("TEMPLATE_DEF.TEMPLATE_ID"))
    EMAILS = Column(String(128), nullable=True)
    SOUND = Column(String(16))

    def __repr__(self):
        return "<IASIO(id='%s', type='%s', canShelve=%d,  template_id='%s', doc='%s', desc='%s', emails='%s', sound=-'%s')>" % (
            self.IO_ID, self.IASTYPE, self.CANSHELVE, self.TEMPLATE_ID, self.DOCURL, self.SHORTDESC, self.EMAILS, self.SOUND
        )

class Dasu(Base):
    __tablename__ = 'DASU'
    DASU_ID  = Column(String(64), primary_key=True)
    LOGLEVEL = Column(String(16))
    OUTPUT_ID = Column(String(64), ForeignKey('IASIO.IO_ID'))
    TEMPLATE_ID = Column(String(64), ForeignKey('TEMPLATE_DEF.TEMPLATE_ID'))

    def __repr__(self):
        return "<DASU(id='%s', logLevel='%s', outputId='%s', templateId='%s')>" % (
            self.DASU_ID, self.LOGLEVEL, self.OUTPUT_ID, self.TEMPLATE_ID
        )

class TransferFunction(Base):
    __tablename__ = 'TRANSFER_FUNC'

    CLASSNAME_ID = Column(String(64), primary_key=True)
    IMPL_LANG = Column(String(16), nullable=False)

    def __repr__(self):
        return "<TRANSFER_FUNC(classNameId='%s', implLang='%s')>" % (
            self.CLASSNAME_ID, self.IMPL_LANG
        )

class Asce(Base):
    __tablename__ = 'ASCE'

    ASCE_ID = Column(String(64), primary_key=True)
    TRANSF_FUN_ID = Column(String(96), ForeignKey('TRANSFER_FUNC.CLASSNAME_ID'))
    OUTPUT_ID = Column(String(64), ForeignKey('IASIO.IO_ID'))
    DASU_ID = Column(String(64), ForeignKey('DASU.DASU_ID'))
    TEMPLATE_ID = Column(String(64), ForeignKey('TEMPLATE_DEF.TEMPLATE_ID'))

    def __repr__(self):
        return "<ASCE(id='%s', transfFuncId='%s', outputId='%s', dasuId='%s', templateId='%s')>" % (
            self.ASCE_ID, self.TRANSF_FUN_ID, self.OUTPUT_ID, self.DASU_ID, self.TEMPLATE_ID
        )

asce_props_association_table = Table("ASCE_PROPERTY", Base.metadata,
                                     Column('ASCE_ID',Integer, ForeignKey('ASCE.ASCE_ID')),
                                     Column('PROPS_ID',Integer, ForeignKey('PROPERTY.ID'))
                                     )

asce_iasio_association_table = Table("ASCE_IASIO", Base.metadata,
                                     Column('ASCE_ID',Integer, ForeignKey('ASCE.ASCE_ID')),
                                     Column('IO_ID',Integer, ForeignKey('IASIO.IO_ID'))
                                     )

class Supervisor(Base):
    __tablename__ = 'SUPERVISOR'

    SUPERVISOR_ID = Column(String(64), primary_key=True)
    HOSTNAME = Column(String(64), nullable=False)
    LOGLEVEL = Column(String(16))

    def __repr__(self):
        return "<SUPERVISOR(id='%s', hostName='%s', logLevel='%s')>" % (
            self.SUPERVISOR_ID, self.HOSTNAME, self.LOGLEVEL
        )

class DasuToDeploy(Base):
    __tablename__ = 'DASUS_TO_DEPLOY'

    ID = Column(Integer, primary_key=True)
    SUPERVISOR_ID = Column(String(64), ForeignKey('SUPERVISOR.SUPERVISOR_ID'))
    DASU_ID = Column(String(64), ForeignKey('DASU.DASU_ID'))
    TEMPLATE_ID = Column(String(64), ForeignKey('TEMPLATE_DEF.TEMPLATE_ID'))
    INSTANCE = Column(Integer)

    @validates('INSTANCE')
    def validate_instance(self,key,instance):
        assert(instance>=0)
        return instance

    def __repr__(self):
        return "<DASUS_TO_DEPLOY(id=%d, supervisorId='%s', dasuId='%s', templateId='%s', instance=%d')>" % (
            self.ID, self.SUPERVISOR_ID, self.DASU_ID, self.TEMPLATE_ID, self.INSTANCE
        )


