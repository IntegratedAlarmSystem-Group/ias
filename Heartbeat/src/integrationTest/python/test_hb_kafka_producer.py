import logging
import socket
import uuid
from queue import Queue, Empty

import pytest

from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasKafkaUtils.IasKafkaConsumer import IasLogConsumer, IasLogListener
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasHeartbeat.HearbeatMessage import HeartbeatMessage
from IasHeartbeat.HbKafkaProducer import HbKafkaProducer
from IasHeartbeat.Heartbeat import Heartbeat
from IasHeartbeat.HeartbeatProducerType import HeartbeatProducerType
from IasHeartbeat.HeartbeatStatus import HeartbeatStatus

class HbListener(IasLogListener):
    """
    The listener of HBs
    """
    def __init__(self, logs_container: Queue[HeartbeatMessage]):
        self._logger = logging.getLogger(self.__class__.__name__)
        self._logs_container = logs_container

    def iasLogReceived(self, log: str) -> None:
        self._logger.info("HB received %s", log)
        hb_msg = HeartbeatMessage.fromJSON(log)
        self._logs_container.put(hb_msg)

    def clear(self):
        while True:
            try:
                self._logs_container.get_nowait()
                self._logs_container.task_done()  # only if you're using task tracking
            except Empty:
                break

class TestHbKafkaProducer():

    @pytest.fixture(scope="class", autouse=True)
    def setup_class(self, request):
        request.cls.logger = logging.getLogger(TestHbKafkaProducer.__name__)
        request.cls._log_container = Queue()

        request.cls._hb_listener = HbListener(request.cls._log_container)

    @pytest.fixture(autouse=True)
    def setup(self):
        # Fixuture executed before each test
        self._hb_listener.clear()

        self._consumer = IasLogConsumer(
            clientid="HbKafkaConsumerTest",
            kafkabrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
            listener=self._hb_listener,
            groupid="TestGroupId"+str(uuid.uuid4()),
            topic=IasKafkaHelper.topics['hb'])
        
        self.logger.info("Starting the consumer of HB logs...")
        assert self._consumer.start(waitAssigmentTimeout=15)
        self.logger.info("HB consumer started")

        yield
        # Fixuture executed after each test
        self._consumer.close()

    def test_hb_content(self):
        """
        Test the sending of one HB from the HB kafka producer
        and check if the content the HB received matches with the HB just sent
        """
        producer = HbKafkaProducer(
            clientid="HbKafkaProducerTest", 
            kafkabrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
        
        hb = Heartbeat(
            hbType=HeartbeatProducerType.CLIENT,
            name="TestClient",
            hostName=socket.gethostname())
        
        hb_status = HeartbeatStatus.RUNNING

        props = {"prop1": "val1", "prop2": "val2"}

        tstamp = Iso8601TStamp.now()

        if not self._consumer.isSubscribed():
            self.logger.warning("Consumer NOT subscribed")

        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)

        # Clear the queue even af at this point in time the HB could already have been received
        # but we want to be sure that the HB we are going to get is the one just sent
        self._hb_listener.clear()
        hb_msg = self._log_container.get(timeout=5)
        self.logger.info("HB received %s", hb_msg.toJSON())
        producer.close()
        self.logger.info("Producer closed. Test done")

        assert hb_msg.state == hb_status
        assert hb_msg.timestamp == tstamp
        assert hb_msg.props == props

        recv_hb = Heartbeat.fromStringRepr(hb_msg.hbStringrepresentation)
        assert hb.hbType == recv_hb.hbType
        assert hb.name == recv_hb.name
        assert hb.hostname == recv_hb.hostname

        self.logger.debug("Closing the HB producer")
        producer.close()
        

    def test_sending_hbs(self):
        """
        Test the sending of more HBs
        """
        producer = HbKafkaProducer(
            clientid="HbKafkaProducerTest", 
            kafkabrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
        
        hb = Heartbeat(
            hbType=HeartbeatProducerType.CLIENT,
            name="TestClient",
            hostName=socket.gethostname())
        
        hb_status = HeartbeatStatus.PARTIALLY_RUNNING

        props = {"prop1": "val1", "prop2": "val2"}

        tstamp = Iso8601TStamp.now()


        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)

        tstamp = Iso8601TStamp.now()
        hb_status = HeartbeatStatus.RUNNING
        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)

        tstamp = Iso8601TStamp.now()
        hb_status = HeartbeatStatus.EXITING
        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)

        # Get the 3 HBs
        hb_msg1 = self._log_container.get(timeout=5)
        self.logger.info("First HB receivedd")
        assert hb_msg1.state == HeartbeatStatus.PARTIALLY_RUNNING

        hb_msg2 = self._log_container.get(timeout=5)
        self.logger.info("Second HB received")
        assert hb_msg2.state == HeartbeatStatus.RUNNING

        hb_msg3 = self._log_container.get(timeout=5)
        self.logger.info("Third HB received")
        assert hb_msg3.state == HeartbeatStatus.EXITING

        producer.close()

    def test_send_when_closed(self):
        """
        Test if the sending is forbidden when the producer
        has been closed
        """
        producer = HbKafkaProducer(
            clientid="HbKafkaProducerTest", 
            kafkabrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
        
        hb = Heartbeat(
            hbType=HeartbeatProducerType.CLIENT,
            name="TestClient",
            hostName=socket.gethostname())
        
        hb_status = HeartbeatStatus.RUNNING

        props = {"prop1": "val1", "prop2": "val2"}

        tstamp = Iso8601TStamp.now()

        if not self._consumer.isSubscribed():
            self.logger.warning("Consumer NOT subscribed")

        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)

        self._hb_listener.clear()
        hb_msg = self._log_container.get(timeout=5)
        self.logger.info("HB received %s", hb_msg.toJSON())

        # Close the producer and check if HBs arrives
        producer.close()
        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)
        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)
        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)
        # The following is expected to fail when the timeout expires
        with pytest.raises(Empty):
            self._log_container.get(timeout=5)

