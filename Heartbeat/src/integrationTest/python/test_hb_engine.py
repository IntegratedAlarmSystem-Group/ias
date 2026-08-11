import logging
import socket
import uuid
import time
from queue import Queue, Empty

import pytest

from IasLogging.log import Log
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasHeartbeat.HbKafkaConsumer import HbKafkaConsumer, HeartbeatListener
from IasHeartbeat.HbKafkaProducer import HbKafkaProducer
from IasHeartbeat.HbEngine import HbEngine
from IasHeartbeat.HearbeatMessage import HeartbeatMessage
from IasHeartbeat.Heartbeat import Heartbeat
from IasHeartbeat.HeartbeatProducerType import HeartbeatProducerType
from IasHeartbeat.HeartbeatStatus import HeartbeatStatus

class HbListener(HeartbeatListener):
    """
    The listener of HBs
    """
    def __init__(self):
        super().__init__()
        self._logger = logging.getLogger(self.__class__.__name__)
        self._queue: Queue[HeartbeatMessage] = Queue()
    
    def clear(self):
        while True:
            try:
                self._queue.get_nowait()
                self._queue.task_done()  # only if you're using task tracking
            except Empty:
                break

    def iasHbReceived(self, hb: HeartbeatMessage):
        self._logger.info("HB received %s", hb.toJSON())
        self._queue.put(hb)

    def get(self, block=True, timeout=None)->HeartbeatMessage:
        """
        Get and remove an element from the queue by delegating to 
        self._queue

        Remove and return an item from the queue.
        If optional args block is true and timeout is None (the default), 
        block if necessary until an item is available. If timeout is a positive number, 
        it blocks at most timeout seconds and raises the Empty exception if no item was available 
        within that time.
        
        Otherwise (block is false), return an item if one is immediately available,
        else raise the Empty exception (timeout is ignored in that case).
        """
        return self._queue.get(block, timeout)

@pytest.mark.usefixtures("setup_class_fix")
class TestHbEngine():
    """
    Test the HbEngine using the HB controller and consumer
    """
    @pytest.fixture(scope="class", autouse=True)
    def setup_class_fix(self, request):
        Log.init_logging(__file__)
        request.cls.logger = logging.getLogger(TestHbEngine.__name__)

        request.cls.hb_listener = HbListener()

        cons_id = "HbKafkaConsTest"+str(uuid.uuid4())
        request.cls.consumer = HbKafkaConsumer(
            clientid=cons_id, 
            groupid=cons_id, 
            kafkabrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
            listener=request.cls.hb_listener)
        
        request.cls.consumer.start(assgnemntTimeout=30)
    
    @pytest.fixture(autouse=True)
    def setup(self):
        # Fixture executed before each test
        self.producer = HbKafkaProducer(
                clientid="HbKafkaProducerTest-prod", 
                kafkabrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
        self.hb_listener.clear()
        self.hb = Heartbeat(
            hbType=HeartbeatProducerType.CORETOOL,
            name="HB-Test",
            hostName=socket.gethostname())
        self.hb_engine = HbEngine(
            hb=self.hb,
            frequency=3,
            producer=self.producer)
        
        yield 
        # What follows is executed after the test
        self.logger.info("Closing the HbEngine")
        self.hb_engine.close()
        self.logger.info("HbEngine closed")


    def test_default_initial_state(self):
        # Start the HB engine with no state so it shall use the default
        self.hb_engine.start()
        msg = self.hb_listener.get(timeout=5)

        assert msg.state == HeartbeatStatus.STARTING_UP

    def test_custom_initial_state(self):
        # Start the HB engine with no state so it shall use the default
        self.hb_engine.start(HeartbeatStatus.RUNNING)
        msg = self.hb_listener.get(timeout=5)

        assert msg.state == HeartbeatStatus.RUNNING


    def test_changing_state(self):
        # Start the HB engine with no state so it shall use the default
        self.hb_engine.start()
        msg = self.hb_listener.get(timeout=5)

        assert msg.state == HeartbeatStatus.STARTING_UP

        self.hb_engine.update_hb_state(HeartbeatStatus.PARTIALLY_RUNNING)
        time.sleep(3.5)
        msg = self.hb_listener.clear()

        msg = self.hb_listener.get(timeout=5)

        assert msg.state == HeartbeatStatus.PARTIALLY_RUNNING


    def test_changing_props(self):
        # Start the HB engine with no state so it shall use the default
        self.hb_engine.start()
        msg = self.hb_listener.get(timeout=5)

        assert msg.state == HeartbeatStatus.STARTING_UP

        assert not msg.props

        self.hb_engine.update_props({"Key1": 10, "Key2":100})

        msg = self.hb_listener.clear()
        msg = self.hb_listener.get(timeout=5)
        tries = 0
        while not msg.props and tries < 5:
            msg = self.hb_listener.get(timeout=5)
            tries += 1

        assert len(msg.props) == 2
        assert msg.props["Key1"] == 10
        assert msg.props["Key2"] == 100


    def test_send_when_closed(self):
        # Start the HB engine with no state so it shall use the default
        self.hb_engine.start()
        msg = self.hb_listener.get(timeout=5)

        assert msg.state == HeartbeatStatus.STARTING_UP

        self.hb_engine.close()
        msg = self.hb_listener.clear()

        with pytest.raises(Empty):
            self.hb_listener.get(timeout=5)
