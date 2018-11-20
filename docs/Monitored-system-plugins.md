# Getting values from a monitored system 

A remote system sends monitor point values and alarms to the &brvbar;AS for processing. 
Retrieving such values must be done through the API that the remote system itself provides and closely depends on its architecture and middleware so it is not possible to provide a generic software tool that does the value retrieval for all the possible systems.
On top of that, some of the monitored system allows to deploy software, others only allow to poll for values, others only provide logs and so on. This means that for each monitored system the user must implement a way to collect monitor point values and alarms out of it.

The &brvbar;AS provides an API, in the `plugin` module, to
* filter monitor point values and alarms against noise
* send the values to the &brvbar;AS core for processing

Each plugin has its own unique identifier that allows the &brvbar;AS core to understand who is the producer of a given monitor point value or alarm. The ID of the plugin must also be saved in the &brvbar;AS configuration database to let it know of the remote software system.

## Supported data type

The plugins support all the primitive data types plus the alarm (with the `AlarmSample` java class).

Support for complex data type like user defined objects or arrays of primtives is not yet supported (@see [Issue18](https://github.com/IntegratedAlarmSystem-Group/ias/issues/18))

## Filtering values

To ease the work of the developer, the API allows the developer to push the monitor point value or alarm when it is provided by the monitored system without cleaning for noise: the &brvbar;AS takes care of mitigating the noise by applying the user specified filter selected from a library of available filters.

## Sending monitor values to the &brvbar;AS core

A monitor point value or alarm, after being filtered, must sent to &brvbar;AS core on change or, if its values does not change, at the latest when a specified time interval elapses. The latter ensures that the source is still alive and the value up-to-date.

Sending monitor point values and alarms to the &brvbar;AS happens in two times: monitor point values and alarms are initially sent to the BackStage DataBase (BSDB) and later on converted into a common data type (the Integrated Alarm System Input/Output, or IASIO) understood by the &brvbar;AS core. 
The plugin API takes care of sending monitor point values and alarms to the BSDB while the conversion of such values into IASIOs is performed by other software tools.

The plugin API transparently sends filtered monitor point values and alarms to the BSDB: no knowledge of low level details is requested to the developer. An abstraction layer allows to transparently change the implementation of the sending. The only requested piece of information that the developer must provide is the host and port to send data to but the way they will be used is out of his/her control.

The plugin collects the monitor point values collected during a time interval (the throttling time): if the value of a monitor point is refreshed during the throttling time, only the last value will be sent to the core of the &brvbar;AS. The sending is triggered at definite time intervals or when the max number of items in the buffer is reached. However, note that depending on the adopted message passing framework, the buffering can be done by framework itself so the collected monitor point values can be sent one by one or all in one single message: the former allows to take advantage of the buffering offered by the framework.

Monitor points and alarms read from the monitored system and filtered for noise reduction are sent as strings. Each message is composed of the following fields:
* plugin ID
* monitored system ID
* timestamp when the message is published
* list of monitor point values and alarms produced by the monitored system each of which has the following fields
  * timestamp when the sample has been provided by the monitored system
  * timestamp when the samples have been submitted to the filter and the value to send to the IAS has been produced
  * the value (string) of the monitor point or value

## Plugin configuration

The configuration of a plugin consists at least of
* the unique identifier of the plugin
* the identifier of the monitored system
* the identifier, filter and sending time interval of each of the monitor point values and alarms it collects from the monitored system and send to the BSDB
* the refresh rate (seconds, defaults to 5) to automatically send the last computed value of a monitor point or alarm
* the host and port to send the values to the BSDB
* a user defined set of properties in the form key,value

All those values need to be send to the plugin API. For plugins collecting a few number of monitor point values they can be hardcoded but for more complex situation it is better to have the configuration saved somewhere and read at startup or when convenient for the implementation.

There are cases of plugins that cannot access the &brvbar;AS Configuraton DataBase (CDB) for example those that cannot access the network where the &brvbar;AS CDB runs so it does not make sense to provide a plugin configuration into the CDB.
A plugin that runs as a component of a monitored system generally stores its configuration the configuration database of the monitored system. For example, a ALMA Common Software (ACS) Component its configuration in the ACS configuration database.

The &brvbar;AS plugin API allows the developer to hardcode the configuration or store it as his/her prefers. The only support provided by the plugin API is the reading of the configuration from a user provided JSON file.

The user defined properties red from the JSON configuration file are saved in the System properties if not already present (i.e. java properties passed in the command line override the user defined properties set in the JSON configuration file).

### Configuration in a JSON file

The configuration can be passed to the &brvbar;AS plugin API with a single JSON files with the following format:
* plugin ID: the unique ID of this plugin i.e. there is only one plugin in the &brvbar;AS with this ID
* sink server host and port: the host and port to send monitor point values and alarms to i.e. the BSDB; they way the host and port will be finally used depends on the adopted framework, if any
* an array of user defined properties composed of key and value
* the name of a global filter to apply of all the monitor points
  * global options to the filter: a string of comma-separated to pass as they to the global filter
* time interval in secs: the values are sent to the &brvbar;AS on change or after this time interval elapses
* an array of monitor point values and alarms read from the monitored system and forwarded to the &brvbar;AS; each values is composed of
  * unique ID of the value
  * refresh rate (msecs): the refresh rate at which the monitored system or the devices produces values
  * the name of the local filter to apply, if any; the API provides a set of predefined filters in the filters package
    * local options to the filter: a string of comma-separated options to pass as they are to the filter

There is no need to specify the type of the value at this stage: it will be required later when converting the value into a IASIO the, in fact, contains the type of the value in the configuration. A type mismatch will be detected by reading the logs of the type converter; a mismatched value will be discarded by the converter and never sent to the 
core.
A type mismatch is detected by the plugin if the filter to apply is incompatible with the monitor point value for example trying to average a string.

It's possible set a global filter and a global filter option for all the monitor points instead of set the local filter in every monitor points, the plugin configuration give the priority to the local filter first then check the global ones.

Examples of valid and not valid JSON configuration files can be read from `plugin/test/resources/org/eso/iasplugin/config/test/jsonfiles`.

The time interval for the plugin is used to periodically send the value of a monitor point if its value did not change. Before sending, the validity is evaluated against the actual time stamp.
In addition, each monitor point has a refresh rate in the configuration that is the rate at which the monitored system or the device produces a new sample. The implementation of the plugin, is supposed to offer a new value before the refresh rate elapses otherwise the monitor point will be marked as invalid when sent to the BSDB.

### Runtime configuration

It is a developer task to retrieve the samples of monitor point and alarms from the monitored system and feed the plugin; it is a plugin task to get the filtered values and send them to the IAS core at the proper time interval.
Internally the plugin sets a pool of threads for that.
The number of threads in the pools is generated at startup depending on the number of available CPUs unless differently set in the `org.eso.ias.plugin.scheduledthread.poolsize` java property.

The table summarize the available java properties for the plugin (`org.eso.ias.plugin` prefix omitted for readability):

| Name | Default | Type | Meaning |
| ---- | ------- | ---- | ------- |
| `scheduledthread.poolsize` | (#available processors)/2 | int>0 | The number of threads for getting and sending values to the IAS core |
| `stats.frequency` | 10 | int>=0 | The time interval in minutes to log statistics (0 disabled) |
| `stats.detailed`| `false` | boolean | If `true`enables the generation of detailed statistics |
| `throttling` | 500 | 100<=int<=1000 | The throttling time (msec) to send monitor point values to the IAS |
| `buffersize` | `131072` | int>0 | The max number of monitor point values to buffer in the throttling time interval |
| `validity.delta` | 500 | long>0 | The delta time in msec to add to the refresh rate when deciding if a values is reliable or not |
| timeinterval | 5 | Integer>0 | The time interval (seconds) to automatically re-send values | 

## Notes for developers

There is no need to build and install the whole &brvbar;AS to write plugin: the required jars can be taken from a &brvbar;AS build and added to the plugin classpath. This is to minimize the impact of plugins running as part of monitored software system. For the same reason, the plugin API is written entirely in java to not require a scala installation in the remote software system.

The required jars include not only those implementing the plugin API but also those required by external tools. For example if the sending of data is done with the help of a middleware there could be the need to add to the java classpath of the plugin some third-party jars. This is the only place where a third-party dependency will be visible: changing the midllware will require to change the classpath but will not require to rebuild the plugin whose source code is independent of the middleware.

### Filtering

The `plugin` module provides a set of filters in the `org.eso.ias.plugin.filter` package.
If the user wants to use one of that filter, it must be passed at construction time or the name of the class (without the package) provided in the configuration file.

Each filter implements the `Filter` interface and extends the `FilterBase` abstract class that provides useful methods to handle the history.

Each time a new sample is submitted, it is added to the history and passed down to the implementation if it needs a customization.
This filter is finally applied when the `apply` method is invoked and it returns the value together with a snapshot of the history, i.e. the samples, used to generate the new value. The history can be important for time based filtering i.e. those filters that for example averages the returned value from the sample received in a defined time interval.

The final implementation of the filter, must take care of keep the size of the history under control: it cannot be done by the base class because it closely depends on the algorithm of the filter. But the base class provides methods to shorten the size of the history to a given number of samples or the most recent samples.

We kindly suggest implementers of filters to look at the existing filters in the [org.eso.ias.plugin.filter](https://github.com/IntegratedAlarmSystem-Group/ias/tree/master/plugin/src/java/org/eso/ias/plugin/filter) package at least to `FilterBase` and `NoneFilter` that is simplest filter in the library.

#### NoneFilter
If the user does not provide a filter to apply, the `NoneFilter` is used by default.
This filter simply returns the last sample provided by the monitored system.

#### AverageBySamples
`AverageBySamples` return the average of the last `n` received samples. The number of samples is a positive integer greater than 1, passed in the property of the filter.

#### AverageByTime
`AverageByTime` is very similar to `AverageBySamples` but returns the average of the samples received in the last time frame. The time frame is a positive long (milliseconds) greater than 0, passed in the property of the filter.

### Replication (@see [#81](https://github.com/IntegratedAlarmSystem-Group/ias/issues/81))
In the real world it happens to have identical devices. The core supports the replication of identical plugin to get monitor point values and alarms from identical devices.
The same configuration provided for standard plugins can be reused for replicated plugins: what makes the difference is the number of the instance passed in the constructor of the `Plugin`. In such a case the ID of the plugin and those of the monitor points sent to the BSDB are remapped to contain the number of the instance by delegating to the scala `Identifier`.
The user defined part that gets values from the monitored device must be aware of which device to connect to for each instance. It can be a command line parameter for example. But this is the only difference for example the IDs of the collected monitor points to send to the BSDB does not change because the magic is done by the `Plugin`.
The advantage of the replication is that only one configuration must be written for all the replicated instances reducing the effort in configuring and reducing the risk of errors.

### Publishing monitor point values

The plugin provides a buffered and unbuffered version of the publishers. In this context the buffering means that more monitor point values are encapsulated in one single message instead of being sent one by one. The latter implies more network messages but allow to take advantage of the buffering and optimization of the adopted framework.

The plugin provide the following publishers, the choice to which one to use is left to developer by dependency injection in the constructor (i.e. switching from one publisher to another does not require changes in the plugin source code):
* Listener: monitor point values to be published are sent to a listener
* FilePublisher: write monitor points on file
* JsonFilePublisher: write monitor points on a JSON file
* KafkaPublisher publishes monitor points to a kafka topic

Some of the provided publisher have a buffered and a unbuffered version.

##### Kafka publisher

Kafka publishers send monitor point values to a kafka topic, `PluginsKTopic` by default: the same kafka topic is used by all the plugins, but each plugin uses its own partition automatically assigned by Kafka depending on the unique ID of the plugin. It is also posisble top assigna specific partifion by setting a java property in the configuration file or in the command line.

In addition to the plugin configuration file, kafka is configured by a set of properties that can be passed in the command line or set in a kafka configuration file whose default is `resources/org/eso/ias/plugin/publisher/impl/KafkaPublisher.properties`.

The reason for let the plugins use the same topic but a dedicated partition is that kafka associates one consumer (of each consumer group) to one partition.
In that way each consumer deals with the monitor point values sent by only one plugin: it does not need to load the configuration of all the possible monitor point values but only the configuration of the monitor point values generated by the specific plugin.

Kafka `key.serializer and `value.serializer` forced to be `org.apache.kafka.common.serialization.StringSerializer` because the plugins always send formatted strings (JSON). 
The receivers parse the strings to build the IASIOs to be injected the core of the IAS.

The `KafkaPublisher` forces the `client.id` property to be the ID of the plugin to help debugging in the server side.

The `KafkaPublisher` accepts the following additional properties from the JSON configuration file or the java command line:

| Name | Default | Type | Meaning |
| ---- | ------- | ---- | ------- |
| org.eso.ias.plugin.kafka.config || String | The path to the configuration file |
| org.eso.ias.plugin.kafka.topic | "PluginsKTopic" | String | The name of the kafka topic |
| org.eso.ias.plugin.kafka.partition || int>0 | The partition of the kafka topic |

The publisher gets the values of the kafka configuration properties in the following order:
1. java properties in the command line
1. properties from the plugin JSON configuration file
1. the configuration file whose name is passed in the command line, if present
1. the default configuration file

### Statistics

If requested, the plugin collects and publishes statistics about submitted samples to ease the identification and fixing of problematic monitor points.
We have seen in the past that misbehaving monitor points, or algorithms to get values from the hardware or software components of the monitored systems, could cause instability. 
For example taking samples at an unreasonable rate could overload the CPU or flood the network.
Filtering can for sure mitigate issues like that but ensuring to not overload the system or being able to catch the problematic monitor points is of great help.

The plugin collects statistics and publish them in form of logs at a given time interval (10 minutes by default).
A basic log is submitted at INFO level whenever the time interval elapses provided that the user did not disabled the collection of statistics. Such log shows the number of submitted updates in the elapsed time interval.

More detailed statistics can be collected by setting the `stats.detailed` java property to `true`. In such a case, aside of the standard statistic message, the plugin logs another message with the IDs of the must frequently updated monitor points together with their number of occurrences. The generation of detailed statistics can be computationally intensive and must be used carefully.

### Dependencies

The list of direct dependencies of `ias-plugin.jar` can be obtained by `jdeps`:
```
ias-plugin.jar -> /home/acaproni/IasRoot/lib/ias-basic-types.jar
ias-plugin.jar -> /home/acaproni/IasRoot/lib/ias-heartbeat.jar
ias-plugin.jar -> /home/acaproni/IasRoot/lib/ias-kafka-tools.jar
ias-plugin.jar -> /home/acaproni/IasRoot/lib/ExtTools/jackson-core-2.9.4.jar
ias-plugin.jar -> /home/acaproni/IasRoot/lib/ExtTools/jackson-databind-2.9.4.jar
ias-plugin.jar -> /home/acaproni/IasRoot/lib/ExtTools/kafka-clients-1.0.0.jar
as-plugin.jar -> /home/acaproni/IasRoot/lib/ExtTools/slf4j-api-1.7.25.jar
```

The plugin

## Plugin development example

The test folder of the `plugin`module contains a simple example in the `org.eso.ias.plugin.test.example`package.
The example shows how to use the plugin API to create a plugin that interfaces to a remote monitored system and publishes the monitor point values to the core of the IAS.

The example simulates a fantasy weather station with 5 monitor points:
* Temperature
* WindDirection
* WindSpeed
* Humidity
* Pressure

The configuration of the plugin is in `test/resources/org/eso/iasplugin/config/test/jsonfiles/WeatherStationPlugin.json`. 
Being the resources folder, the json configuration file will be packed in the jar generated by the `test/build.xml`.

By looking at the configuration file, you will find the ID of the plugin, and the definition of the monitored points. 
Each monitor point has a refresh rate that is the time interval to send each monitor point to the core of the &brvbar;AS: it is not the refresh rate of the monitor point in the monitored system.
The core of the &brvbar;AS requires each value to be refreshed often for 2 reason:
* ensure that the plugin is alive and the value of the monitor point updated
* provide to clients the value as soon as it arrives

The sending of the values to the core of the &brvbar;AS is done automatically by the plugin API: the user needs only to send new values when the monitored system produces them. 
For example, if the humidity is updated by the hardware device at a rate of once per minute, the developer provides such value once per minute to the plugin API but the same value will be transparently sent many times to the core of the &brvbar;AS as specified by the Humidity `refreshTime`.

This example shows how to develop such a plugin against a simulated weather station that represents our monitored control system. 
In real case, the developer of the plugin must access the monitor points with the API provided by the monitored control system that can range for a socket, REST API, a databse, a file system and so on.

To show what is effectively sent to the core of the &brvbar;AS, the plugin publishes the values of the monitored points in a JSON file, the MonitorPointSender passed in the constructor of the Plugin.

We kindly suggest to read all the javadoc and the comments of the sources because not all the informations provided there are reported here.

The example provides a simulated weather station, `SimulatedWeatherStation` where you can see when the hardware refreshes the value of each simulated monitor point through a `SimulatedValueGenerator`.

The development of a plugin consists of providing the implementation of
* initializion: the plugin connects to the control software of the monitored system through the API it provides to read the values of the monitored points
* a loop that get the values of the monitored to be sent to the core of &brvbar;AS
* cleanup: disconnection from the monitored system, release of the acquired resources and cleanup

The three phases closely depends on the API provided by control software of the monitored system.

There are many possible scenarios but the general concept is that the loop provides the values of the monitored points whenever they are available so in principle the loop is a never ending task. However if the plugin is started as a process of the controlled software system, then the controlled system itself could control his life cycle.

Form the core of the &brvbar;AS there are 2 possibilities:
* the plugin is not running, no monitor point is received: the user panel shows that the controlled system is not connected
* the plugin is running but it is not possible to get the values of the monitored points: the plugin continues to send the last acquired value, the core of &brvbar;AS knows that the plugin is alive but that the values it provides are invalid.

The plugin provides methods to stop and resume the periodic sending of monitor point values to the core of the &brvbar;AS. You can try the effect of calling such methods by modifying the source code of the example.

The kafka converter needs the following properties passed in the command line

## Python plugins ([#95](https://github.com/IntegratedAlarmSystem-Group/ias/issues/95))

In the ALMA operational environment, we have found that it would be useful to be able to develop plugins in python. Python is very easy to write and there ar, in ALMA, a lot of python scripts already used to get monitor points and alarms that can be adapted to feed the IAS.

Instead of an entire new python implementation of the java plugin, the IAS provides a python binding: the python script sends monitor points to a dedicated java plugin that, reusing the existing framework, takes care of filtering and sending the data to the BSDB.

The binding happens by means of sockets: the python plugin sends monitor points and alarms formatted as JSON strings to the java plugin. 
At the present the IAS offers only one implementation based on UDP sockets; other implementations can be offered on demand.
It is possible to develop plugins with any other programming language as soon as they send properly formatted JSON strings to the java plugin.

The functioning of the python plugin is summarized in the following schema. The detailed description of the implementation is in the [ticket](https://github.com/IntegratedAlarmSystem-Group/ias/issues/95).

![Python plugin schema](https://user-images.githubusercontent.com/8500519/39758645-d75bc0f6-52a6-11e8-8ebe-f962aba1805c.png)

### Python plugin howto

The UDP python plugin sends data to a stand alone tool, the `UdpPlugin`, that needs the same JSON configuration file of the java plugin: all the expected monitor points must be present in the JSON file with the same rule we have already described for the java plugin. The command line of the `UdpPlugin` must provide the UDP port to get monitor points from.

There are 2 versions of the UpdPlugin, one for python 3.x, `UdpPlugin3` and one for python 2.7, `UdpPlugin2`.
They are very similar apart that the version for python 2 uses strings instead of enumerated that have been introduced in version 3.

The following example shows how to develop a python 3 plugin:
```
from IasPlugin3.UdpPlugin import UdpPlugin

if __name__ == '__main__':
    udpPlugin = UdpPlugin("iastest.osf.alma.cl",10101)
    udpPlugin.start()
    
    # Collect monitor points
    ....
    # Send few examples...
    udpPlugin.submit("ID-Double", 122.54, IASType.DOUBLE, operationalMode=OperationalMode.INITIALIZATION)
    udpPlugin.submit("ID-Long", 1234567, IASType.INT, operationalMode=OperationalMode.STARTUP)
    udpPlugin.submit("ID-Bool", False, IASType.BOOLEAN, operationalMode=OperationalMode.OPERATIONAL)
    udpPlugin.submit("ID-Char", 'X', IASType.CHAR, operationalMode=OperationalMode.DEGRADED)
    udpPlugin.submit("ID-String", 'Testing for test', IASType.STRING, operationalMode=OperationalMode.CLOSING)
    udpPlugin.submit("ID-Alarm", Alarm.SET, IASType.ALARM)


    udpPlugin.shutdown()
```

The python plugin sends UDP datagrams to the port 10101 of the iastest server by using the UdpPlugin IAS library.
`UdpPlugin.start()` must be called before sending monitor points and `UdpPlugin.shutdown()` must be called before exiting.

The collecting and sending of monitor points is normally done in a loop, not show in the code snippet.

`UdpPlugin.submit(...)` is used to send monitor points to the java plugin. It is optionally possible to associate the operational mode to a monitor point that will be forwarded up to the BSDB.
