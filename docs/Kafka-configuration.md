# Kafka framework

The ¦AS uses [kafka](https://kafka.apache.org/) for the publisher/subscriber protocol. Kafka, in turn, depends on [zookeper](https://zookeeper.apache.org/).

Kafka has been selected because it is distributed, fast, reliable and easy to scale. 
One important feature is the possibility to keep the records in the topic for a definite amount of time that matches with the life span of the monitor point values and IASIOs circulating in the core of the alarm system.

Kafka will initially be configured in the easiest possible way unless there is a immediate reason that requires to add complexity.
For that reason, initially there is only one kafka broker.

## Ingestion of monitor point values produced by plugins

[Plugins](https://github.com/IntegratedAlarmSystem-Group/ias/wiki/Monitored-system-plugins) collect monitor point values from the monitored control software and send them to the core off the ¦AS.

All the plugins publish monitor point values and alarms red from the monitored software system in a single topic named **PluginsKTopic**. 
Assignment of records to partitions is normally based based on the unique ID of the plugin (@see [issue 9](https://github.com/IntegratedAlarmSystem-Group/ias/issues/9)) but can be overridden by setting a java property in the command line or in the configuration file.

The receivers convert the monitor point values and alarms into IASIOs that will be elaborated by the core of the alarm system. 
The receiver access the configuration database to be able to convert a monitor point value in the IASIO of the proper type.
They store in memory the configuration of each IASIO to avoid accessing the CDB too often and improve responsiveness.
The draw back is that if a receivers converts too many different type of IASIOs it could run out of memory.
The solution is to let each receiver deal with a limited set of partitions, ideally only one i.e. starting one receiver for each partition.

If the assignment of partitions to plugin is manually done, the impact in the receivers must be carefully evaluated.
