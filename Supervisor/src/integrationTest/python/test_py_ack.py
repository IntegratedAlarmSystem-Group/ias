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


import subprocess
import time
import uuid

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

class TestPyAck():

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
    def setup_class(cls):
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
    def teardown_class(cls):
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

    def test_ack_from_python(self):
        logger.info("Test started")

        value = TestPyAck.buildIasio(0)
        TestPyAck.iasioProducer.send(value)
        # Give the supervisor time to produce the alarm 
        time.sleep(10)
        alarm = TestPyAck.iasioListener.last_alarm_received
        assert alarm is not None
        assert not alarm.is_set()
        assert alarm.is_acked()

        # set the alarm
        value = TestPyAck.buildIasio(100)
        TestPyAck.iasioProducer.send(value)
        # Give the supervisor time to produce the alarm 
        time.sleep(10)
        alarm = TestPyAck.iasioListener.last_alarm_received
        assert alarm is not None
        assert alarm.is_set()
        assert not alarm.is_acked()

        # Send the ACk command to the Supervisor
        logger.info("Sending the ACK to the Supervisor")
        ack_cmd_params = [TestPyAck.iasioListener.last_alarm_iasio.fullRunningId,
                          "User provided comment for ACK from python"]
        reply = TestPyAck.cmdSender.send_sync(TestPyAck.supervisorId, 
                                              IasCommandType.ACK,
                                              ack_cmd_params,
                                              None,
                                              30)
        assert reply  is not None
        assert reply.exitStatus == reply.exitStatus.OK
        logger.info("Reply from cmd %s", str(reply.exitStatus.name))

        # Give the supervisor time to produce the ACK'ed alarm 
        time.sleep(10)
        alarm = TestPyAck.iasioListener.last_alarm_received
        assert alarm  is not None
        assert alarm.is_set()
        assert alarm.is_acked()

        logger.info("Test done")
