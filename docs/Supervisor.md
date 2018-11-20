The Supervisor is the container of of the DASU: it allows to run more DASUs in the same JVM.

The implementation of the Supervisor is not very complex: it consists, in fact, of only one scala source.

The Supervisor takes the configuration from the CDB to instanziate the DASUs to run then builds a map of the IDs of the IASIOs needed by each of the DASUs.

The Supervisor connects to the BSDB to consume the IASIOs produced by the plugin (actually from the Converters) and by other DASUs and forwards them to the DASUs.
The Supervisor catches the IASIOs produced by its DASUs and publishes them in the BSDB.

To get IASIOs from the BSDB and to put the outputs in the BSDB, the Supervisor needs an `InputSubscriber`and an `OutputPublisher` in the constructor.
Note that they are the same interfaces that the DASUs expect in their constructor. In fact, the Supervisor uses the implementations defined in the DistributedUnit module.

With this trick it is possible to run the DASUs as stand alone processes instead of being built by a Supervisor.
The Supervisor itself implements the `OutputPublisher`and `InputSubscriber` interfaces to pass to the constructor of the DASUs. In this way the DASUs publishes the outputs in the Supervisor interface and the Supervisor, in turn, forward them to its publisher i.e. to kafka BSDB. There is almost no computation in the forwarding.
The Supervisor gets the data from its consumer i.e. from the Kafka BSDB, and forward them to the listeners of the DASUs. The only computation consists in partitioning the received IASIOs to send them only to the DASUs that expect such inputs.

There is essentially nothing complicate in the implementation of the Supervisor apart of this trick is clear.

| Property | Default Value | Type | Meaning |
| -------- | --------------| ---- | ------- |
| `ias.supervisor.autosend.time` | 5 | Integer>0 | The time interval (secs) to automatic send the last calculated output |
| `ias.supervisor.autosend.validity.threshold` | 15 | Integer>=0 | The threshold (secs) to mark an input invalid |

