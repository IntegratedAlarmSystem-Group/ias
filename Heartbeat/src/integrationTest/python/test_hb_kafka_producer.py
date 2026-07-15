import logging
import socket
from queue import Queue

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

class TestHbKafkaProducer():

    @classmethod
    def setup_class(cls):
        cls.logger = logging.getLogger(TestHbKafkaProducer.__name__)
        cls._log_container = Queue()

        cls._hb_listener = HbListener(cls._log_container)

        cls._consumer = IasLogConsumer(
            clientid="HbKafkaConsumerTest",
            kafkabrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
            listener=cls._hb_listener,
            groupid="TestGroupId",
            topic=IasKafkaHelper.topics['hb'])
        
        cls.logger.info("Starting the consumer oh HB logs...")
        assert cls._consumer.start(waitAssigmentTimeout=10)
        cls.logger.info("HB consumer started")

    def test_hb_content(self):
        """
        Test the sending of one HB from the HB kafka producer
        and check if the content the HB received matches with the one sent
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

        if not TestHbKafkaProducer._consumer.isSubscribed():
            TestHbKafkaProducer.logger.warning("Consumer NOT subscribed")

        producer.send(hb=hb, hb_status=hb_status, props=props,tstamp=tstamp)

        hb_msg = TestHbKafkaProducer._log_container.get(timeout=5)
        TestHbKafkaProducer.logger.info("HB received %s", hb_msg.toJSON())

        assert hb_msg.state == hb_status
        assert hb_msg.timestamp == tstamp
        assert hb_msg.props == props

        recv_hb = Heartbeat.fromStringRepr(hb_msg.hbStringrepresentation)
        assert hb.hbType == recv_hb.hbType
        assert hb.name == recv_hb.name
        assert hb.hostname == recv_hb.hostname

        TestHbKafkaProducer.logger.debug("Closing the HB producer")
        producer.close()
        TestHbKafkaProducer.logger.info("Producer closed. Test done")

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
        hb_msg1 = TestHbKafkaProducer._log_container.get(timeout=5)
        TestHbKafkaProducer.logger.info("First HB receivev")
        assert hb_msg1.state == HeartbeatStatus.PARTIALLY_RUNNING

        hb_msg2 = TestHbKafkaProducer._log_container.get(timeout=5)
        TestHbKafkaProducer.logger.info("Second HB receivev")
        assert hb_msg2.state == HeartbeatStatus.RUNNING

        hb_msg3 = TestHbKafkaProducer._log_container.get(timeout=5)
        TestHbKafkaProducer.logger.info("Third HB receivev")
        assert hb_msg3.state == HeartbeatStatus.EXITING


        