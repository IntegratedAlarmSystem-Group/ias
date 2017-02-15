/*
  A PROPERTY table stores PropertyDao java pojos.
  
  The OneToMany relation between tables is unidirectional 
  (as described in http://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#associations-one-to-many)
  and realized through a link table (PROPERTY_IAS)
*/
CREATE TABLE PROPERTY (
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
  The table for a ASCE
*/
CREATE TABLE ASCE ( 
  asce_id VARCHAR(64) NOT NULL,
  tfClass VARCHAR2(96) NOT NULL,
  output_id VARCHAR2(64) NOT NULL,
  CONSTRAINT ASCE_PK PRIMARY KEY(asce_id),
  CONSTRAINT output_FK FOREIGN KEY (output_id) REFERENCES IASIO(io_id));
  
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
  
  