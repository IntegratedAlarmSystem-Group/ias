# IasValueProcessor

The IasValueProcessor is a kafka sink tool that allows to get all the `IASValues` published in the BSDB and processes them like for example notify registered users when an alarm is set or cleared or send the value to the LTDB.

The IasValueProcessor instantiates a kafka consumer to get monitor points from the kafka topic that forwards them to one or more listeners for processing. The values read from the BSDB are not sent one by one to the processors but buffered in a list whose minimum size can be customized by setting the `org.eso.ias.valueprocessor.thread.minsize` java property property. The processing is also periodically triggered. If this property is 1 then one thread per listener is spawned to process only one received IasValue. If it is set to _n_ then one thread is created to process a set of _n_ values.

The `IasValueListener` detects and logs the names of the slowest threads. The warning is issued if the queue of IasValues waiting in the queue to be processed grows too much. At the same time the tool identifies the threads that take too much time to terminate, logs their IDs and finally, kills the ones that did not terminate after a timeout.
The time to check if a thread is slow and the timeout to kill not responding threads can be configured by setting java properties.
The killing of not responding threads is done by calling their `close()` method and marking them as broke so that their `process()` methods is not invoked anymore to process new IASValues.

The processors accepts the following properties to override the values read from the CDB

| Property | Default Value | Type | Meaning |
| -------- | --------------| ---- | ------- |
| `org.eso.ias.kafka.brokers` | localhost:9092 | String | kafka brokers |
| `org.eso.ias.valueprocessor.thread.minsize` | 50 | Integer | The size of the list of IasValues to process |
| `org.eso.ias.valueprocessor.thread.timeout` | 3 | Integer | The time (secs) to wait for thread termination |
| `org.eso.ias.valueprocessor.thread.periodic.time` | 500 | Integer | The frequency of the periodic processing of values |
| `org.eso.ias.valueprocessor.thread.killafter` | 60 | Int | Kill threads that do not terminate in this number of seconds |
| `org.eso.ias.valueprocessor.maxbufsize` | 100000 | Int | Max size of the buffer to save unprocessed values |
| `org.eso.ias.valueprocessor.log.throttling` | 5 | Int | Log throttling time in seconds |
| `org.eso.ias.valueprocessor.log.warningbuffersize` | 25000 | Int | Logs a message if the number of non processed values is greater than this value | 

The listener of events produced by the IASValueProcessor process the IASValues read from the kafka topic. They inherits from the `ValueListener`abstract class and must implement three methods:
* `init()`: to initialize the object, acquire resources and get ready to process monitor point
* `process(..)` is invoked whenever a new set of values has been consumed from the kafka topic
* `close()` is called at the end of the computation to close the listener and free all the acquired resources

The `IasValueProcessor` allows to register more listeners each of which concurrently run to process the value. At the present the slowest thread is the one who slow down all other processor. For this reason the `IasValueProcessor` constantly monitors the slowest threads, logs message to detect bottlenecks and even close misbehaving processors.

In general, kafka processors must be fast enough to cope with the data produced in the kafka topic. The process method should be fast enough for that otherwise the process should be replicated.
The `IasValueProcessor` monitors the queue of unprocessed events and automatically discard the old ones if the processors are too slow. Also in this case logs allows to detect and investigate the problem.