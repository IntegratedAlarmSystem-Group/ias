The alarm system computing element (ASCE) runs inside the DASU. Its main task is to run the inputs against a user provided transfer function (TF) to generate the new ouput.

The ASCE generates a new output when the DASU sends a new set of inputs (IASIOs):
* received from other ASCEs running in the same DASU
* generated by remote systems i.e. by the plugins
* generated by other DASUs

The ASCE does not read IASIOs from the BSDB queues neither publishes the generated output. 
It is the DASU that interfaces with the BSDB (later the Supervisor will interface with the BSDB).

# The state of the ASCE

The ASCE is a state machine with the following possible states

| State | Meaning |
| ----- | ------- |
| Initializing | The initial state of the ASCE |
| Healthy | the ASCE has been initialized nad is correctly running the TF |
| TFBroken | The TF throws an exception: it will never be executed again |
| TFSlow | The TF is too slow: if a transient problem then ok otherwise it is marked as broken and inhibited |
| ShuttingDown | The ASCE is shutting down |
| Closed | The ASCE is closed |

After initialization, when the DASU submits a new set of inputs, the ASCE runs the TF but only if its state is either Healthy or Slow.
If the execution of the TF takes long time (@see TransferFunctionLanguage.MaxTolerableTFTime) the the state changes to `TFSlow` but the ASCE continues to invoke the TF.
In reality can happen that a TF is pretty fast but sometimes slow down due for example to a peak.
if the slowness is transient then the state returns to `Healthy` after awhile.
But if the the TF remains in the `TFSlow` state for long time then the ASCE switch to the `TFBroken`state and will never run the TF again.
This policy must be implemented (@see [#28](https://github.com/IntegratedAlarmSystem-Group/ias/issues/28) to avoid that a misbehaving TF affects the entire system.

# IASIOs in input

The ASCE get the list of possible inputs in the constructor: these are the all and only inputs that the ASCE recognizes.
If the DASU sends a inputs whose ID is unrecognized this is a run-time error.

The IASIOs in the constructor have no value because this will arrive later from the BSDB or from another ASCE running in the same DASU.
For the same reason, the parent ID of the IASIO in the constructor is unknown till the it arrives from the BSDB or another ASCE running in the same DASU.
In that moment the parent of the IASIO is fixed and the running ID is meaningful.
This is perfectly legal because the computation of the TF is entirely based on the ID and the parent ID (and the related runningID) is only used for logging and debugging.

The DASU triggers the update of the output by sending a set of inputs because the DASU knows the desired refresh rate of the last ASCE (i.e. the one who also produces the output of the DASU).
For that reason, the ASCE does not make any use of the refresh rates of the IASIOs (either the ones in input and the output).

# ASCE java properties

| Property | Default Value | Type | Meaning |
| -------- | --------------| ---- | ------- |
| `org.eso.ias.asce.transfer.maxexectime` | 1000 | Integer (msec) | The max execution time to mark the TF as slow |
| `org.eso.ias.asce.transfer.maxtimeinslow`| 30 | Integer (ses) | If the TF is slow responding for more then the this amount of seconds then the TF is marked as broken and will not be executed anymore |