#! /usr/bin/env python3
"""
Module for testing the ACK in the Supervisor from python.

It is the python implementation of TestAck.scala when the ACK is
sent by a python script

The test:
  - run the SupervisorWithKafka supervisor as an external process
  - checking the HB, wait until the supervisor is up and running
  - set, clear and ack alarms produced by the DASUs of the supervisor by sending inputs and commands
  - check if the commands have been executed by checking the replies to the commands sent by the supervisor
  - check if the ALARM have been effectively set/cleared
  - shut down the Supervisor sending the SHUTDOWN command
 
The ACK command is sent using the python IasCommandSender.
 
The tests waits for a certain time (greater than the auto refresh time interval)
to be sure that the supervisor emits the alarms i.e. there is no other synchronization mechanism
than waiting for a resonable time before getting the alarms
"""


import unittest
import subprocess
import time
import uuid
import logging

from IASLogging.logConf import Log
from IasCmdReply.IasCommandSender import IasCommandSender
from IasCmdReply.IasCommandType import IasCommandType
from IasCmdReply.IasCmdExitStatus import IasCmdExitStatus
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasKafkaUtils.KafkaValueConsumer import KafkaValueConsumer, IasValueListener
from IasHeartbeat.HbKafkaConsumer import HbKafkaConsumer, HeartbeatListener
from IasHeartbeat.HearbeatMessage import HeartbeatMessage
from IasHeartbeat.IasHeartbeatStatus import IasHeartbeatStatus
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasBasicTypes.IasType import IASType
from IasBasicTypes.Alarm import Alarm
from IasKafkaUtils.KafkaValueProducer import KafkaValueProducer

# Set up logging
logger = Log.getLogger(__file__)

class HbListener(HeartbeatListener):
    """
    The HB listener to know when the Supervisor is ready (i.e. its HB state is RUNNING)
    """
    def __init__(self, supervisorId: str):
        super().__init__()
        self.supervisorId = supervisorId
        # The HBs received from kafka
        self. hbs = []

        self.lastSupervState: IasHeartbeatStatus = None

    def iasHbReceived(self, hb: HeartbeatMessage):
        logger.info("HB received: %s %s",hb.hbStringrepresentation,hb.state.name)
        if (self.supervisorId in hb.hbStringrepresentation):
            self.hbs.append(hb)
            self.lastSupervState = hb.state

class IasioListener(IasValueListener):
    def __init__(self):
        super().__init__()
        # The last alarm received
        self.last_alarm_received: Alarm = None
        # The IASIO with the last received alarm
        self.last_alarm_iasio: IasValue = None

    def iasValueReceived(self, iasValue: IasValue):
        logger.info("IasValue received: %s", iasValue.toString())
        if iasValue.valueType==IASType.ALARM:
            self.last_alarm_iasio = iasValue
            self.last_alarm_received = Alarm.fromString(iasValue.value)
            logger.info("Alarm received %s", self.last_alarm_received.to_string())

class TestPyAck(unittest.TestCase):

    # The ID of the Supervisor to run
    supervisorId = "SupervisorWithKafka"

    # The Supervisor process
    completedSupervisorPopen = None

    # The command sender fulrrunning ID
    cmdSenderFrid = "CmdSenderFullRunningID"

    # The ID of the sender for the command kafka topic
    senderId = "CmdSender-"+str(uuid.uuid4())

    cmdSender = IasCommandSender(cmdSenderFrid, senderId, IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)

    hbListener = HbListener(supervisorId)

    hbConsumer = HbKafkaConsumer(IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
                                 "hbListenerId", 
                                 "hbListenerGId"+str(uuid.uuid4()), 
                                 hbListener)

    iasioListener = IasioListener()

    iasioConsumer = KafkaValueConsumer(iasioListener, 
                                       IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
                                       IasKafkaHelper.topics['core'], 
                                       "coreListenerId", 
                                       "coreListenerGId"+str(uuid.uuid4()))
    
    iasioProducer = KafkaValueProducer(IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
                                       IasKafkaHelper.topics['core'], 
                                       "IasioProducerId")
    
    @classmethod
    def buildIasio(cls, value: float) -> IasValue:
        """
        Builds the double IASIO to trigger/clear the alarm

        Params:
            value The value to set in the IASIO
        Return:
            The IasValue to send to the core topic
        """
        now = Iso8601TStamp.now()
        json = f'{{"value":"{value}","readFromMonSysTStamp":"{now}","receivedFromPluginTStamp":"{now}","sentToConverterTStamp":"{now}",'
        json = f'{json}"receivedFromPluginTStamp":"{now}","convertedProductionTStamp":"{now}","sentToBsdbTStamp":"{now}","productionTStamp":"{now}",'
        json = f'{json}"mode":"OPERATIONAL","iasValidity":"RELIABLE",'
        json = f'{json}"fullRunningId":"(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(SimulatedPluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature:IASIO)",'
        json = f'{json}"valueType":"DOUBLE"}}'
        return IasValue.fromJSon(json)


    @classmethod
    def setUpClass(cls):
        """
        Run the Supervisor, the listeners etc.
        """
        logger.info("Setting up the test")
        # Start getting HBs
        logger.info("Connecting the HB listener")
        cls.hbConsumer.start(30)
        
        logger.info("Connecting the IASIO listener")
        cls.iasioConsumer.start()
        
        logger.info("Starting the Supervisor %s", TestPyAck.supervisorId)
        cmd = ["iasSupervisor", TestPyAck.supervisorId, "-j", "src/test", "-x", "WARN"]
        TestPyAck.completedSupervisorPopen = subprocess.Popen(cmd)
        
        # Give the Supervisor time to run
        timeout = 60
        now = 0
        while now<timeout and cls.hbListener.lastSupervState!=IasHeartbeatStatus.RUNNING:
            time.sleep(1)
        assert cls.hbListener.lastSupervState==IasHeartbeatStatus.RUNNING
        logger.info("Supervisor up and running")

        TestPyAck.cmdSender.set_up()
        logger.info("Test set up")

    @classmethod
    def tearDownClass(cls):
        """
        Terminate the Supervisor, the listeners, etc.
        """
        cls.iasioConsumer.close()
        cls.hbConsumer.close()
        # Send the command to terminate to the Supervisor
        print(f"Sending CMD to shutdown the {TestPyAck.supervisorId}")
        TestPyAck.cmdSender.send_sync(TestPyAck.supervisorId, IasCommandType.SHUTDOWN)
        # Close the command sender
        TestPyAck.cmdSender.close()

        if TestPyAck.completedSupervisorPopen is not None:
            TestPyAck.completedSupervisorPopen.kill()

            # Throws TimeoutExpired if the Supervisor does not terminate
            TestPyAck.completedSupervisorPopen.wait(10)

    def testAckFromPython(self):
        logger.info("Test started")

        value = TestPyAck.buildIasio(0)
        TestPyAck.iasioProducer.send(value)
        # Give the supervisor time to produce the alarm 
        time.sleep(10)
        alarm = TestPyAck.iasioListener.last_alarm_received
        self.assertIsNotNone(alarm)
        self.assertFalse(alarm.is_set())
        self.assertTrue(alarm.is_acked())

        # set the alarm
        value = TestPyAck.buildIasio(100)
        TestPyAck.iasioProducer.send(value)
        # Give the supervisor time to produce the alarm 
        time.sleep(10)
        alarm = TestPyAck.iasioListener.last_alarm_received
        self.assertIsNotNone(alarm)
        self.assertTrue(alarm.is_set())
        self.assertFalse(alarm.is_acked())

        # Send the ACk command to the Supervisor
        logger.info("Sending the ACK to the Supervisor")
        ack_cmd_params = [TestPyAck.iasioListener.last_alarm_iasio.fullRunningId,
                          "User provided comment for ACK from python"]
        reply = TestPyAck.cmdSender.send_sync(TestPyAck.supervisorId, 
                                              IasCommandType.ACK,
                                              ack_cmd_params,
                                              None,
                                              30)
        self.assertIsNotNone(reply)
        self.assertEqual(reply.exitStatus, reply.exitStatus.OK)
        logger.info("Reply from cmd %s", str(reply.exitStatus.name))

        # Give the supervisor time to produce the ACK'ed alarm 
        time.sleep(10)
        alarm = TestPyAck.iasioListener.last_alarm_received
        self.assertIsNotNone(alarm)
        self.assertTrue(alarm.is_set())
        self.assertTrue(alarm.is_acked())

        logger.info("Test done")

if __name__ == "__main__":
    unittest.main()
##############################################################

# class Identifier:
#     """
#     Class representing an identifier.

#     Attributes:
#         value (str): The value of the identifier.
#         identifier_type (str): The type of the identifier.
#     """

#     def __init__(self, value: str, identifier_type: str):
#         self.value = value
#         self.identifier_type = identifier_type

# class Process:
#     """
#     Class representing a process.

#     Attributes:
#         command (List[str]): The command to run in the process.
#     """

#     def __init__(self, command: List[str]):
#         self.command = command

# class CommandSender:
#     """
#     Class for sending commands to a supervisor.

#     Attributes:
#         kafka_producer (KafkaProducer): The Kafka producer used to send commands.
#     """

#     def __init__(self, kafka_brokers: str, client_id: str):
#         self.kafka_producer = KafkaProducer(bootstrap_servers=kafka_brokers, client_id=client_id)

#     def send_sync(self, topic_name: str, command_type: str, params: List[str], props: dict, timeout: int):
#         """
#         Send a synchronous command to the supervisor.

#         Args:
#             topic_name (str): The name of the Kafka topic.
#             command_type (str): The type of the command.
#             params (List[str]): The parameters of the command.
#             props (dict): Additional properties for the command.
#             timeout (int): The timeout in seconds.

#         Returns:
#             dict: The result of sending the command, containing an exit status.
#         """
#         try:
#             self.kafka_producer.send(topic_name, value={"type": command_type, "params": params})
#             return {"exit_status": 0}
#         except Exception as e:
#             logging.error(f"Error sending command to supervisor: {e}")
#             return {"exit_status": -1}

#     def send_async(self, topic_name: str, command_type: str, params: List[str], props: dict):
#         """
#         Send an asynchronous command to the supervisor.

#         Args:
#             topic_name (str): The name of the Kafka topic.
#             command_type (str): The type of the command.
#             params (List[str]): The parameters of the command.
#             props (dict): Additional properties for the command.
#         """
#         self.kafka_producer.send(topic_name, value={"type": command_type, "params": params})

# class IasValue:
#     """
#     Class representing an IASIO.

#     Attributes:
#         value (float): The value of the IASIO.
#         operational_mode (str): The operational mode of the IASIO.
#         validity (str): The validity of the IASIO.
#         client_id (str): The client ID of the IASIO.
#         data_type (str): The data type of the IASIO.
#         t0 (int): The first timestamp of the IASIO.
#         t1 (int): The second timestamp of the IASIO.
#         t2 (int): The third timestamp of the IASIO.
#         t3 (int): The fourth timestamp of the IASIO.
#         t4 (int): The fifth timestamp of the IASIO.
#         t5 (int): The sixth timestamp of the IASIO.
#         alarm_status (str): The alarm status of the IASIO.
#         cleared_status (str): The cleared status of the IASIO.
#     """

#     def __init__(self, value: float, operational_mode: str, validity: str, client_id: str, data_type: str, t0: int, t1: int, t2: int, t3: int, t4: int, t5: int, alarm_status: str, cleared_status: str):
#         self.value = value
#         self.operational_mode = operational_mode
#         self.validity = validity
#         self.client_id = client_id
#         self.data_type = data_type
#         self.t0 = t0
#         self.t1 = t1
#         self.t2 = t2
#         self.t3 = t3
#         self.t4 = t4
#         self.t5 = t5
#         self.alarm_status = alarm_status
#         self.cleared_status = cleared_status

# class IasiosProducer:
#     """
#     Class for producing IASIOs.

#     Attributes:
#         kafka_producer (KafkaProducer): The Kafka producer used to produce IASIOs.
#     """

#     def __init__(self, kafka_brokers: str, topic_name: str):
#         self.kafka_producer = KafkaProducer(bootstrap_servers=kafka_brokers)

#     def send_iasio(self, ias_value: IasValue):
#         """
#         Send an IASIO to the supervisor.

#         Args:
#             ias_value (IasValue): The IASIO to be sent.
#         """
#         self.kafka_producer.send(topic_name, value=ias_value.__dict__)

# class IasiosConsumer:
#     """
#     Class for consuming IASIOs.

#     Attributes:
#         kafka_consumer (KafkaConsumer): The Kafka consumer used to consume IASIOs.
#     """

#     def __init__(self, kafka_brokers: str, topic_name: str, client_id: str):
#         self.kafka_consumer = KafkaConsumer(bootstrap_servers=kafka_brokers, group_id=client_id)

#     def start_getting_events(self):
#         """
#         Start getting events from the IASIOs.

#         Returns:
#             None
#         """
#         for message in self.kafka_consumer:
#             logging.info(f"Received IASIO: {message.value}")
#             # Process the received IASIO
#             ias_value = IasValue(**message.value)
#             alarms_received[ias_value.client_id] = ias_value

# class SupervisorProcess:
#     """
#     Class representing a supervisor process.

#     Attributes:
#         supervisor_proc (Process): The supervisor process.
#     """

#     def __init__(self, kafka_brokers: str):
#         self.supervisor_proc = Process(["supervisor", "SupervisorWithKafka"])

#     def start(self):
#         """
#         Start the supervisor process.

#         Returns:
#             None
#         """
#         try:
#             self.supervisor_proc.command.run()
#             logging.info("Supervisor started")
#         except Exception as e:
#             logging.error(f"Error starting supervisor: {e}")

# class TestHarness:
#     """
#     Class for testing IASIOs.

#     Attributes:
#         kafka_producer (KafkaProducer): The Kafka producer used to send commands.
#         supervisor_process (SupervisorProcess): The supervisor process.
#     """

#     def __init__(self, kafka_brokers: str):
#         self.kafka_producer = KafkaProducer(bootstrap_servers=kafka_brokers)
#         self.supervisor_process = SupervisorProcess(kafka_brokers)

#     def before_all(self):
#         """
#         Method to be called before all tests.

#         Returns:
#             None
#         """
#         logging.info("Starting test")
#         self.supervisor_process.start()
#         time.sleep(15)  # Wait for supervisor to start
#         self.iasios_consumer.start_getting_events()

#     def after_all(self):
#         """
#         Method to be called after all tests.

#         Returns:
#             None
#         """
#         logging.info("Stopping test")
#         self.iasios_consumer.stop_getting_events()
#         self.kafka_producer.close()

# alarms_received = {}

# class TestPyAck(unittest.TestCase):

#     def setUp(self):
#         """
#         Setup method for the test class.

#         Returns:
#             None
#         """
#         self.kafka_brokers = "localhost:9092"
#         self.topic_name = "iasio_topic"
#         self.client_id = uuid4()

#     def test_send_iasio(self):
#         """
#         Test sending an IASIO.

#         Returns:
#             None
#         """
#         iasios_prod = IasiosProducer(self.kafka_brokers, self.topic_name)
#         ias_value = IasValue(5.0, "operational", "reliable", self.client_id, "double", 100, 101, 102, 103, 104, 105, None, None)
#         iasios_prod.send_iasio(ias_value)

#     def test_send_async(self):
#         """
#         Test sending an asynchronous command.

#         Returns:
#             None
#         """
#         cmd_sender = CommandSender(self.kafka_brokers, self.client_id)
#         reply = cmd_sender.send_sync("ack_topic", "ACK", [self.client_id], {"comment": "User provided comment for ACK"}, 15)
#         assert reply["exit_status"] == 0




