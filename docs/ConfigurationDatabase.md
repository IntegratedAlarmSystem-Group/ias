# Motivation 

The configuration database stores configuration properties of the various items of the &brvbar;AS. At the present a RDB seems to suffice.  Browsing the web for free of charge RDB, it seems that oracle and postgres are the best alternatives (MySQL passed under Oracle' wings but the community now complains that it is too slow fixing bugs and inefficient while postgres is gaining momentum). I am personally in favor of postgres but considering that we already use oracle in ALMA, the latter could be the best choice. However note that the free version that Oracle distributes comes with  the [OTN license](http://www.oracle.com/technetwork/licenses/database-11g-express-license-459621.html) that has some limitation. 

If it not too much work, a separation layer between the database and code would be great as this would allow to develop and run the CDB with postgres and deploy in operation with our licensed oracle database. Hibernate for mapping java objects to and from RDB seems the best option. Spring allow dependency-injection so it could be great to get configurations from RDB, test files and whatever else. Spring could also be useful for testing so spring+hibernate over text files (XML? Json? Comma-separated? I would not deal with XML complexity if possible) or a RDB seems the best option at the present. 

There will be 2 areas in the server: 
* Production: the database to use in operation 
* Test: a database with the configuration for testing with some limitations to keep the tool easy 
    * Only one test is executed at a time
    * The test database is cleaned before running a test
    * The test database is populated with the data provided by the user by means of text file in the test folder of the module to test
    * Possibly the test database is cleaned when the test is over 

Running the tests against a database instead of text files ensures that test follows the same pattern of the production system i.e. even reading/writing data in the configuration database. 

Internally, the CDB implementation uses hibernate whose properties are defined in `hibernate.cfg.xml`.

# Installation of Oracle Xe 
You can skip this chapter if want to use the JSON implementation only.

We follow the Oracle xe installation procedure described [here](http://www.davidghedini.com/pg/entry/install_oracle_11g_xe_on): 
* Port 8090 (as 8080 already used by jenkins) 
* Database listener port: 1521, default 
* Initial password for SYS and SYSTEM: oradbSys 
* Oracledb will start at boot 
* The user needs to source `/u01/app/oracle/product/11.2.0/xe/bin/oracle_env.sh` in the bash_profile 
* increase the number of processes: 
```
sqlplus sys/oradbSys as sysdba 
sqlplus>alter system set processes=300 scope=spfile; 
sqlplus>shut immediate; 
sqlplus>startup 
```
We suggest to use this [web page](https://docs.oracle.com/cd/E17781_01/admin.112/e18585/toc.htm#XEGSG109) as a quick oracle guide

Create a user for test (iastest and password test) and one for operations (ias and password operDb) and grant all the rights:
```
sqlplus sys/oradbSys as sysdba; 
create user iastest identified by test; 
grant CREATE SESSION, ALTER SESSION, CREATE DATABASE LINK, CREATE MATERIALIZED VIEW, CREATE PROCEDURE, CREATE PUBLIC SYNONYM, CREATE ROLE, CREATE SEQUENCE, CREATE SYNONYM, CREATE TABLE, CREATE TRIGGER, CREATE TYPE, CREATE VIEW, UNLIMITED TABLESPACE to iastest; 
```

# Tables 

The tables are created by the [CreateTables.sql](https://github.com/IntegratedAlarmSystem-Group/ias/blob/develop/Cdb/src/resources/org/eso/ias/rdb/sql/CreateTables.sql) script.

[[https://github.com/IntegratedAlarmSystem-Group/ias/wiki/img/RCDB.png|alt=Schema of the RDB CDB]]

## IAS 

This table contains global IAS properties
* id PK, string): the identifier of the IAS
* logLevel (String): the log level
* refreshRate (integer): time interval in seconds to resend a value even if its value/mode/validity did not change
* tolerance (integer): delta time (seconds) the clients expect after the refresh rate to invalidate a monitor point
* hbFrequency (integer): time interval (seconds) to periodically send the heartbeat

The log level will be used by all other components unless overridden locally i.e. a DASU logs at the minimum log level between the log level defined in the IAS and those defined in the DASU.
The log level defaults is set in the logback config file.

## TEMPLATE_DEF

The definition of a template that states the min and max number of the instances:
* template_id (PK, String): the identifier of the template
* min (integer): the min number of a possible instance
* man (integer): the max number of a possible instance

For a given template, the possible concrete instances belogn to [min, max]. See the replication chapter for further details.

## SUPERVISOR 

The supervisor is needed if we want to allow to run more than one DASU in a single JVM. In principle if we are happy with only one DASU running as a single process we could simplify the deployment. 
* supervisor_id: (PK, String) unique identifier 
* hostName: the Host to run the JVM process
* logLevel (String): the log level

## DASUS_TO_DEPLOY

The DASUS to deploy in a supervisor
* id (PK, integer): the identifier
* supervisor_id (FK, String): the supervisor to deploy the DASU into
* dasu_id (FK, string): the identifier of the DASU to deploy
* template_id (FK, string, optional): the ID of the template, if any
* instance (integer): the number of  the instance if the DASU is generated out of a template

The instance must be not null when the template_id is not null. The instance must also belong to the [min,max] interval defined in the template.

## DASU 

The following fields needs to be stored for each DASU 
* dasu_id: (PK, String) the unique identifier of the DASU
* logLevel(String): the log level
* output_id: (FK, String) the ID of the IASIO produced buy the DASU (note that it matches with the output produced by one ASCE running in the DASU)
* template_id (FK, string, optional): the ID of the template, if any

## TRANSFER_FUNC
* className_id (PK, String): the name of the class
* implLang: the implementation language of the class to run (can be either scala or java)

## ASCE 

The following fields need to be stored for each ASCE 
* asce_id: (PK, String) the unique identifier of the ASCE
* dasu_id: (FK, String) the ID of the DASU where the ASCE runs
* outputt_id: (FK, String) the ID of the IASIO produced buy this ASCE
* transf_fun_id: (FK, String) the identifier of the transfer function
* template_id (FK, string, optional): the ID of the template, if any

## PROPERTY

The user defined properties in the form name,value to pass java properties around different IAS tools.
* id: (PK, String)
* Name: (string) The name of the property
* Value: (string) the value of the property. The type of this values must be translated by the ASCE when it reads the property. 

## IASIO
* io_id: (PK, String) the unique ID of the IASIO
* shortdesc: (String) human readable description of the monitor point
* iasType: (FK) one of the types recognized by the IAS core
* docUrl (string): the URL to browse the documentation of the alarm
* canShelve (boolean, number(1)): `true` (default) if the alarm can be shelved
* template_id (FK, optional): the ID of the template, if any

# Replication by templates

Very often there is the need to monitor different instances of the same equipment. For example in ALMA there are identical antennas or identical weather stations. 
They are indistinguishable one another and must be monitored in the very same way to generate the very same set of alarms.

The CDB supports the replication in order to simplify the editing of the CDB by means of templates: the user needs to insert only the template. During the startup, the Supervisor translates a template into a concrete item so that at run time there is not distinction if an item has been generated out of a template or not.

In the scope of the IAS, the DASUs with its ASCEs, inputs and output can be templated. In the core it is not possible to distinguish if a DASU has been generated out of the template i.e. the DASUs and the ASCEs do not have any special code to be run when they are generated out of a template.
The only exception is the number of the instance that is needed in transfer function as we will explain later.

A device is represented by one or more DASUs, the ASCEs that run in each DASU, the inputs and finally the output produced by the DASUs. If we need to replicate the monitoring of a device, we need to be able to replicate DASUs, ASCEs and IASIOs (both inputs and outputs). In that case we have to add a template to each of them in the CDB.

The supervisor, knows the DASUs to instantiate and, if they are generated out of a template, it knows the identifier of the template and the instance.
The definition of the template includes the range, [min,max] of the instances that can be generated out of the template: this is mostly to catch errors at deployment time.

Replicated DASUs, ASCEs and IASIOs must all refer to the same template: one ASCE that runs in a DASU must all have the same template of the DASU; if the DASU is not-templated then the ASCEs must also be not-templated.
The output of ASCEs and DASUs is always templated otherwise it would not be possible to distinguish who produced what.

The inputs are a little bit different because even a replicated ASCE could need to receive a non replicated input: i.e. the inputs of an ASCE can be not-template or have the same template of the ASCE (and therefore the same template of the DASU).
It is also possible to define inputs that belong to different templates. Such inputs must be added in the CDB in the templated input instances. More details in [#124](https://github.com/IntegratedAlarmSystem-Group/ias/issues/124).

It is the Supervisor that generates concrete instances of DASUs when a template and a number of instance is provided. The Supervisor basically transform each identifier into a another identifier that includes the number of the instance.
At the time of writing the pattern is to transform the identifier `name` of a instance `n` of a template into `name[!#n!]`: by looking at an identifier it is immediately possible to recognize if it has been generated out of a template.

:exclamation: _sources shall never directly use the pattern of the identifier_ as it could change from one version to another. The [`Identifier`](https://github.com/IntegratedAlarmSystem-Group/ias/blob/develop/BasicTypes/src/scala/org/eso/ias/types/Identifier.scala) offers methods to deal with templates

More information about templates can be found in [Issue #80](https://github.com/IntegratedAlarmSystem-Group/ias/issues/80) and [Issue #124](https://github.com/IntegratedAlarmSystem-Group/ias/issues/124).

## Transfer function for templates

The transfer function gets a map of inputs and outputs that depends on the number of instance i.e. known only at run-time.
The typical usage to get a input is to access the map passing the identifier of the IASValue in input.

As we already said, the instance `n` of an ASCE can get non templated inputs, or templated inputs of the same instance `n` or even templated inputs of another instance (templated input instances).
The code of the TF does not need to know if a IASValue is templated or not because scala and java APIs provide a method just for that: `getValue(...)` that returns the IASValue in input being the ASCE or the input itself templated or not templated.

We _strongly_ recommend to get the inputs by calling `getValue(...)` instead of accessing the map directly.

However, there could be cases where this is not needed like the `MinMaxThreshold` that expects to receive only one input and does not really care if it is generated out of a template or not. Tis rule generally applies to generic, reusable transfer functions.

# GUI 

A GUI helps editing the production database. The test database is always populated from the test files in the test folder of the module. 

There are 2 type of user
* Manager: read/write permission
* Operator: read only permission 

To keep the project simple we hardcode the passwords and do not implement a user login + a role even if it is a very nice-to-have feature to implement sooner or later. 

The GUI shall allow to
* Add, remove, change IAS properties
* Add, remove, change, IASIOs
* Edit IASTypes (note that one type exist for each IASIO types defined in the code so this feature must be used carefully) 
* Add, remove, change supervisors
* Create a new DASU deployed in a supervisor
* Re-deploy an existing DASU in other supervisor
* Create a ASCE deployed in an existing DASU; it shall have at least one input, the output and the TF
* Edit a ASCE and its properties
* Re-deploy an existing ASCE in other DASU 

It is desirable to have some complex operation automatically supported:
* Creation of duplicated ASCE (also DASU?) where the inputs and outputs have an incremental name as described in the architecture of the IAS.
* Export/import the entire database into a set of JSON files (as we do not implement any version control at the moment, we could easily use a CVS software for saving the json files for that purpose) 

The GUI shall ensure the consistency of the data when not already provided by the database definition. 

A consistency check at system startup is highly desirable as it catches errors before running the system introduced by errors while editing or manipulating the database through the command line instead of the provided GUI. 

A fail-fast policy should be put in place here. The same consistency check must be run while inserting data and before staring the system. If the consistency check results too slow for real-time editing, then the GUI could build a in-memory model of the database adding more coding complexity. Another option could be that of mirroring the database into an in-memory database like Redis just for the purpose of editing.

If not already provided by the database definition, the consistency check must as a minimum check
* That same DASU is not deployed into more than one supervisor
* The same ASCE is not deployed into more than one DASU 

The consistency check run at &brvbar;AS boot time issues warning in case of minor inconsistencies like for a example a DASU or an ASCE not deployed because in this case the output they produce will never be updated. In case we want to allow to have undeployed elements we could define a special sink supervisor that never runs but even in this case the consistency should at least issue a info or debug log at startup. 

# CDB on JSON files 

Configuration database on JSON files is foreseen for
* Testing purposes: a set of JSON files must be written in the test folder of a module
* Import/export of the content of the RDB CDB 

As already said, as there is no version control of the content of the RDB CDB, a version control can be implemented by exporting the content of the CDB into JSON files and committing them into a [VCS](https://en.wikipedia.org/wiki/Version_control). 

Before running a test, the JSON files are flushed into the RDB: tests read configuration data from RDB the same way the tool does in operation. Consistency check with a fails-safe policy is done while injecting JSON data into the RDB. 

JSON files resemble the structure of the table we described upon and subject to change if we modify the tables. At a first glance:
* `CDB/ias.json`: array of properties for the ias in the form &lt;name,value>*
* `CDB/Supervisor/SupervisorID.json`: an entry for the supervisor having as ID the name of the file (this implicitly avoid duplication as the files system does not allow 2 files with the same name in the folder). It contains at least the name of the host to deploy the supervisor
* `CDB/DASU/DasuId.json`: Each entry contains at least the name of the supervisor that runs the DASU
* `CDB/ASCE/AsceId.json`: one entry for each ASCE with the name of the files equal top the ID of the ASCE. It contains at least the ID of the DASU that runs the ASCE, the name of the TF class, the IDs of the IASIOs in input and the output. It might also contain a array of properties in the form &lt;name, value&gt;*
* `CDB/TF/fts.json`: the definition of the transfer function
* `CDB/IASIO/IASIO.json`: a single file with all IASIOs: an array of &lt;ID, type, refreshRate>*
* `CDB/TEMPLATE/templates.json`: the definition of the templates for replication

# Using python with the CDB (oracle implementation)

Version 5 introduced a set of classes to access the CDB in the oracle server.
You need to have [cx_Oracle](http://www.oracle.com/technetwork/articles/dsl/python-091105.html) installed. You ca find [here](https://cx-oracle.readthedocs.io/en/latest/installation.html#installing-cx-oracle-on-linux) the installation instructions.
We use the [Alchemy](http://www.sqlalchemy.org/) ORM to provide a mapping between database tables and python.
You need to install the [oracle instant client](http://www.oracle.com/technetwork/database/database-technologies/instant-client/downloads/index.html) that is required by cx_Oracle; we are currently using `oracle-instantclient11.2-basiclite-11.2.0.4.0-1.x86_64.rpm`.