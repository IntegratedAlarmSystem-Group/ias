[![ias-build](https://github.com/IntegratedAlarmSystem-Group/ias/actions/workflows/ias-build-install.yml/badge.svg)](https://github.com/IntegratedAlarmSystem-Group/ias/actions/workflows/ias-build-install.yml)
[![Super-Linter](https://github.com/IntegratedAlarmSystem-Group/ias/actions/workflows/lint.yml/badge.svg)](https://github.com/IntegratedAlarmSystem-Group/ias/actions/workflows/lint.yml)
# ias
The production version of the core of Integrated Alarm System.

Website: [Integrated Alarm System](https://integratedalarmsystem-group.github.io)

🆕IAS [Tutorials](https://github.com/IntegratedAlarmSystem-Group/Tutorials/wiki)

Browse the [wiki pages](https://github.com/IntegratedAlarmSystem-Group/ias/wiki) for help.

Use `gradlew build install` to build all the modules and install them in `$IAS_ROOT`

The core is composed of the following modules (in order of compilation):
* `Tools`: generation of API documentation, external libraruies, configuration files, IAS python scripts, support of scala logging and ISO 8601 timestamps
* `Cdb`: RDB and JSON CDB
* `BasicTypes`: IAS supported value types, operational mode, validity
* `KafkaUtils`: producers, consumers and utilities for Kafka
* `Heartbeat`: heartbeat generation
* `plugin`: java plugin library
* `PythonPuginFeeder`: python 2.x and 3.x plugin libarries
* `Converter`: the converter that converts IASIOs produced by plugin in IAS valid types
* `CompElement`: the ASCE computing element that runs the transfer function
* `DistributedUnit`: the DASU the produce the output delegating to ASCEs
* `Supervisor`: the supervisor to deploy more DASUs in the same JVM
* `WebServerSender`: the web server sender that forwards IASIOs published in the BSDB to the web server via websockets
* `TransferFunctions`: a collection of transfer functions
* `SinkClient`: clients that gets IASIOs out of the BSDB and process them like the email sender
* `Tests/SimpleLoopTest`: a module with a test
* `Extras`: supporting tools like the cdbChecker
