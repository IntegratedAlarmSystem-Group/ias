# DASU overview

To allow running more then one DASU into the same JVM, each DASU starts into a Supervisor.
In the first implementation we will build the DASU as a stand-alone process: when implementing the supervisor, part of the code of the DASU will be moved in the Supervisor

The task of the Distributed Alarm System Unit is:
* read the configuration from the CDB to know which ASCEs to activate and for each ASCE
    * get the list of its inputs and the output it produces
    * generate the topology of the connection between ASCEs to know how to propagate IAS values
* ensure there are no cyclic graphs
* instantiate and initialize the ASCEs
* start the loop to get values from the BSDB
    * discard messages not needed by the ASCEs hosted in the DASU
    * enrich messages (update the validity at least; or is it a task of the ASCE?)
    * route messages to the ASCEs
    * send produced values to the BSDB

The implementation is in progress in the `dasu-implementation` feature branch.

## The topology

A Topology object tracks the ASCEs that run into the DASU.
A Topology is built early when the DASU reads the configuration from the configuration database and before instantiating the ASCEs.
The purpose of such an object is to link monitor point and values read from the queues (i.e. generated outside the DASU itself by a monitored software system by means of a plugin, or produced by other DASU in the IAS) to the ASCEs running into the DASU.
It provides flow information to move values coming from the outside to the ASCEs and finally to send the value produced by the DASU to IASIO queues of the IAS.

The DASU uses the topology when it receives IASIOs and must produce the output.
From one side IASIOs must be sent to ASCEs at the proper time from the other side the output of ASCEs is the input of other ASCEs in the DASU with or without other IASIOs coming form the queues. 
There is a dependency between IASIOs and ASCEs that constitute a flow graph: the topology builds such a graph making a lot of integrity checks to ensure that the configuration is correct; in particular it ensures that the graph is a-cyclic avoiding infinite loops of the data flowing into the DASU.

## Architecture and data flow

The DASU is a 4 layered tool.

<P align="center">
  <img src="https://github.com/IntegratedAlarmSystem-Group/ias/wiki/img/DASU-4layers.png" width="75%" height="75%">
</P>

From the bottom:
1. the subscriber at the bottom receives the IASIOs from the BSDB or other sources: if IASIOs come from the BSDB they are read from a kafka subscriber but they can be red from JSON files (useful for testing) or from a database (the Cassandra LTDB for example for after the facts investigation)
1. the subscriber passes the IASIOs to the filter level that accepts only the IASIOs needed by the ASCEs running in the DASU and discards all the others (Kafka does not provide filtering): the filtering is done on the unique ID of the IASIOs
1. the next phase is composed of 2 steps that jointly produce the output of the DASU:
    * accepted IASIOs are propagated though the ASCEs, from the bottom to the ASCE that produces the output of the   DASU itself: the data flow is based on the layers of the ASCEs provided by the topology
    * each ASCE, as soon as receives the inputs, runs the transfer function and provides its output to the other ASCEs and the DASU
1. the last layer is the sending of the computation of the DASUs to the BSDB to be available to other entities of the IAS; aside of the kafka publisher, there could be more implementation for example saving in a file or sending to a listener useful for testing.

## Update of the output

When a set of inputs arrive, the DASU updates its output and sends it to the BSDB if its value changed.

To avoid running the TFs too many times, the subscriber keeps collecting the inputs in a map till a time interval elapses then triggers the refreshing of the output (throttling).

If no new inputs arrive, the DASU keeps sending the same output at definite time interval (refresh rate). Before sending the output, the DASU recalculate its validity.
Such ahe automatic refresh of the output when there are no new inputs is done by a timer thread and must be activated by invoking a dedicated method. At the same way, it is possible to inhibit the refresh and reactivate later.

## DASU java properties

| Property | Default Value | Type | Meaning |
| -------- | --------------| ---- | ------- |
| `ias.dasu.threadpoolcoresize` | (Number of available cores)/2| Integer | The size of the pool of the executor service of the DASU |
| `ias.dasu.min.allowed.output.refreshrate` | 250 | Integer (msec) | The min allowed refresh rate of the output |
| `ias.dasu.stats.timeinterval` | 10 | Integer>=0 (min) | The time interval to log statistics ; 0 disable the logging of statistics |
