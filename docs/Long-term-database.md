# Kafka connector to store IASValues in the Long Term DataBase (LTDB)
`LtdbKafkaConnector` is a kafka connector in charge of saving IAS values in the LTDB. In this version it is supposed to run as standalone but can be extended to run distributed. It is composed of only one task that consumes all the IASIOs from the core topic.
There is no reason to process monitor points in the plugin topic because they are converted in IASValues and put in the core topic.Extending the `LtdbKafkaConnector` to process also monitor points from the plugin topic should be straightforward.

It saves all the values published in the core topic of the BSDB into Cassandra. Feeding the IAS with values read from Cassandra would allow to re-play real situation and can be a useful tool for debugging. If needed it can be implemented by a tool that reads IASValues from the LTDB and push them directly in the core topic of the BSDB so that they are processed by the DASUs.

The kafka connector saves all the `IASValues` without any further computation. It is possible to associates a time to live (TTL) to each record stored in the database: cassandra takes care of removing records older than the associated TTL in hours. A value of 0 disables the TTL and records will never be removed from the database. Cassandra marks deleted records (tombstone) but do not effectively remove them; as a consequence the data store does not shrink after a delete. The data store is freed after running the compaction for all the tombstones older than `gc_grace_seconds` table property, set to 10 days by default. Such a long time is to give time for bringing back online failed nodes. To save disk space this configuration parameter must be configured until the LTDB will be moved in the production cassandra servers.

Configuration parameters are passd to the kafka connector in 2 property files, `connector.properties` and `standalone.properties`.

This is an example of `connector.properties`:
```
name=LTDB-Cassandra-Connector
connector.class=org.eso.ias.sink.ltdb.LtdbKafkaConnector
tasks.max=1
cassandra.contact.points=192.168.139.131
cassandra.keyspace=IntegratedAlarmSystem
cassandra.ttl=1
topics=BsdbCoreKTopic
task.stats.time.interval=10
```
where
* name: the name of the connector
* connector.class: the name of the class implementing the kafka connector
* tasks.max: the max number of tasks to consumes items
* cassandra.contact.points: the address of cassandra nodes to contact
* cassandra.keyspace: cassandra keyspace to use for storing IASIOs
* cassandra.ttl: time to leave (TTL) in hours; if <=0 TTL is disabled
* topics: the topics to consume values
* task.stats.time.interval: the time interval to log statistics in minutes; if <=0 no statics are logged

This is an example of `standalone.properties`:
```
offset.storage.file.filename=/tmp/LtdbStorageFile
plugin.path=/home/ialarms/IasRoot
bootstrap.servers=localhost:9092
type.name=kafka-connect
value.converter=org.apache.kafka.connect.storage.StringConverter
key.converter=org.apache.kafka.connect.storage.StringConverter
log.level=DEBUG
offset.flush.interval.ms=15000
```
where:
* offset.storage.file.filename: storage file name
* plugin.path: the folder that contains connector classes (it points to the parent actually)
* bootstrap.servers: kafka brokers
* type.name: the name of the type
* value.converter: the converter to get the value of a record (IAS values are strings)
* key.converter: the converter to get the key of a record (IAS keys are strings)
* log.level: log level
* offset.flush.interval.ms: time interval to flush in milliseconds

The LTDB kafka connector saves in a buffer all the records consumed from the BSDB. A thread concurrently pulls record out of the buffer and stores them in the LTDB. When the flush is invoked all the remaining records in the buffer are stored in the LTDB.
Note that kafka expects that the `put` and `flush` methods completed in a definite time interval (5 seconds by default) otherwise the connector is marked as malfunctioning. In the actual implementation. This is not a problem for the `put` method that vas the IOASIOs in the buffer; it can be a problem for the `flush` for this reason the execution time of the `flush` is monitored and logged by the statistics.

`iasLTDBConnector` bash scritpt starts the tool reading the configuration written in the 2 property files we described upon. Those files are installed in the `config` folder.
# Installation and setup of Cassandra

The LTDB is implemented with the [Cassandra](http://cassandra.apache.org/) version 3.11.x. There are many other alternatives but Cassandra is already used in ALMA for the monitoring system. In this release, Cassandra will be installed in one single dedicated server i.e. a very simple single data center, single rack, single node configuration. You might need to customize the configuration if you want to install in more complex configurations.

To install Cassandra on CentOS 7, we follow the installation instruction provided in the [download page](http://cassandra.apache.org/download/). We added the cassandra repo and install with yum.

First add the cassandra.repo in `/etc/yum.repos.d/`:
```
[cassandra]
name=Apache Cassandra
baseurl=https://www.apache.org/dist/cassandra/redhat/311x/
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://www.apache.org/dist/cassandra/KEYS
```

The install with
```
yum install cassandra
```
You need to accept the key.

Cassandra configuration is installed in `/etc/cassandra/conf` that points to ` /etc/alternatives/cassandra`.
We customized `cassandra.yaml` as follows:
* cluster_name='IAS-LTDB cluster'
* seeds: "192.168.139.131"
* listen_address: 192.168.139.131
* rpc_address: 192.168.139.131

where 192.168.139.131 is the IP of the cassandra server.

Finally start the server and ensure it is starts at boot:
```
> service cassandra start 
> chkconfig cassandra on
```

To check if the server is up and running, start the cassandra shell and get the description of the cluster. You should see something like this:
```
> cqlsh
Connected to IAS-LTDB cluster at 127.0.0.1:9042.
[cqlsh 5.0.1 | Cassandra 3.11.3 | CQL spec 3.4.4 | Native protocol v4]
Use HELP for help.
cqlsh> DESCRIBE CLUSTER;

Cluster: IAS-LTDB cluster
Partitioner: Murmur3Partitioner
```

Cassandra writes logs in  `/var/log/cassandra/` and stores data in the folder pointed by the `data_file_directories` property of the `cassandra.yaml` configuration file (`/var/lib/cassandra/data` by default).

## Keyspace

IAS stuff goes in the "IntegratedAlarmSystem" keyspace:
```
cqlsh> CREATE KEYSPACE IntegratedAlarmSystem WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
cqlsh> DESCRIBE KEYSPACE IntegratedAlarmSystem

CREATE KEYSPACE integratedalarmsystem WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}  AND durable_writes = true;
cqlsh> USE integratedalarmsystem;
cqlsh:integratedalarmsystem>
```

At the present there is only one node: `SimpleReplication` with a factor of 1 would suffice. You might want to customize the replication for different cassandra topologies.

## Queries

Cassandra will contain all the IASIOs (i.e. `IASValue`s) published in the core topic. It is a time series as the producers, DASUs and plugins, pushed timestamped records in the Kafka core topic on change and periodically.

In production we foresee the following queries
* Q1: get all the IASIOs produced in a given time interval
* Q2: get all the records of a IASIO with a given identifier
* Q3: get all the records of a IASIO with a given identifier in a given time interval
* Q4: get the activation state of an alarm at a given point in time (alarm status)
* Q5: get all the alarms that were active at a given point in time (snapshot)

where the time interval is the production time of the IASIOs and not the insertion time of the record in the LTDB.

The records can have a time to leave so that cassandra can automatically remove the records after a customized time interval. At least in the beginning while cassandra runs in a virtual machine this parameter allows to keep disk usage under control.

Each records is composed of the IASValue with its identifier, the timestamp and the TTL. The IASIO is pushed  in the kafka topic as human-readable JSON string where human readable means the fields like the timestamps internally represented by longs are formatted ISO-8601 strings.

There are 2 alternatives for storing IASIOs in the database: save the JSON strings read form the kafka core topic or save each property of the IASIOs in a column. 
In the former case we need to extract the production time (from DASU or plugin) and the ID to store together with the original JSON string. In this case the records have three columns: ID, timestamp and the JSON string.
In the latter, the JSON string must be converted in a `IASValue` and each property mapped in a column of the LTDB. This is more coupled to the actual implementation of the `IASValue` because every property modified, added or removed from the `IASValue` triggers a change in the table definition of the database. At the present in the `IASValue` there are 16 properties, including the map of properties.

From the requirements, the number of alarms and synthetic parameters is 60K. The difference between alarms and synthetic parameters is only the type of the value so we can treat them in the same way for this discussion.

The length of the JSON string published in the kafka core topic depends on several factors that change at run time and might also depend on the deployment, in particular the user defined properties and the full running Id of the dependents are difficult to estimate. It is also difficult to know the length of the value in the case of IASIO of type string: the association of antennas to pads is a IASIO of type string with a length of 640 chars however, being provided by a plugin it is not the worst case (the length of the JSON representation of this IASIO is about 1140 bytes).
The longest JSON string representing a IASIO that we have been able to get from the core topic is about 1170 bytes so for the sake of this discussion and to have a reasonable margin, we consider that the length of the JSON strings is 1500 bytes. 

To store in the LTDB a record with the JSON string we can estimate to 1500 bytes for the JSON string itself, plus the timestamp (64 bits signed integer), plus the ID of the IASIO plus another timestamp for the TTL (64 bits). Being generous with the length of the ID, we can estimate that cassandra needs 1600 bytes for each record if we save the whole JSON string published in the core topic.

The length of a record mapping each property of the IASIO in a column of the database is much shorter because JSON contains the name of each property, dates are saved as 64 bits integers, enumerated like reliability, mode and type require only one byte. We can estimate the length of the worst case from the 1600 bytes calculated before and subtracting the overhead we just mentioned. A reasonable estimation for this case is 1000 bytes.

Cassandra 3 has a limit of 2 billion (`2*10^9`) column per row i.e. 2 billion cells per partition.
IASIOs are pushed in the BSDB on change and  periodically as defined in the refresh rate property of the IAS in the CDB. Actually the refresh rate is 3 seconds that means that at least each IASIO is pushed in the BSDB 20 times per minute i.e. 1200 times per hour, 28.800 times per day, 864K times per month (aprox) and 10.5 million times per year that is much less then the maximum allowed number of cells allowed by Cassandra. According to the requirements such figures must be multiplied by 60K, all the possible IASIOs managed by the Alarm system. Without entering in Cassandra calculation that we will do after, saving data per month would require about 51 billion values that is much over the limit. Partitioning by day requires 1728million values that is already too close to the limit. Partitioning by time requires 72 million entries that is far less than the cassandra limit but we still have to check the number of values effectively used by cassandra that depends on the table structure.

The table to store IASIOs with their JSON string representation is:
```
CREATE TABLE iasio_by_day (
  iasio_id text, 
  date text, 
  event_time timestamp, 
  json text, 
  PRIMARY KEY ((iasio_id, date), event_time) ) 
  WITH CLUSTERING ORDER BY (event_time DESC);
```

where `iasio_io` id is the identifier of the IASIO, `date` is the date when the IASIO have been inserted, `event_time` is the production time (from plugin or DASU) and `json` is the JSON string representing the IASIO.
The key is compounded with the identifier and the date. In this way the key will group the all the IASIOs of the same day in a single row.
Rows are ordered by event_time so that the latest produced IASIOs is is on top of the table and records removed by the TTL, if used, are at the bottom of the table.
We can tune the date to save IASIOs by hour if there too many in one day.

A possibility to insert all the fields of an IASIO is to use the JSON support provided by cassadra. The JSON string of the IASIO published in the BSDB can be used to easily insert a IASIO using the `INSERT INTO table_name JSON ..` feature of Cassandra.
```
CREATE TYPE integratedalarmsystem.iasio (
    fullrunningid text,
    valuetype text,
    iasvalidity text,
    mode text,
    props map<text, text>,
    depsfullrunningids set<text>,
    dasuproductiontstamp timestamp,
    readfrombsdbtstamp timestamp,
    senttobsdbtstamp timestamp,
    convertedproductiontstamp timestamp,
    receivedfromplugintstamp timestamp,
    senttoconvertertstamp timestamp,
    pluginproductiontstamp timestamp,
    readfrommonsyststamp timestamp,
    value text
);
CREATE TABLE iasio_by_day (
    iasio_id text,
    date text,
    event_time timestamp,
    value FROZEN<iasio>,
    PRIMARY KEY ((iasio_id, date), event_time) )
    WITH CLUSTERING ORDER BY (event_time DESC);
```

With this definition we can easily insert JSON strings read from the BSDB with `INSERT` statements like this:
```
cqlsh:integratedalarmsystem>  INSERT INTO iasio_by_day JSON '{"iasio_id":"jsonID2", "date":"2018-10-20", "event_time":"2018-10-19T12:22:01.2", "value":{"value":"CLEARED","sentToBsdbTStamp":"2018-10-19T15:04:08.650","dasuProductionTStamp":"2018-10-19T15:04:08.469","depsFullRunningIds":["(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P-WindSpeed15:DASU)@(Asce-WS-P-WindSpeed15-Inhibitor:ASCE)@(WS-P-WindSpeed15:IASIO)","(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P-WindSpeed30:DASU)@(Asce-WS-P-WindSpeed30-Inhibitor:ASCE)@(WS-P-WindSpeed30:IASIO)","(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P-WindSpeed20:DASU)@(Asce-WS-P-WindSpeed20-Inhibitor:ASCE)@(WS-P-WindSpeed20:IASIO)"],"mode":"SHUTTEDDOWN","iasValidity":"RELIABLE","fullRunningId":"(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P-WindSpeed:DASU)@(Asce-WS-P-WindSpeed:ASCE)@(WS-P-WindSpeed:IASIO)","valueType":"ALARM"}}';

cqlsh:integratedalarmsystem> SELECT * FROM iasio_by_day;


 iasio_id | date       | event_time                      | value
----------+------------+---------------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  jsonID2 | 2018-10-20 | 2018-10-19 12:22:01.002000+0000 | {fullrunningid: '(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P-WindSpeed:DASU)@(Asce-WS-P-WindSpeed:ASCE)@(WS-P-WindSpeed:IASIO)', valuetype: 'ALARM', iasvalidity: 'RELIABLE', mode: 'SHUTTEDDOWN', props: null, depsfullrunningids: {'(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P-WindSpeed15:DASU)@(Asce-WS-P-WindSpeed15-Inhibitor:ASCE)@(WS-P-WindSpeed15:IASIO)', '(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P-WindSpeed20:DASU)@(Asce-WS-P-WindSpeed20-Inhibitor:ASCE)@(WS-P-WindSpeed20:IASIO)', '(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P-WindSpeed30:DASU)@(Asce-WS-P-WindSpeed30-Inhibitor:ASCE)@(WS-P-WindSpeed30:IASIO)'}, dasuproductiontstamp: '2018-10-19 15:04:08.469000+0000', readfrombsdbtstamp: null, senttobsdbtstamp: '2018-10-19 15:04:08.650000+0000', convertedproductiontstamp: null, receivedfromplugintstamp: null, senttoconvertertstamp: null, pluginproductiontstamp: null, readfrommonsyststamp: null, value: 'CLEARED'}


```

The identifier needs to be extracted from the `IASValue` and the event_time is the production time from DASU or plugin. The value is the JSON string read from the BSDB.

We need to compare the number of cells used by the 2 different tables to check if the row will have less the 2 billion cells, using the formula suggested by Jeff Carpenter and Eben Hewitt in _Cassandra the definitive guide_: 
N<sub>v</sub>=N<sub>r</sub>(N<sub>c</sub>-N<sub>pk</sub>-N<sub>s</sub>)+N<sub>s</sub>

From the figure calculated before, there are N<sub>r</sub>=72M IASIOs per hour. This number came assuming that each monitor point is refreshed every 3 seconds but without considering that a monitor point is sent also when its value changes. It is difficult to estimate how often monitor points changes in operations because some of them depends on sensors and can change very often. Usually the plugins refresh the value at a given rate that depends on the specification of the device. 

N<sub>pk</sub>=2 because in both solutions there are 2 primary keys, `iasio_id` and `date`. N<sub>s</sub>=0 because there are no static columns. With this numbers we can get the size of the partition in the 2 cases:
* table with JSON string: N<sub>v</sub>=72M*(4-2-0)+0=72M*2=144M
* table with all the fields of the IASIO: N<sub>v</sub>=72M*(19-2-0)+0=1224M

Both are below the 2 billion limit of Cassandra but grouping by hour is probably risky because in our calculation we did not consider the publications of values on change that partially sums up to the standard refresh rate. The former solution requires considerably less values of the latter. Both allows to get the JSON string of the IASIO but the latter allows to get all the fields of a IASIO by a simple query.
For this reason we prefer to adopt the latter solution but grouping the values per minute to stay in the safe side untile we can evaluate in operation the impact of updates of monitor points when their value changes. 

In the same book there is a complex formula to estimate the disk space from which we got these figures
* table with JSON string: ~116Gb/hour
* table with all the fields of the IASIO: ~95Gb/hour

The explanation is mostly due to the fact the data types like timestamps require only 64 bytes and the length of text fields is mush less than the length of the entire JSON file. In the latter case, the JSON is reconstructed by JSON feature of cassandra.

### Table definition for IASIOs

```
CREATE KEYSPACE integratedalarmsystem WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}  AND durable_writes = true;

CREATE TYPE integratedalarmsystem.iasio (
    fullrunningid text,
    valuetype text,
    iasvalidity text,
    mode text,
    props map<text, text>,
    depsfullrunningids set<text>,
    dasuproductiontstamp timestamp,
    readfrombsdbtstamp timestamp,
    senttobsdbtstamp timestamp,
    convertedproductiontstamp timestamp,
    receivedfromplugintstamp timestamp,
    senttoconvertertstamp timestamp,
    pluginproductiontstamp timestamp,
    readfrommonsyststamp timestamp,
    value text
);

CREATE TABLE integratedalarmsystem.iasio_by_day (
    iasio_id text,
    date text,
    event_time timestamp,
    value frozen<iasio>,
    PRIMARY KEY ((iasio_id, date), event_time)
) WITH CLUSTERING ORDER BY (event_time DESC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND crc_check_chance = 1.0
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99PERCENTILE';
```

### Q1: : get all the IASIOs produced in a given time interval

```
select * from iasio_by_day where event_time >= '2018-10-19 11:18:01' and event_time <= '2018-10-19 11:18:02' allow filtering;
```

### Q2: get all the records of a IASIO with a given identifier
```
select * from iasio_by_day where iasio_id='jsonID'  allow filtering;
```

### Q3: get all the records of a IASIO with a given identifier in a given time interval
```
select * from iasio_by_day where event_time >= '2018-10-19 11:18:01' and event_time <= '2018-10-19 11:18:02' and iasio_id='jsonID' allow filtering;
```
### Q4: get the activation state of an alarm at a given point in time (alarm status)
```
select * from iasio_by_day where  iasio_id='jsonID3'  and event_time<='2018-10-23 11:18:07' limit 1 allow filtering;
```
### Q5: get all the alarms that were active at a given point in time (snapshot)

For supporting this last query, we need to introduce a materialized view:
```
create materialized view iasio_by_time 
as select iasio_id, event_time, value 
from iasio_by_day where iasio_id is not null and event_time is not null and date is not null 
primary key ((iasio_id), date, event_time) WITH CLUSTERING ORDER BY (date DESC, event_time DESC);
```

The query to get the last values of all the IASIOs at a given point in time is 
```
select iasio_id,event_time from iasio_by_time  where event_time<='2018-10-23 11:18:07' per partition limit 1 allow filtering;
```

