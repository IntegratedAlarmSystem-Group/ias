Monitor point values and alarms are collected from the remote monitored system by plugins and sent to the back stage database (BSDB) of the integrated alarm system. 
Such values are converted to the datatype used by the core of the IAS before being digested by ASCEs and DASUs.

Monitor point and values produced by plugins and those produced by ASCEs and DASUs are sent around as JSON formatted strings. 

The format of the JSON strings sent by plugins and those circulating into the BSDB are different as they cover different purposes.

# Data from plugins

The monitored software system collects samples of the monitor point values and alarms and submits such values to the plugin. 
The collected samples are codified to [MonitorPointData](https://github.com/IntegratedAlarmSystem-Group/ias/blob/master/plugin/src/java/org/eso/ias/plugin/publisher/MonitorPointData.java) for sending to the core.
The `MonitorPointData` contains a set of timestamps to follow the production of the value and the identifiers of the value, the plugin and the monitored system to be able to understand from where the values comes.
The value of the monitor point or alarm is serialized to a String and will be translated in the proper type (for example a double) by the core.

# Data inside the core

Data circulating inside the core of the IAS are formatted as the generic java [IASVAlue&lt;T&gt;](https://github.com/IntegratedAlarmSystem-Group/ias/blob/master/BasicTypes/src/java/org/eso/ias/prototype/input/java/IASValue.java).
For each of the supported type there is also a dedicated java type the extends the `IASValue`.
For example for the type long, i.e. a `IASValue&lt;Long&gt;`, there is a [IasLong](https://github.com/IntegratedAlarmSystem-Group/ias/blob/master/BasicTypes/src/java/org/eso/ias/prototype/input/java/IasLong.java) type that is not generic.

`IASValue&lt;Long&gt;` has a property of type [IASTypes](https://github.com/IntegratedAlarmSystem-Group/ias/blob/master/BasicTypes/src/java/org/eso/ias/prototype/input/java/IASTypes.java) that allows to translate the string in the proper java/scala type at runtime.
At the present, the IAS supports basic java types plus a [alarm type](https://github.com/IntegratedAlarmSystem-Group/ias/blob/master/plugin/src/java/org/eso/ias/plugin/AlarmSample.java) for sending alarms.

# Operational mode

Each monitor point or alarm red from a monitored system has a operational mode that depends on the operational mode of the monitored system or the device.
Knowing the operational mode allows to asses the reliability of the value. For example if the value is retrieved when the operating system is starting up or the device is initializing must be treated differently then after a successful initialization.

The operational mode is mainly useful inside the transfer function for example to avoid triggering false alarms during initialization or shutdown.

It could make sense to show the operational mode to operators or engineers with a dedicated color coding or combine the validity with the operational mode for visualization purposes.

# Validity

A monitor point has a Validity that tells ow reliable is the associated value.
At the present there are only 2 possible type of validity: `Reliable` and `Unreliable` that will be extended by more types to distinguish different cases.
For example a value has been red from the monitored system and immediately sent by the plugin. 
At this stage the  validity is reliable. 
Let´s suppose that a network problem delay the reception of the value in the core of 2 minutes. 
At this stage the value that has been received may or may not reflect the actual state of the monitor point in the monitored system so it is invalid.

The operators and engineers must be aware that a value or alarm is invalid so they will be represented by a dedicated color coding that must be combined with the color coding for the operational mode to allow understanding both kind of information at a glance.