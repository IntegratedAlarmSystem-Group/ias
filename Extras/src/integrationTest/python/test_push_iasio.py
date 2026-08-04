"""
Test the pushging of IASIOs with iasIasPushIasio.
This test requires a running BSDB and Kafka instance, and the iasPushIasio.py script to be available in the PATH.

The test run miasPushIasio commands and checks if the IASIOs are pushed in the core topic of the BSDB
"""
import queue
import uuid
import subprocess
import logging
from threading import Event
import time

from IasLogging.log import Log
from IasKafkaUtils.KafkaValueConsumer import KafkaValueConsumer, IasValueListener
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasBasicTypes.IasValue import IasValue

class IasioListener(IasValueListener):
    def __init__(self, iasios_received: queue.Queue):
        super().__init__()
        self.logger=logging.getLogger(IasioListener.__name__)
        # The last alarm received
        self.last_iasio_received: IasValue|None = None

        self.queue: queue.Queue = iasios_received

    def clear(self):
        self.logger.debug("Clearing the queue of IasValues")
        self.last_iasio_received = None
        while True:
            try:
                self.queue.get_nowait()
            except queue.Empty:
                break
        self.logger.debug("Queue empty")

    def iasValueReceived(self, iasValue: IasValue):
        self.logger.info(f"IasValue received: {iasValue.toString()}")
        self.last_iasio_received = iasValue
        self.queue.put(iasValue)
        self.logger.info("IasValue in queue")

class TestPushIasioScript():

    # The frId of the temperature IASIO
    temperature_frid = id = "(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(Temperature-ID:IASIO)"

    # The frId of the temperature alarm 
    alarm_frid = "(AlarmSuperv:SUPERVISOR)@(DasuTemperature:DASU)@(AsceTemperature:ASCE)@(TemperatureAlarm:IASIO)"

    @classmethod
    def setup_class(cls):
        Log.init_logging(__file__)
        cls.LOGGER = logging.getLogger(TestPushIasioScript.__name__)
        # Create a queue to receive the IASIOs
        cls.iasios_received = queue.Queue()

        cls.iasio_listener = IasioListener(cls.iasios_received)

        id = "coreListenerGId"+str(uuid.uuid4())
        cls.iasio_consumer = KafkaValueConsumer(cls.iasio_listener, 
                                       IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
                                       IasKafkaHelper.topics['core'], 
                                       id, 
                                       id)
        cls.LOGGER.info("Connecting the IASIO listener")
        consumer_ready = Event()
        cls.iasio_consumer.start(ready_event=consumer_ready)
        cls.LOGGER.info("Wait until the consumer is ready...")
        assert consumer_ready.wait(timeout=30), "Kafka not ready before timeout expired"
        cls.LOGGER.info("IASIO consumer connected")

    @classmethod
    def teardown_class(cls):
        cls.LOGGER.info("Closing the IASIO listener")
        cls.iasio_consumer.close()
        cls.LOGGER.info("IASIO listener closed")

    def test_push_iasio(self):
        TestPushIasioScript.LOGGER.info(f"Testing the pushing of an IASIO with iasPushIasio, isSubscribed={TestPushIasioScript.iasio_consumer.isSubscribed()}")
        TestPushIasioScript.iasio_listener.clear()
        assert TestPushIasioScript.iasio_consumer.isGettingValues(), "KafkaValueConsumer not subscribed"
        cmd = [
            "iasPushIasio",
            "-i", TestPushIasioScript.alarm_frid,
            "-t", "ALARM",
            "-v", "SET_ACK:HIGH"]
        TestPushIasioScript.LOGGER.info(f"Running command: {' '.join(cmd)}")
        # time.sleep(2)
        proc = subprocess.Popen(cmd, 
                                  shell=False,
                                  stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE,
                                  text=True
        )
        try:
            stdout, stderr = proc.communicate(timeout=10)
            
            print("iasPushIasio STDOUT:", stdout)
            print("iasPushIasio STDERR:", stderr)
            print("iasPushIasio RETURN CODE:", proc.returncode)

        except subprocess.TimeoutExpired:
            proc.kill()
            assert False, "iasPushIasio command did not complete in time"

        assert proc.returncode==0, f"iasPushIasio command failed with return code {proc.returncode}."
        TestPushIasioScript.LOGGER.info("iasPushIasio command executed successfully")

        iasio = None
        try:
            TestPushIasioScript.LOGGER.info("Waiting for the IASIO to be received from the BSDB...")
            iasio = TestPushIasioScript.iasios_received.get(timeout=30)
            TestPushIasioScript.LOGGER.info("IASIO received")
        except queue.Empty:
            TestPushIasioScript.LOGGER.error("NO IASIO received")
        assert iasio, "No IASIO received from iasPushIasio"

        
        
