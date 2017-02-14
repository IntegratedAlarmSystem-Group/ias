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
  CONSTRAINT props_UQ UNIQUE(props_id),
  CONSTRAINT Prop_FK FOREIGN KEY(props_id) REFERENCES PROPERTY(id),
  CONSTRAINT Ias_FK FOREIGN KEY(Ias_id) REFERENCES IAS(id),
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
  CONSTRAINT refreshGreaterThenZero CHECK (refreshRate>0))
