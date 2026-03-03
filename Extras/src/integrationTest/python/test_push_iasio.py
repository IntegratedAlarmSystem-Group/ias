"""
Test the pushging of IASIOs with iasIasPushIasio.
This test requires a running BSDB and Kafka instance, and the iasPushIasio.py script to be available in the PATH.

The test run miasPushIasio commands and checks if the IASIOs are pushed in the core topic of the BSDB
"""
import queue
import uuid
import subprocess
from threading import Event

from IASLogging.log import Log
from IasKafkaUtils.KafkaValueConsumer import KafkaValueConsumer, IasValueListener
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasBasicTypes.IasValue import IasValue

class IasioListener(IasValueListener):
    def __init__(self, iasios_received: queue.Queue):
        super().__init__()
        # The last alarm received
        self.last_iasio_received: IasValue|None = None

        self.iasios_received: queue.Queue = iasios_received

    def clear(self):
        self.last_iasio_received = None
        while True:
            try:
                self.iasios_received.get_nowait()
            except queue.Empty:
                break

    def iasValueReceived(self, iasValue: IasValue):
        print(f"IasValue received: {iasValue.toString()}")
        self.last_iasio_received = iasValue
        self.iasios_received.put(iasValue)
        print(f"IASIO received {self.last_iasio_received.toString()}")

class TestPushIaioScript():

    # The frId of the temperature IASIO
    temperature_frid = id = "(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(Temperature-ID:IASIO)"

    # The frId of the temperature alarm 
    alarm_frid = "(AlarmSuperv:SUPERVISOR)@(DasuTemperature:DASU)@(AsceTemperature:ASCE)@(TemperatureAlarm:IASIO)"

    @classmethod
    def setup_class(cls):
        Log.init_logging(__file__)
        # Create a queue to receive the IASIOs
        cls.iasios_received = queue.Queue()

        cls.iasio_listener = IasioListener(cls.iasios_received)

        id = "coreListenerGId"+str(uuid.uuid4())
        cls.iasio_consumer = KafkaValueConsumer(cls.iasio_listener, 
                                       IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
                                       IasKafkaHelper.topics['core'], 
                                       id, 
                                       id)
        print("Connecting the IASIO listener")
        consumer_ready = Event()
        cls.iasio_consumer.start(ready_event=consumer_ready)
        print("Wait until the consumer is ready...")
        assert consumer_ready.wait(timeout=30), "Kafka not ready before timeout expired"
        print("IASIO listener connected")

    @classmethod
    def teardown_class(cls):
        print("Closing the IASIO listener")
        cls.iasio_consumer.close()
        print("IASIO listener closed")

    def test_push_iasio(self):
        print(f"Testing the pushing of an IASIO with iasPushIasio, isSubscribed={TestPushIaioScript.iasio_consumer.isSubscribed()}")
        TestPushIaioScript.iasio_listener.clear()
        cmd = [
            "iasPushIasio",
            "-i", TestPushIaioScript.alarm_frid,
            "-t", "ALARM",
            "-v", "SET_ACK:HIGH"]
        print(f"Running command: {' '.join(cmd)}")
        proc = subprocess.Popen(cmd, 
                                  shell=False,
                                  stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE,
                                  text=True
        )
        try:
            stdout, stderr = proc.communicate(timeout=10)
            
            print("STDOUT:", stdout)
            print("STDERR:", stderr)
            print("RETURN CODE:", proc.returncode)

        except subprocess.TimeoutExpired:
            proc.kill()
            assert False, "iasPushIasio command did not complete in time"

        assert proc.returncode==0, f"iasPushIasio command failed with return code {proc.returncode}."
        print("iasPushIasio command executed successfully")

        iasio = None
        try:
            print("Waiting for the IASIO to be received from the BSDB...")
            iasio = TestPushIaioScript.iasios_received.get(timeout=30)
        except queue.Empty:
            pass
        assert iasio, "No IASIO received from iasPushIasio"

        
        