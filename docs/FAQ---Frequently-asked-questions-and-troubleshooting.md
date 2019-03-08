This wiki page contains FAQ and trouble shooting hints collected while delivering the IAS to ALMA.
Please, if have any questions let us know and we will add it and reply to this wiki.


***
#  Where are the log files saved?
Logs are written in the console and in text file. The location is defined by the `$IAS_LOGS_FOLDER` environment variable (set by default in `$IAS_ROOT/logs`).

* Where is the log configuration?
IAS uses logback for the logging. The configuration file is in `Utils/config/ logback.xml` and installed in `$AIS_ROOT/config`.
Kafka uses log4j and for that reason IAS kafka connectors like the LTDB feeder uses log4j. 
The configuration file is `SinkClient/config/kafka-connect-log4j.xml`.

# How to change the log level of IAS tools?
IAS tools get the log level from the CDB, the command line or the default (from logback.xml) if not available.

For example the Supervisor takes the log in this order:
1. from the command line (`-x` or `--logLevel` parameter), if defined
1. from the supervisor configuration in the CDB, if defined
1. from the IAS configuration in the CDB, if defined,
1. from logback configuration file if none of the others was found

The CDB configuration is read at startup only so to change the log level of a tool you need also to restart it.

# Can we check what is published in the kafka topics?
At the present the IAS uses 3 topics: plugin, core and heartbeat. 
The values published in the three topics are non compressed, human readable json string.
Kafka provides tools for dumping the content what is published in a topic but the IAS provide a useful shortcut:
```iasDumpKafkaTopic -t topicName```
A nice colored pretty printing of the json strings is given by `jq` (you might need to install this package):
```iasDumpKafkaTopic -t topicName|jq```

# What to monitor to know if the IAS is behaving as expected?
The IAS will monitor itself (the last feature to implement at the time of writing this FAQ) and display alarms if something is not working.

* disk space used by kafka depends on the number of monitor points, synthetic parameters and DASUs; it should remain constant because data older than one hour (configurable at kafka level) are automatically deleted by the framework. 
* CPU usage: it closely depends on the number of tools deployed (Supervisors, DASU, sink clients, converters..) as well as the number of IASIOs handled by the system. A good amount of RAM, performant CPU and fast hard disk helps for sure. Redeploy components in additional servers can help as well: the IAS is scalable and distributed by design.
* RAM: at this stage IAS tools do not persist data and store data structures in memory. There is no special JVM allocation of RAM to any of the process but you might need to assign additional memory when the system grows.
* kafka performances: there are many ways to monitor kafka, this [web page](https://www.datadoghq.com/blog/monitoring-kafka-performance-metrics/) seems a good starting point


# What if the disk space used by the BSDB increases?
If the components are too slow getting data from the queues than kafka will not remove the data and the disk space increases. To debug this case you should check the statistics written in the logs. 

# What happens if a TF is too slow or fails (exception)?
The ASCE monitors the execution of the TF: if it throws an exception or its execution time is too high for a given amount of time the TF will be marked as misbehaving and not be executed anymore. 
Both cases are clearly logged and the alarm displayed in the panel will be marked as invalid.

To avoid that a misbehaving TF affects all the other DASUs and ASCEs running in the same jvm, we prefer to mark and inhibit it. At the present there is no mechanism implemented to interrupt a misbehaving TF: the counter action is taken only when it terminates.

# How often are monitor points, alarms and synthetic parameters published in the BSDB?
The follow the same rule regardless if they come from a plugin or a DASU:
* pushed immediately on change i.e. if their value, mode or validity change
* periodically if they do not change

The time interval to publish monitor points in the BSDB is customizable by setting the `refreshRate` property of the IAS in the CDB.

# Can we decrease the traffic in the BSDB without impacting performances?
If a value changes it must be pushed immediately in the BSDB to be processed in a short time in case its change triggers and alarm.
The only parameter to play with is the `refreshRate` i.e. the number of seconds a monitor point or alarms is pushed in the BSDB if it did not change.
Bigger values of refreshRate usually requires also an increase in the validity threshold i.e. the time to wait to declare a monitor point or alarm invalid. In fact, the validity threshold cannot be less than the refreshRate.

# Does the IAS produce statistics?
IAS tools logs statistics at customizable time intervals (10 mins by default) with a level of INFO.

# What can be the cause of an alarm being invalid?
In general an alarm is invalid if its value has not been refreshed or processed in time (the validity threshold of the IAs configuration in the CDB. An alarm validity depends also one th evalidity of the monitor pints that are in input to the ASCEs (TFs).
Some hinths:
* the plugin does not produce the value (is it running? is the loop executed? check the validity in the plugin topic
* the plugin does not produce the value in time (the expected time is configured in the plugin config file for each monitor point and must not be confused with the refresh rate): check the validity in the plugin topic
* is the network between the plugin and the IAS available? if not the monitor point will not be published in the plugin topic
* is the converter running? check that IASIOs produced by plugins in the plkugin topic appears in the core topic as well; if the converter is not running then all the IASIOs from the plugin will not be plublished in the core topic
* the converter works, but it is too slow: compare the timestamps of the IASIOs in the plugin topic with those (with the same ID) in the core topic against the validity threshold
* The TF that produces an alarm has been inhibited (too slow or throwing exception): check logs
* Supervisor/DASU/ASCE not running: check logs; if the Supervisor does not runn all the alarms produced by the DASUs deployed in that Supervisor will be invalid
* External causes like network down or saturated

If all the alarms are invalid in the browser than it could be that kafka is down (no disk space left for example), the web server sender is not running, no network connection.
Single point of failure like the webserver sender and the converter can be replicated.

# How to investigate bottlenecks?
IASIO have a set of timestamps updated during their life time. To check the timestamps get the JSON string from the BSDB and check the following fields: 
* readFromMonSysTStamp: The point in time when the value has been read from themonitored system (set by the plugin only)
* pluginProductionTStamp: The point in time when the plugin produced this value; this timestamp is updated when the plugin re-send the last computed value to the converter  (note that a IASIOs can be produce by a DASU or by a plugin so if this timestamp is set then dasuProductionTStamp is not set)
* sentToConverterTStamp: The point in time when the plugin sent the value to the converter
* receivedFromPluginTStamp: The point in time when the converter received the value from the plugin
* convertedProductionTStamp: The point in time when the converter generated the value from the data structure received by the plugin
* sentToBsdbTStamp: The point in time when the value has been sent to the BSDB; note that this timestamp is set by the converter when it sends the converted value to the BSDB and by by the DASU when it publishes the output to the BSDB
* readFromBsdbTStamp: The point in time when the value has been read from the BSDB
* dasuProductionTStamp: The point in time when the value has been generated by the DASU (note that a IASIOs can be produce by a DASU or by a plugin so if this timestamp is set then pluginProductionTStamp is not set)

# How to know where a IASIO come from?
The `fullRunningID` of a IASIO contains the ID of all the IAS components that manipulated such a monitor pint.
For example `"(Supervisor-Weather:SUPERVISOR)@(Dasu-WS-P:DASU)@(Asce-WS-P:ASCE)@(WS-P:IASIO)` tells us that the `WS-P` IASIO has been generated by the `Asce-WS-P` ASCE, instantiated in the `Dasu-WS-P` DASU, deployed in the `Supervisor-Weather` SUPERVISOR.

# How to know on which monitor points an alarm depend?
The IASIOs published in the core topic have a list, `depsFullRunningIds`, of the fullRunningIDs of all the monitor points used by the ASCE to calculate the output.

# How to check if there are errors in the CDB?
The `iasCdbChecker`  can be used to test the correctness of the configuration.
The checker produces a lot of information in the output: you might want to restrict the log level to warning.

# Is there a way to push manually a value in a kafka topic?
You can use the DummyPlugin or the MultiDummyPlugin for that.
They offer command line to set the value, operational mode and validity of monitor points

# Can the IAS run in a simulated environment?
We are deploying with docker containers (see ias-contrib repo), so you could use dockers to deploy in your simulated environment. There are two plugins that simulate the generation of monitor points, the DummyPlugin and the MultiDummyPlugin.
You can also run the IAS in virtual machines (on top of dockers if you like) to duplicate the production environment. Production plugin could send monitor points to the simulated environment by passing the kafka server of the simulated system instead of the one of the production.
If you want to check what a plugin produces using a simulated environment you must pass to the plugin the kafka server of the simulated environment and check what the plugin pushes in the plugin kafka topic for example.

If you want to check a new TF you can write tests that do not require the entire IAS loop (there are many example in the `TransferFunctions` module). If you change the deployment in the CDB you can start the supervisors in the simulated environment and let plugins push IASIOs in the kafka server of the simulated environment.
There are many use cases for testing. Using virtual machines with or without dockers for the deployments could help.

# Do the IAS tools communicate point to point?
IAS tools do not communicate directly but are decoupled by the kafka BSDB. The ports are 9092 for kafka and 2181 for zookeeper.

# What are the network requirements for running the IAS?
External tools need to be able to send data to kafka, including plugins so port 9092 and 2091 must accept incoming connections.
Python plugins communicate to the java counter part via UDP (the port is part of the configuration). Usually python plugins run in the same server of their java counterpart: it is the java part that connects to kafka.

The WebServerSender sends alarms to the web server with websockets port 8000. The URI (customizable with a java property) is ws://localhost:8000/core/ by default (i.e it assumes that the server runs in the same host of the web server sender).
Other tools might access different ports but these are mostly out-coming connections like the SMTP port of the email sender or the port of the LTDB feeder.
Up to know we did not ask IT of any special setup to run the IAS servers.

# What does the _"The coordinator is not aware of this member"_ error means and how to fix?
This errors shows up if the kafka consumer spends too much time in the `poll` i.e. if it is too slow processing events.
Apart of investigating why `poll` takes too long to terminate, there are 2 parameters in the kafka settings regarding this issue:
- `max.poll.interval.ms` basically allows` poll` to last longer. This must be tuned when you know that poll needs long time
- `max.poll.records`: limit the numbers of records returned to the client so that it can process them faster in the `poll`