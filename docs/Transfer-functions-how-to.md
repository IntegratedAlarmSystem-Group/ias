Only a proper configuration allows the IAS to produce alarms.
The configuration is composed of 2 parts: setting the CDB and developing the transfer functions run by the ASCEs.

This wiki page describe how to write the configuration; it does not contain details of all the fields of the tables of the CDB that you can find in the [CDB wiki](https://github.com/IntegratedAlarmSystem-Group/ias/wiki/ConfigurationDatabase).

CDB configuration allows to
* convert monitor point and alarms produced by plugin into values in the core topic of the BSDB
* provide inputs to ACSEs
* associate transfer functions to ASCEs
* deploy ASCEs into DASUs
* deploy DASUs into supervisors

Writing transfer function allows to elaborate the inputs of an ASCE in order to produce the output being it an alarm or synthetic parameter. In a certain way, the transfer functions represent the knowledge base of the alarm system.

# Plugins

The plugin require its proper configuration as described in the [plugin wiki](https://github.com/IntegratedAlarmSystem-Group/ias/wiki/Monitored-system-plugins) usually passed as a JSON file to the plugin itself.
This configuration let the plugin library know which kind of monitor points will be produced, their expected refresh rates, if they must be filtered before being sent and so on.
If a monitor point produced by the plugin is not configured in JSON file, the library does not know what to do with it and it will never be sent to the BSDB.

The plugin library pushes the monitor point produced by the plugin into the plugin topic of the BSDB waiting to be translated and pushed in the core topic of the BSDB by the Converter. Only at this point a monitor point produced by a plugin will be received by DASUs and ASCEs.

For the translation the converter needs to match the ID of the monitor point sent by the plugin to one of the IASIOs defined in the `IASIO/iasio.json` (or the IASIO table) of the CDB.
If the ID of the monitor pint does not match with any entry in that file the Converter does not know what to do and that point is discarded.

# Deployment of DASUs

DASUs run into supervisor. The Supervisor reads its configuration from the CDB and gets the IDs of the DASUs to deploy from the `DasuToDeploy` array i.e. to run a DASU, its ID must be present in the configuration of one Supervisor.
There is no point to deploy the same DASU in more then one Supervisor because the 2 will make the very same computation and produce the same output.

In the same way, it is enough to remove the ID of a DASU from the configuration of the Supervisor to not deploy it.

The deployment of a DASU, in the `DasuToDeploy` is independent of its definition in the CDB that is in the DASU table.

At run time the Supervisor gets the list of IDs of the DASU to deploy and gets their configuration form the DASU table. If the definition of a DASU is not found the Supervisor logs the error and exit (fail fast). The CDB checker helps finding inconsistencies. 

# Configuration of DASUs and ASCEs

The configuration of DASU depends on the configuration of the ASCEs deployed in the DASU itself. The Supervisor checks the configuration before instantiating a DASU and in case of errors logs the event and exits (fail fast).
 
## ASCE 

The configuration of the ASCEs includes, among the others,
* the list of inputs expected by the ASCE
* the output of the ASCE
* the transfer function to apply to the inputs

The inputs and the output of the ASCE must be defined in the IASIOs. 
The list of inputs must match with the inputs required by the transfer function. Such a check cannot be done statically before running the ASCE because the transfer function code is unknown at that moment. In case of mismatch the TF throws an exception at build time, initialization or run-time: the core catches the problem and inhibits the execution of the TF.
This kind of Situation can be identified by reading the logs. In the panel the alarm produced by the ASCE or the output produced by all the DASUs that gets its output will be marked as invalid.

The transfer function to run is composed of the class name plus the implementation language.
The implementation language is needed by the ASCE to pass the data structures (inputs) with the proper format. At the present the scala implementation is more performant because java needs some data type conversion due to the fact that the core that is written in scala.
The TF must be defined in the transfer function table of the CDB. The reason to have the TFs in a separate table is to allow reusability of TFs. In practice it means that it is possible to have a library of TFs and pick the proper one instead of writing many times the same code.
For example one of the most common TF is the threshold that activate an alarm if the value of the inputs is greater than a passed threshold: the code to execute is the same while the threshold is passed to the ASCE by means of properties in the CDB.

The configuration of the ASCE accepts a user defined list of properties that will be accessible to the ASCE itself (@see the [ASCE wiki](https://github.com/IntegratedAlarmSystem-Group/ias/wiki/Computing-Element-%28ASCE%29)) and to the transfer function.
Setting java property is the way to reuse general purpose TF like the MinMaxThreashod that set alarms whenever the value in input is lower than the min threshold or greater than the max threshold: the min and max thresholds are passedd by setting the `org.eso.ias.tf.minmaxthreshold.lowOn` and `org.eso.ias.tf.minmaxthreshold.lowOff` java properties.

## DASU

The configuration of the DASU tells to each DASUs which ASCEs to instantiate by passing their IDs. If a ASCE with the give ID is not found the Supervisor exits after logging the problem.

From the IDs of the ASCEs the DASU builds the topology of the ASCEs: the ASCEs must form a a-cyclic graph: the DASU ensures that there are no cycles in the connections.
The topology is logged for investigation and shows how the DASU forward the values between the ASCEs until the last one produces the output of the DASU to be pushed in the core topic of the BSDB

DASUs are not directly connected: the output of each DASU goes straight to the BSDB.
If one DASU running in one supervisor needs the output produced by another DASU it will get it from the BSDB even if the two DASUs run in the same supervisor.

# Transfer functions

The transfer functions contains the heuristic needed by the ASCE to produce the output depending on the value of the inputs.
The output of an ASCE is normally an alarm that can be clear or set with a given priority.
There are cases where the output of a ASCE is a synthetic parameter of any supported IAS data type. 
Such synthetic parameters are stored in the BSDB as the alarms are can be the inputs of DASUs and ASCEs.

The purpose of synthetic parameters is to produce value to be reused by many other ASCEs reducing replication.
They can also be used to decompose a complex heuristic in sub task allowing to write shorter and clearer TFs.

The powerful of the IAS is mostly given by the fact that it is possible to write and run whatever TF to cope even with the most complex cases.


## TF APIs

The TF API is composed of three methods that needs to be implemented:
* `initialize`: the ASCE runs this method soon after builnding the TF; all the initialization should go in this method
* `shutdown`: is the method called by the ASCE before shutting down to the the TF release all the allocated resource
* `eval`: invoked by the ASCE when a new set of inputs must be processed to produce the output

`eval` contains the heuristic to evaluate the inputs and produce the output and as such is the most important method.
It receives as parameters a map of the inputs and the last computed output.

The map of inputs contains all the inputs needed by the TF: if the ASCE does not have all the inputs (not yet published in the BSDB for example) then it does not invoke `eval()`.
Once all the inputs have been received by the ASCE then it executes `eval()` even if only one of the inputs change but passing all the inputs.
At a given point in time the map of inputs passed to `eval()`could contain very old inputs whose value is therefore invalid.

Note that even if the inputs did not change, the ASCE runs `eval()` at regular time intervals to assess the validity of the output (see [validity](https://github.com/IntegratedAlarmSystem-Group/ias/wiki/Validity)).

At run time, the TF is a scala/java object of the computing element.
It is built during the initialization of the ASCE and shut down upon termination.

The TF is statefull where the state is represented by the values of its variables and resources: there is no need to initialize the variable or read resources at each invocation of `eval()` as it can be done during the inizialization.

## TransferFunction guidelines

### Modify the `actualOutput` instead of building a new output
The output returned by the `eval()` method of the TF should be a transformation of the `actualOutput` received as parameter instead of being a new `InOut`.
This is ensures the consistency of the output. 

The output is an immutable object that allows the TF to return something like `actualOutput.updateValue(Some(iasio.value.get)).updateProps(iasio.props.get)` and it is the preferred way instead of building a new `InOut` object.

### Never save the value of the last computed output
The core can modify the output outside of the control of the TF itself so the value of the output passed to the `eval()` method can differ from the one saved between invocations.

There could be reason to save the value of the output in the implementation of the TF because there could be validi reasons to do that.
However, it is strongly discouraged and must be used with caution.

### The TF must be fast

The `eval()` method must be short and fast to not affect the functioning of the entire DASU and in the worst case all the DASUs running in the same Supervisor.

The ASCE monitors the execution of time of the transfer function and can inhibit its execution if it is too slow.

In particular the TF shall avoid to access slow resources like making queries to remote database or reading and parsing long XML files. If such computation is needed, it must be done by a plugin and the result of the computation pushed in the BSDB as synthetic parameter: the TF receives the value in one of its inputs and the TF.

### DASU to model complex devices

Modelling complex devices can be done by one single TF with a very long list of inputs. 
In this case there is only one DASU that runs a single ASCE.

In general it is not very easy to cope with many inputs to produce the output: the TF becomes very long and complex and the development error prone.

A model of the device is generally possible by means of an acyclic graph of interconnected subdevices.
Such modelling can be mapped to interconnected DASU by associating the subdevices to the DASUs.
In this way it is possible to decompose the modeling of a complex device into the modeling of many easier interconnected sub devices.

### Split complex TF between many ASCEs

Another way to reduce the complexity of a transfer function is to split its computation into easier parts like decomposing the function into sub-steps.
This can be done by splitting one transfer function into many transfer function each of which is executed be a ASCEs.

How the output of one ASCE is passed to one or many ASCEs is configured in the CDB with the only constraint to avoid cycles.

### Replication

Replication is part of the reusability of the components.
We have already described how to write and reuse the same transfer function in different contexts (ASCEs and CDB) by passing a proper set of java properties.

Replication allows to reuse the same DASUs with their topology of inputs to produce different outputs.

As we just said a complex device can be modeled by a set of interconnected DASUs each of which models one of its sub-devices.
Replication allows to have many of such devices and subdevices writing the configuration of the DASUs only once in the CDB.

For replication a template DASU must be added in the CDB.
The Supervisor is able to run a given concrete instance of such a DASU.

Replication allows to write in the CDB only a template that will be transformed by the Supervisor into a concrete instance at run-time (see [#80](https://github.com/IntegratedAlarmSystem-Group/ias/issues/80))

Replication is supported also at level of plugin (see [plugin](https://github.com/IntegratedAlarmSystem-Group/ias/wiki/Monitored-system-plugins))
