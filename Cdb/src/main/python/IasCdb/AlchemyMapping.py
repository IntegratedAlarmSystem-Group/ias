#! /usr/bin/env python

from enum import Enum

from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, ForeignKey, Table
from sqlalchemy.schema import Sequence
from sqlalchemy.orm import validates, relationship

'''
Defines the mapping of the CDB with  alchemy
'''

Base = declarative_base()

ias_props_association_table = Table("IAS_PROPERTY", Base.metadata,
                                    Column('IAS_ID',Integer, ForeignKey('IAS.ID')),
                                    Column('PROPS_ID',Integer, ForeignKey('PROPERTY.ID'))
                                    )
asce_props_association_table = Table("ASCE_PROPERTY", Base.metadata,
                                     Column('ASCE_ID',String(64), ForeignKey('ASCE.ASCE_ID')),
                                     Column('PROPS_ID',Integer, ForeignKey('PROPERTY.ID'))
                                     )

class Property(Base):
    __tablename__ = "PROPERTY"

    ID = Column(Integer, Sequence('PROP_SEQ_GENERATOR'), primary_key=True, autoincrement='auto')
    NAME = Column(String(255))
    VALUE = Column(String(255))

    @validates('NAME')
    def validate_name(self,key,name):
        assert(name is not None and name is not '')
        return name

    iass = relationship("Ias", secondary=ias_props_association_table,back_populates="props")
    asces = relationship("Asce", secondary=asce_props_association_table,back_populates="asceProps")

    def __repr__(self):
        if self.ID is not None:
            return "<PROPERTY(id='%d', name='%s', value='%s')>" % (self.ID,self.NAME,self.VALUE)
        else:
            return "<PROPERTY(id=<?>, name='%s', value='%s')>" % (self.NAME,self.VALUE)

class Ias(Base):

    __tablename__ = 'IAS'

    ID = Column(Integer, Sequence('IAS_SEQ_GENERATOR'), primary_key=True, autoincrement='auto')
    LOGLEVEL = Column(String(10))
    REFRESHRATE = Column(Integer, nullable=False)
    TOLERANCE= Column(Integer, nullable=False)
    HBFREQUENCY = Column(Integer, nullable=False)
    BSDBURL = Column(String(255), nullable=False)
    SMTP = Column(String(64))

    props = relationship("Property", secondary=ias_props_association_table,back_populates="iass")

    @validates('REFRESHRATE')
    def validate_refresh_rate(self,key,rate):
        assert(rate>0)
        return rate

    @validates('HBFREQUENCY')
    def validate_refresh_hbe(self,key,hb):
        assert(hb>0)
        return hb

    @validates('TOLERANCE')
    def validate_refresh_tolerancee(self,key,tolerance):
        assert(tolerance>0)
        return tolerance

    def __repr__(self):
        return "<IAS(logLevel='%s', refreshRate=%d, tolerance=%d, hbFrequency=%d, bsdbUrl='%s', smtp='%s', props=%s)>" % (
            self.LOGLEVEL,self.REFRESHRATE,self.TOLERANCE,self.HBFREQUENCY, self.BSDBURL, self.SMTP, self.props)

class Template_def(Base):
    __tablename__ = 'TEMPLATE_DEF'

    TEMPLATE_ID = Column(String(64), primary_key=True)
    MIN = Column(Integer)
    MAX = Column(Integer)

    @validates('MIN', 'MAX')
    def validate_max(self,key,field):
        if key=='MIN':
            assert(field>=0)
        else:
            assert(field>0)
        if self.MIN is not None and self.MAX is not None:
            assert(self.MAX>self.MIN)
        return field

    def __repr__(self):
        return "<TEMPLATE_DEF(template_id='%s', min=%d, max=%d)>" % (self.TEMPLATE_ID, self.MIN, self.MAX)

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

    template = relationship('Template_def')

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

    output = relationship('Iasio')
    template = relationship('Template_def')
    asces = relationship('Asce',back_populates="dasu")

    def __repr__(self):
        return "<DASU(id='%s', logLevel='%s', outputId='%s', templateId='%s')>" % (
            self.DASU_ID, self.LOGLEVEL, self.OUTPUT_ID, self.TEMPLATE_ID
        )

class TransferFunction(Base):
    __tablename__ = 'TRANSFER_FUNC'

    CLASSNAME_ID = Column(String(64), primary_key=True)
    IMPLLANG = Column(String(16), nullable=False)

    def __repr__(self):
        return "<TRANSFER_FUNC(classNameId='%s', implLang='%s')>" % (
            self.CLASSNAME_ID, self.IMPLLANG
        )

asce_iasio_association_table = Table("ASCE_IASIO", Base.metadata,
                                     Column('ASCE_ID',String(64), ForeignKey('ASCE.ASCE_ID')),
                                     Column('IO_ID',Integer, ForeignKey('IASIO.IO_ID'))
                                     )

class Asce(Base):
    __tablename__ = 'ASCE'

    ASCE_ID = Column(String(64), primary_key=True)
    TRANSF_FUN_ID = Column(String(96), ForeignKey('TRANSFER_FUNC.CLASSNAME_ID'))
    OUTPUT_ID = Column(String(64), ForeignKey('IASIO.IO_ID'))
    DASU_ID = Column(String(64), ForeignKey('DASU.DASU_ID'))
    TEMPLATE_ID = Column(String(64), ForeignKey('TEMPLATE_DEF.TEMPLATE_ID'))

    output = relationship('Iasio')
    transferFunction = relationship('TransferFunction')
    dasu = relationship("Dasu",back_populates="asces")
    template = relationship('Template_def')

    asceProps = relationship("Property", secondary=asce_props_association_table,back_populates="asces")
    inputs = relationship("Iasio", secondary=asce_iasio_association_table)

    def __repr__(self):
        return "<ASCE(id='%s', transfFuncId='%s', outputId='%s', dasuId='%s', templateId='%s', props=%s)>" % (
            self.ASCE_ID, self.TRANSF_FUN_ID, self.OUTPUT_ID, self.DASU_ID, self.TEMPLATE_ID, self.asceProps
        )

class Supervisor(Base):
    __tablename__ = 'SUPERVISOR'

    SUPERVISOR_ID = Column(String(64), primary_key=True)
    HOSTNAME = Column(String(64), nullable=False)
    LOGLEVEL = Column(String(16))

    dasusToDeploy = relationship('DasuToDeploy',back_populates="supervisor")

    def __repr__(self):
        return "<SUPERVISOR(id='%s', hostName='%s', logLevel='%s')>" % (
            self.SUPERVISOR_ID, self.HOSTNAME, self.LOGLEVEL
        )

class DasuToDeploy(Base):
    __tablename__ = 'DASUS_TO_DEPLOY'

    ID = Column(Integer, Sequence('DASU_TO_DEPLOY_SEQ_GENERATOR'), primary_key=True, autoincrement='auto')
    SUPERVISOR_ID = Column(String(64), ForeignKey('SUPERVISOR.SUPERVISOR_ID'))
    DASU_ID = Column(String(64), ForeignKey('DASU.DASU_ID'))
    TEMPLATE_ID = Column(String(64), ForeignKey('TEMPLATE_DEF.TEMPLATE_ID'))
    INSTANCE = Column(Integer)

    dasu = relationship('Dasu')
    template = relationship('Template_def')
    supervisor = relationship('Supervisor',back_populates="dasusToDeploy")

    @validates('INSTANCE')
    def validate_instance(self,key,instance):
        assert(instance>=0)
        return instance

    def __repr__(self):
        if self.ID is not None:
            return "<DASUS_TO_DEPLOY(id=%d, supervisorId='%s', dasuId='%s', templateId='%s', instance=%d')>" % (
                self.ID, self.SUPERVISOR_ID, self.DASU_ID, self.TEMPLATE_ID, self.INSTANCE)
        else:
            return "<DASUS_TO_DEPLOY(id=?, supervisorId='%s', dasuId='%s', templateId='%s', instance=%d')>" % (
                self.SUPERVISOR_ID, self.DASU_ID, self.TEMPLATE_ID, self.INSTANCE)


