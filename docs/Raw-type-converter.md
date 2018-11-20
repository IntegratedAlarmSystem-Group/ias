# Raw type converter

Raw monitor points and alarms retrieved by heterogeneous monitored systems needs to be converted in the proper &brvbar;AS data type to be processed by the core.
Converters are stand alone software tools that consist of a loop that repeats the same sequence of actions:
* read a value received from plugins
* covert the value in the proper &brvbar;AS data type
* inject the value in the core for processing

The conversion phase needs to get, from the configuration database, the type of the data to convert from its identifier.

In the actual implementation, the converter reads from the CDB and keeps in memory the configuration of all the IASIOs. 
However, for each IASIO it saves in the cache only the fields it needs for the conversion.
This implementation could not be optimal depending on the number of IASIOs in the CDB, in that case we will need to implement some strategy to save memory.

The object to get raw data from plugins and send the converted values to the core and the object to read the CDB are injected at startup: dependency injection (DI) is handled by [spring](https://spring.io/) with annotations.
The DI configuration is provided by the `ConverterConfig` java class.
For testing purposes you have to provide your own config and initialize the mock objects as done in `Converter#main(String[])`.

## Processing data from plugins

`MonitorPointData`objects published by plugins are received as JSON strings, transformed into IASIO values by applying a transformation function and finally sent to the core.

Such processing is done by a `ConverterStream`: it starts getting events from plugins and for each received event applies the transformation function and sent the result to the core of the &brvbar;AS.

## Transformation function

The transformation function gets as parameter a string that is the JSON representation of a `MonitorPointData` object and performs the followings:
- build a `MonitorPointData` from the JSON string
- get the configuration of the monitor point value from the CDB
- build the `IASValue` that is the core representation of such a monitor point
- serialize and return the IASValue in a JSON string

---------

# Using [kafka streams](https://kafka.apache.org/0110/documentation/streams/tutorial)

The first version of the converter basically transformed the input JSON string (representing a `MonitorPointData`) in another JSON string (representing a `IASValue`) within a loop:
* poll for the next json string
* build a ``MonitorPointData``, mpd, out of the received json string
* build a ``IASValue<?>`` representation of mpd, iasVal
* serialize iasVal into a json string
* send the json string to the core

The actual version takes advantage of the Kafka streams that make this concept fairly easy i.e. something like:
```
KStream<String, String> source = builder.stream("plugin-input-stream");
source.flatMapValues(value -> transformvalue)).to("ias-core-stream")
```

The kafka stream converter accepts the following properties

| Property | Default value | Type | Meaning |
| -------- | ------------- | ---- | ------- |
| org.eso.ias.converter.identifier | | String | The unique identifier of the converter |
| org.eso.ias.converter.kafka.servers | | String | Kafka servers and ports |
| org.eso.ias.converter.kafka.inputstream | PluginsKTopic | String | The topic where plugins push monitor points and alarms |
| org.eso.ias.converter.kafka.outputstream | IasCoreKTopic | String | The topic to send values to the core of IAS |
