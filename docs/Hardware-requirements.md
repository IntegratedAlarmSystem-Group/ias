From the requirements, the IAS must cope with 50K alarms and 10k synthetic parameters. Internally synthetic parameters and alarms are indistinguishable so for the sake of this discussion we focus on 60k alarms (also called IASIOs).
60k alarms are produced by elaborating a number of monitor points produced by plugins, number that has not been not specified in the original requirements.

The following table summarizes the IAS configuration in the control room

| *Object type* | *Instances* |
| ------------- | ----------- |
| Alarms | 1025 |
| Monitor points | 625 |
| ASCEs | 68 |
| DASUs | 43 |
| Supervisors | 3 |
| Converters | 1 |
| Sink clients | 2 |


We took the following figures by monitoring the traffic in the BSDB for 1 hours

|     | *plugin topic* | *core topic* |
| --- | -------------- | ------------ |
| # messages | 648474 | 1036789 |
| # bytes produced | 220Mb | 566 Mb |
| longest message | 978 | 8096 |

Kafka runs in a single node and needs 1.7Gb of disk space for keeping 1 hour of monitor points in both the core and plugin topics. 
For the functioning of the core, 1 hour is not really needed because each tool requires only the last few seconds of data published in the topics: the disk space required by kafka could be substantially reduced by setting kafka properties.

Disk space required by zookeeper is as little as 28k and we can ignore for this discussion.

Projecting the numbers above to a system loaded with 60K Alarms and 40K monitor points generated by plugins, kafka would need 110Gb to save 1 hour of alarms and monitor points. We suggest to have at least 2 Gb SSD HDs totally dedicated to kafka plus one for the operating system. A RAID system could increase perfomance and reliability.

The RAM greatly improves kafka performances and the throughput of the system. It also affects the number of processes running concurrently in the server including plugins, converters and Supervisors.
To cache the last seconds and save on disk the rest of the logs, a good choice for kafka nodes is 64Gb RAM but 32 is also possible. This choice is justified by the fact that IAS processes work only on the most recent data only; oldest monitor points and alarms in the BSDB can be flushed on disk without impacting performances.

Fast network interface card is important because all the communication between nodes are networked; it less relevant if all the IAS processes (apart plugins of course) are deployed in a single server. I would install a high performant network card especially if in future you wish to deploy additional kafka nodes to the system.

Kafka does not have high requirements on CPU that depends mostly on other processes running in the server.
At full load, the system processes around 52.000Mb of data in one hour that is around 15Mb/s: IAS processes process 15Mb/s with many network and I/O operations. The number of available cores is a relevant factor in the choice of the server. Choose a modern CPU with multiple cores. More cores takes precedence over faster CPUs.
Projecting the numbers we collected so far in a full loaded system, the total number of processes will be about 100 that is not a big number. But each process must process 15Mb/s of data that means that the CPU must allow to process 1500Mb/s.

XFS linux file system should be used for kafka servers.
 

# Scalability

The IAS is scalable. You can add more processes if you need to increase performances. For example deploy additional converters if one is not able to cope with the flow of data provided by plugins.

If convenient you could deploy more kafka nodes as kafka is distributed and replicated. It can be convenient to have more replicated kafka nodes in production. For example the LTDB feeder that flushes records in cassandra can work locally in a replicated kafka node.

If one server is overloaded, a new server can be added redeploying there part of the Supervisors. In that case i t must be tested if a remote connection to kafka is performant enough or if it is better to replicate also a kafka node.

There are IAS processes that can be duplicated so that each of them wotrk on a partition of the data sharing the load The converter in one of them: if you deploy more converters each of them convert a partition of the data published by plugins (ensure they all have the same group ID and that kafka is properly configured). The same consideration applies to the LTDB feeder and could apply to sink processed.

Supervisors and DASUs instead need to get all the IASIOs in the BSDB to select the ones they need. They cannot work on partition of data otherwise they will never receive all the inputs they need to produce the output.
These kind of tools must be configured to receive all the IASIOs i.e. typically they belong to different groups or, in other words, there is one kafka group for each Supervisor. The IAS handle that transparently.

A Supervisor with its DASU cannot be duplicated to work on partition of data. The only way to increase the performance of the Supervisor is to deploy less DASUs into it with the extreme to have only one DASU in a Supervisor. The draw back is that the number of processes increase. The time spent by the TF shall be taken under control and they shall never perform slow remote operation or time unpredictable operations. The core constantly measure the time spent by TF to produce the output and can decide to shutdown misbehaving TFs.

