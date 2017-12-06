/*
  A PROPERTY table stores PropertyDao java pojos.
  
  The OneToMany relation between tables is unidirectional 
  (as described in http://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#associations-one-to-many)
  and realized through a link table (PROPERTY_IAS)
*/
CREATE TABLE PROPERTY ( --Prop table
  id NUMERIC(15) NOT NULL, 
  name VARCHAR2(255) NOT NULL, 
  value VARCHAR2(255) NOT NULL, 
  CONSTRAINT Property_PK PRIMARY KEY ( id ));
  
/*
  The SEQUENCE to generate PROPERTY IDs
*/
CREATE SEQUENCE PROP_SEQ_GENERATOR
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 1
  INCREMENT BY   1
  NOCYCLE
  CACHE 20
  ORDER;
  
CREATE TABLE IAS (
  id NUMERIC(15) NOT NULL,
  logLevel VARCHAR2(10) NOT NULL,
  CONSTRAINT IAS_PK PRIMARY KEY ( id ));

/*
  The SEQUENCE to generate IAS IDs
*/
CREATE SEQUENCE IAS_SEQ_GENERATOR
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 1
  INCREMENT BY   1
  NOCYCLE
  CACHE 20
  ORDER;
  
CREATE TABLE IAS_PROPERTY (
  Ias_id NUMERIC(15) NOT NULL,
  props_id NUMERIC(15) NOT NULL,
  CONSTRAINT IAS_PROP_props_UQ UNIQUE(props_id),
  CONSTRAINT IAS_PROP_Prop_FK FOREIGN KEY(props_id) REFERENCES PROPERTY(id),
  CONSTRAINT IAS_PROP_Ias_FK FOREIGN KEY(Ias_id) REFERENCES IAS(id),
  CONSTRAINT IASPROPS_PK PRIMARY KEY (Ias_id, props_id));

/*
  The IASIO
*/
CREATE TABLE IASIO (
  io_id varchar2(64) NOT NULL,
  shortDesc VARCHAR2(256),
  refreshRate INTEGER NOT NULL,
  iasType VARCHAR2(16) NOT NULL,
  CONSTRAINT IASIO_PK PRIMARY KEY(io_id),
  CONSTRAINT refreshGreaterThenZero CHECK (refreshRate>0));

  /*
    The Supervisor 
   */
CREATE TABLE SUPERVISOR (
  supervisor_id VARCHAR2(64) NOT NULL,
  hostName VARCHAR2(64) NOT NULL,
  logLevel VARCHAR2(10),
  CONSTRAINT SUPERVISOR_PK PRIMARY KEY(supervisor_id));

/*
The table describing a DASU
*/
CREATE TABLE DASU (
  dasu_id  VARCHAR2(64) NOT NULL,
  logLevel VARCHAR2(16),
  supervisor_id VARCHAR2(64) NOT NULL,
  output_id VARCHAR2(64) NOT NULL,
  CONSTRAINT DASU_PK PRIMARY KEY(dasu_id),
  CONSTRAINT DASU_SUPERVISOR_FK FOREIGN KEY (supervisor_id) REFERENCES SUPERVISOR(supervisor_id),
  CONSTRAINT DASU_OUTPUT_FK FOREIGN KEY(output_id) REFERENCES IASIO(io_id));
  
CREATE TABLE TRANSFER_FUNC (
	className_id VARCHAR2(64) NOT NULL,
	implLang VARCHAR2(16),
	CONSTRAINT TFUNC_PK PRIMARY KEY(className_id));

/*
  The table for a ASCE
*/
CREATE TABLE ASCE ( 
  asce_id VARCHAR(64) NOT NULL,
  transf_fun_id VARCHAR2(96) NOT NULL,
  output_id VARCHAR2(64) NOT NULL,
  dasu_id VARCHAR2(64) NOT NULL,
  CONSTRAINT ASCE_PK PRIMARY KEY(asce_id),
  CONSTRAINT ASCE_output_FK FOREIGN KEY (output_id) REFERENCES IASIO(io_id),
  CONSTRAINT ASCE_DASU_FK FOREIGN KEY (dasu_id) REFERENCES DASU(dasu_id),
  CONSTRAINT ASCE_TRANSFUN_FK FOREIGN KEY(transf_fun_id) REFERENCES TRANSFER_FUNC(className_id));
  
  /*
  One ASCE can have zero to many properties.
  This is the link table betwee ASCE and properties
  (veery similar to the IAS_PROPERTY table)
*/
CREATE TABLE ASCE_PROPERTY (
  asce_id VARCHAR2(64) NOT NULL,
  props_id NUMERIC(15) NOT NULL,
  CONSTRAINT ASCE_PROP_Props_UQ UNIQUE(props_id),
  CONSTRAINT ASCE_PROP_Prop_FK FOREIGN KEY(props_id) REFERENCES PROPERTY(id),
  CONSTRAINT ASCE_PROP_Asce_FK FOREIGN KEY(asce_id) REFERENCES ASCE(asce_id),
  CONSTRAINT ASCEPROPS_PK PRIMARY KEY (asce_id, props_id));

/*
  This table link the ASCE to its many inputs
*/
CREATE TABLE ASCE_IASIO (
  asce_id VARCHAR2(64) NOT NULL,
  io_id  varchar2(64) NOT NULL,
  CONSTRAINT ASCE_IASIO_ASCE_FK FOREIGN KEY(asce_id) REFERENCES ASCE(asce_id),
  CONSTRAINT ASCE_IASIO_IASIO_FK FOREIGN KEY(io_id) REFERENCES IASIO(io_id));
  
