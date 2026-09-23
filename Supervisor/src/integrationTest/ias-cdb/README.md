# SupervisorWithBooleanTFs description

The CDB has been provided by ELT/CII team to reproduce the lack of update from the supervisor
reported in ECII-1383.
The picture and the drawio files describe the structure of this CDB.
The CDB defines a little chain of os dependent values.

ECII-1838 was about reliability: after some iteration the outputs were not produced.

The test sends the inputs and checks the outputs produced by the DASUs.
The test aim to stress the Supervisor and its DASUs more then to check if the ouputs are correct
because the correctness is tested by the tests of the boolean TFs.
