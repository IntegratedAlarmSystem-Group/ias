/*
  A PROPERTY table stores PropertyDao java pojos.
  
  The OneToMany relation between tables is unidirectional 
  (as described in http://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#associations-one-to-many)
  and realized through a link table (PROPERTY_IAS)
*/
CREATE TABLE PROPERTY ( --Prop table
  id NUMBER(15) NOT NULL, 
  name VARCHAR2(255) NOT NULL, 
  value VARCHAR2(255) NOT NULL, 
  CONSTRAINT Property_PK PRIMARY KEY ( id ));
  
/*
  The template for replication of identical equipments
  
  The replicated  eequipments will have indexes between the min and max,
  inclusive [min, max]
*/
CREATE TABLE TEMPLATE_DEF (
  template_id VARCHAR2(64) NOT NULL,
  min NUMBER(8) NOT NULL CHECK (min>=0),
  max NUMBER(8) NOT NULL CHECK (max>0),
  CONSTRAINT maxGreaterThenMin CHECK (max>min),
  CONSTRAINT TEMPLATE_PK PRIMARY KEY(template_id));
  
  
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
  id NUMBER(15) NOT NULL,
  logLevel VARCHAR2(10) NOT NULL,
  refreshRate NUMBER(8) NOT NULL,
  tolerance NUMBER(8) NOT NULL,
  hbFrequency NUMBER(8) NOT NULL,
  bsdbUrl VARCHAR(255) NOT NULL,
  smtp VARCHAR(64) NULL,
  CONSTRAINT IAS_PK PRIMARY KEY ( id ),
  CONSTRAINT refreshGreaterThenZero CHECK (refreshRate>0),
  CONSTRAINT toleranceGreaterThenZero CHECK (tolerance>0));

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
  Ias_id NUMBER(15) NOT NULL,
  props_id NUMBER(15) NOT NULL,
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
  iasType VARCHAR2(16) NOT NULL,
  docUrl VARCHAR2(256),
  canShelve NUMBER(1), -- boolean 0/1
  template_id VARCHAR2(64) NULL,
  emails VARCHAR2(128) NULL,
  sound VARCHAR2(16) NULL,
  CONSTRAINT IASIO_TEMPFK FOREIGN KEY(template_id) REFERENCES TEMPLATE_DEF(template_id),
  CONSTRAINT IASIO_PK PRIMARY KEY(io_id));



/*
The table describing a DASU
*/
CREATE TABLE DASU (
  dasu_id  VARCHAR2(64) NOT NULL,
  logLevel VARCHAR2(16),
  output_id VARCHAR2(64) NOT NULL,
  template_id VARCHAR2(64) NULL,
  FOREIGN KEY(template_id) REFERENCES TEMPLATE_DEF(template_id),
  CONSTRAINT DASU_PK PRIMARY KEY(dasu_id),
  CONSTRAINT DASU_OUTPUT_FK FOREIGN KEY(output_id) REFERENCES IASIO(io_id));

 /*
  * Transfer functions  with the class to run
  * and its implementation language
  */
CREATE TABLE TRANSFER_FUNC (
	className_id VARCHAR2(64) NOT NULL,
	implLang VARCHAR2(16) NOT NULL,
	CONSTRAINT TFUNC_PK PRIMARY KEY(className_id));

/*
  The table for a ASCE
*/
CREATE TABLE ASCE ( 
  asce_id VARCHAR(64) NOT NULL,
  transf_fun_id VARCHAR2(96) NOT NULL,
  output_id VARCHAR2(64) NOT NULL,
  dasu_id VARCHAR2(64) NOT NULL,
  template_id VARCHAR2(64) NULL,
  FOREIGN KEY(template_id) REFERENCES TEMPLATE_DEF(template_id),
  CONSTRAINT ASCE_PK PRIMARY KEY(asce_id),
  CONSTRAINT ASCE_output_FK FOREIGN KEY (output_id) REFERENCES IASIO(io_id),
  CONSTRAINT ASCE_DASU_FK FOREIGN KEY (dasu_id) REFERENCES DASU(dasu_id),
  CONSTRAINT ASCE_TRANSFUN_FK FOREIGN KEY(transf_fun_id) REFERENCES TRANSFER_FUNC(className_id));
  
  /*
  One ASCE can have zero to many properties.
  This is the link table between ASCE and properties
  (veery similar to the IAS_PROPERTY table)
*/
CREATE TABLE ASCE_PROPERTY (
  asce_id VARCHAR2(64) NOT NULL,
  props_id NUMBER(15) NOT NULL,
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
  
  /*
    The Supervisor 
   */
CREATE TABLE SUPERVISOR (
  supervisor_id VARCHAR2(64) NOT NULL,
  hostName VARCHAR2(64) NOT NULL,
  logLevel VARCHAR2(10),
  CONSTRAINT SUPERVISOR_PK PRIMARY KEY(supervisor_id));
  
/*
  The SEQUENCE to generate DASUS_TO+DEPLOY IDs
*/
CREATE SEQUENCE DASU_TO_DEPLOY_SEQ_GENERATOR
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 1
  INCREMENT BY   1
  NOCYCLE
  CACHE 20
  ORDER;
 
  /*
   * The DASUs that the supervisor runs
   */
 CREATE TABLE DASUS_TO_DEPLOY (
 	id NUMBER(15) NOT NULL,
 	supervisor_id VARCHAR2(64) NOT NULL,
	dasu_id  VARCHAR2(64) NOT NULL,
	template_id VARCHAR2(64) NULL,
	instance NUMBER(8) NULL CHECK (instance>=0),
	CONSTRAINT DTD_FK_SUPERV FOREIGN KEY (supervisor_id) REFERENCES SUPERVISOR(supervisor_id),
	CONSTRAINT DTD_FK_DASUS FOREIGN KEY (dasu_id) REFERENCES DASU(dasu_id),
	CONSTRAINT DTD_FK_TEMP FOREIGN KEY(template_id) REFERENCES TEMPLATE_DEF(template_id),
	CONSTRAINT DTD_PK PRIMARY KEY ( id ));
	