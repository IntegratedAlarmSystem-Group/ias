'''

Test the ACK of alarms through python API (i.e. throught IasExtras.AlarmAck )

Created on Feb 12, 2026

@author: acaproni
'''
import time
import subprocess
import uuid
import pytest
import queue

from IASLogging.logConf import Log
from IasKafkaUtils.KafkaValueConsumer import KafkaValueConsumer, IasValueListener
from IasKafkaUtils.KafkaValueProducer import KafkaValueProducer
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasBasicTypes.IasType import IASType
from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasCmdReply.IasCommandSender import IasCommandSender
from IasCmdReply.IasCommandType import IasCommandType

from IasExtras.AlarmAck import AlarmAck

logger = Log.getLogger(__file__)

@pytest.fixture(scope="session", autouse=True)
def session_setup():
    logger.info("Starting the Supervisor")
    superv_proc = subprocess.Popen([ "iasSupervisor", "SupervisorForAck", "-j", "src/integrationTest"], shell=False)
    time.sleep(10)  # Wait for the Supervisor to start
    logger.info("Running tests")
    yield
    logger.info("Wait the termination of the supervisor")
    try:
        superv_proc.wait(5)
        logger.info("Supervisor terminated gracefully")
    except subprocess.TimeoutExpired:
        logger.warning("Supervisor did not terminate with command")
        superv_proc.kill()  

class IasioListener(IasValueListener):
    def __init__(self, alarms_received: queue.Queue):
        super().__init__()
        # The last alarm received
        self.last_alarm_received: Alarm|None = None
        # The IASIO with the last received alarm
        self.last_alarm_iasio: IasValue|None = None

        self.alarms_received: queue.Queue = alarms_received

    def clear(self):
        self.last_alarm_received = None
        self.last_alarm_iasio = None
        while True:
            try:
                self.alarms_received.get_nowait()
            except queue.Empty:
                break

    def iasValueReceived(self, iasValue: IasValue):
        logger.info("IasValue received: %s", iasValue.toString())
        if iasValue.valueType==IASType.ALARM:
            self.last_alarm_iasio = iasValue
            self.last_alarm_received = Alarm.fromString(iasValue.value)

            self.alarms_received.put(self.last_alarm_received)
            logger.info("Alarm received %s", self.last_alarm_received.to_string())

class TestPyAck():
    # The name of the supervisor
    supervisor_id = "SupervisorForAck"
    # Full running ID of the Supervisor
    supervisor_frid = f"({supervisor_id}:SUPERVISOR)"

    # The temperature to set to set the alarm
    temperature_frid = f"({supervisor_id}:SUPERVISOR)@(DasuTemperature:DASU)@(AsceTemperature:ASCE)@(Temperature:IASIO)"

    # Tha alarm to ACK
    alarm_frid = f"({supervisor_id}:SUPERVISOR)@(DasuTemperature:DASU)@(AsceTemperature:ASCE)@(TemperatureAlarm:IASIO)"

    # The alarms received from the IASIO consumer
    alarms_received = queue.Queue()

    iasio_listener = IasioListener(alarms_received)

    iasio_consumer = KafkaValueConsumer(iasio_listener, 
                                       IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
                                       IasKafkaHelper.topics['core'], 
                                       "coreListenerId", 
                                       "coreListenerGId"+str(uuid.uuid4()))
    
    iasio_producer = KafkaValueProducer(IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
                                       IasKafkaHelper.topics['core'], 
                                       "IasioProducerId")
    
    # The command sender fulrrunning ID
    cmd_sender_frid = "CmdSenderFullRunningID"

    # The ID of the sender for the command kafka topic
    senderId = "CmdSender-"+str(uuid.uuid4())

    cmd_sender = IasCommandSender(cmd_sender_frid, senderId, IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)

    @classmethod
    def buildIasio(cls, value: float) -> IasValue:
        """
        Builds the Temperature IASIO to trigger/clear the alarm

        Params:
            value The value to set in the IASIO
        Return:
            The IasValue to send to the core topic
        """
        now = Iso8601TStamp.now()
        json = f'{{"value":"{value}","readFromMonSysTStamp":"{now}","receivedFromPluginTStamp":"{now}","sentToConverterTStamp":"{now}",'
        json = f'{json}"receivedFromPluginTStamp":"{now}","convertedProductionTStamp":"{now}","sentToBsdbTStamp":"{now}","productionTStamp":"{now}",'
        json = f'{json}"mode":"OPERATIONAL","iasValidity":"RELIABLE",'
        json = f'{json}"fullRunningId":"{cls.temperature_frid}",'
        json = f'{json}"valueType":"DOUBLE"}}'
        logger.info(f"IASIO JSON str built: {json}")
        return IasValue.fromJSon(json)
    
    @classmethod
    def setup_class(cls):
        logger.info("Connecting the IASIO listener")
        cls.iasio_consumer.start()
        logger.info("Starting the command sender")
        TestPyAck.cmd_sender.set_up()

    @classmethod
    def teardown_class(cls):
        logger.info("Closing the IASIO listener")
        cls.iasio_consumer.close()

        # Send the command to terminate to the Supervisor
        logger.info(f"Sending CMD to shutdown the {TestPyAck.supervisor_id} Supervisor")
        TestPyAck.cmd_sender.send_sync(TestPyAck.supervisor_id, IasCommandType.SHUTDOWN)
        # Close the command sender
        TestPyAck.cmd_sender.close()

    def wait_alarm(self, timeout: float = 10) -> Alarm|None:
        """
        Waits for an alarm to be received from the IASIO consumer

        Params:
            timeout: The maximum time to wait for the alarm
        Return:
            The alarm received, or None if no alarm is received within the timeout
        """
        logger.info("Waiting for an alarm to be received from the IASIO consumer")
        TestPyAck.iasio_listener.clear()
        try:
            alarm = TestPyAck.alarms_received.get(timeout=timeout)
            logger.info(f"Alarm received within {timeout} seconds: {alarm.to_string()}")
            return alarm
        except queue.Empty:
            logger.error(f"No alarm received within {timeout} seconds")
            return None

    def test_ack(self):
        """
        Test the ACK of an alarm through python AI
        """
        logger.info("Test ACK of an alarm through python API")

        logger.info("Sending high temperature to let the supervisor generate the alarm")
        high_temp = TestPyAck.buildIasio(0.0)
        logger.info(f"Sending high temperature IASIO: {high_temp.toString()}")
        TestPyAck.iasio_producer.send(high_temp)
        alarm = self.wait_alarm()
        assert alarm is not None, "Alarm not received after sending high temperature IASIO"
        assert not alarm.is_set()

        # Send an high temperature to set the alarm
        logger.info("Sending high temperature to set the alarm")
        high_temp = TestPyAck.buildIasio(100.0)
        logger.info(f"Sending high temperature IASIO: {high_temp.toString()}")
        TestPyAck.iasio_producer.send(high_temp)

        tries = 0
        while not alarm.is_set() and tries<3:
            alarm = self.wait_alarm()
            assert alarm is not None, "No alarm received after sending high temperature IASIO"
            tries += 1
        assert alarm.is_set(), "Alarm is not set after sending high temperature IASIO"
        assert not alarm.is_acked(), "Alarm shold NOT be ACKed at this stage"

        # ACk the alarm through the python API
        logger.info("Acknowledging the alarm through the python API")
        acker = AlarmAck(full_running_id="(AlarmAckTest:CLIENT)",
                         command_sender=TestPyAck.cmd_sender)
        acker.start()
        
        acked = acker.ack(alarm_id=TestPyAck.alarm_frid,
                          supervisor_id=TestPyAck.supervisor_id,
                          timeout=0,
                          comment="Acknowledged by test")
        assert acked, "Failed to acknowledge the alarm"
        logger.info("Alarm ACKed")

        # Wait for the Supervisor to ACK the alarm
        tries = 0
        while not alarm.is_acked() and tries<3:
            alarm = self.wait_alarm()
            assert alarm is not None, "No alarm received"
            tries += 1
        assert alarm.is_acked(), "Alarm is still not ACKed after sending the ACK command"

        acker.close()

    def test_ack_cmd_line(self):
        """
        Test the ACK of an Alarm with command line tool (iasAckAlarm)

        This test is the same of the previous one but instead of using the AlarmAck clas,
        it runs iasAckAlarm
        """
        logger.info("Test ACK of an alarm through command line tool")
        
        logger.info("Sending high temperature to let the supervisor generate the alarm")
        high_temp = TestPyAck.buildIasio(0.0)
        logger.info(f"Sending high temperature IASIO: {high_temp.toString()}")
        TestPyAck.iasio_producer.send(high_temp)
        alarm = self.wait_alarm()
        assert alarm is not None, "Alarm not received after sending high temperature IASIO"
        assert not alarm.is_set()

        # Send an high temperature to set the alarm
        logger.info("Sending high temperature to set the alarm")
        high_temp = TestPyAck.buildIasio(100.0)
        logger.info(f"Sending high temperature IASIO: {high_temp.toString()}")
        TestPyAck.iasio_producer.send(high_temp)

        tries = 0
        while not alarm.is_set() and tries<3:
            alarm = self.wait_alarm()
            assert alarm is not None, "No alarm received after sending high temperature IASIO"
            tries += 1
        assert alarm.is_set(), "Alarm is not set after sending high temperature IASIO"
        assert not alarm.is_acked(), "Alarm shold NOT be ACKed at this stage"

        cmd = [
            "iasAckAlarm", 
            "-j", "src/integrationTest",
            "-a", TestPyAck.alarm_frid, 
            "-c", '"Acknowledged by command line tool"']
        logger.info(f"Running command: {' '.join(cmd)}")
        result = subprocess.Popen(cmd, shell=False)
        try:
            result.wait(10)
        except subprocess.TimeoutExpired:
            result.kill()
            assert False, "iasAckAlarm command did not complete within the timeout"

        assert result.returncode==0, f"iasAckAlarm command failed with return code {result.returncode}."
        logger.info("iasAckAlarm command executed successfully")

        # Wait for the Supervisor to ACK the alarm
        # This ensures that the alarm ID is correct
        tries = 0
        while not alarm.is_acked() and tries<3:
            alarm = self.wait_alarm()
            assert alarm is not None, "No alarm received"
            tries += 1
        assert alarm.is_acked(), "Alarm is still not ACKed after sending the ACK command"
