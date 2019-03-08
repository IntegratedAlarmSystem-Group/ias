# Purpose

The validity is an attribute of a monitor point or alarm to estimate its reliability and allows operators and engineers to easily understand if they can trust the information provided by the IAS. 
In the GUIs, a dedicated color coding will be used to show the validity.

At the present a value can only be either ```Reliable``` or ```Unreliable```. The latter represents the case when a value has not been produced in time: operators cannot trust if what they see still represents the actual situation in the monitored system.

# Validity of a `IASValue` produced by plugins 

The validity must be set as unreliable by a plugin if a fresh value is not provided in time by the monitored system.
The value provided by a monitored system travels through the network to be stored in a temporary kafka queue (T): it is processed by the Converter and stored as `IASValue` in the IO queue to be processed by ASCEs.

Even if the monitored system produced the fresh value in time, its validity must be re-evaluated when processing because of
* network problem sending the value to the T queue
* the Converter was too slow to translate the value in the proper `IASValue` representation (this step includes getting the value from the T queue and pushing it back in the IO queue i.e. any slowness introduced by the message passing framework, kafka)

Adding timestamps for each steps allows to identify the bottleneck.

# Validity of a `IASValue` produced by ASCEs

A ASCE gets a set of `IASValue` in input from the IO queues or other ASCEs and produces a new `IASValue` by running the TF.

The validity of the output of the ASCE depends on the validity of its inputs: if all the inputs are valid then the alarm (or synthetic parameter) produced by the ASCE is valid otherwise it is marked as invalid before being pushed in the IO queue. The validity of the inputs depends on the point in time when it is requested because it depends on the last update time of the input and its refresh rate

# Validity update heuristic
* the plugin sets the validity of each value before pushing to the T queue: unreliable if the monitored system did not provide the value in time; reliable otherwise; the plugin 
  * set the time when the value has been provided by the monitored system
  * set the time when the values is pushed to the T queue
* The converter does not change the validity but it
  * set the time when the value has been pulled from the queue
  * set the time when the value has been converted in a ```IASValue```
  * set the value when the ```IASValue``` is pushed into the IO queue
* When calculating the validity of the output, the ASCE validates the validity of each ```IASValue``` it receives (input) against its refresh rate and the actual timestamp

Note that the ASCEs produce the output even if its inputs are invalid: in that case the output itself is marked as invalid and the operator will be informed by a proper color coding