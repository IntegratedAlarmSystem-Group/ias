import uuid
import time
from threading import Lock
import logging

import pytest
from confluent_kafka import Producer

from IasLogging.log import Log
from IasHeartbeat.HearbeatMessage import HeartbeatMessage
from IasHeartbeat.HbKafkaConsumer import HeartbeatListener, HbKafkaConsumer
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasHeartbeat.Heartbeat import Heartbeat
from IasHeartbeat.HeartbeatProducerType import HeartbeatProducerType
from IasHeartbeat.HeartbeatStatus import HeartbeatStatus
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp

class HbListner(HeartbeatListener):
    def __init__(self):
        """
        Constructor
        """
        self._logger = logging.getLogger(HbListner.__name__)
        # The HBs read from the topic
        self.hbs: list[HeartbeatMessage] = []

        self._mutex: Lock = Lock()

    def get_recv_hbs(self)-> list[HeartbeatMessage]:
        """
        Return a copy of the HB received
        """
        with self._mutex:
            return self.hbs.copy()
    
    def clear(self)->None:
        """
        Clear the list of HBs received
        """
        with self._mutex:
            self.hbs.clear()

    def iasHbReceived(self, hb: HeartbeatMessage):
        """
        The callback
        """
        with self._mutex:
            self.hbs.append(hb)
            self._logger.info("HB received: %d hbs in the container", len(self.hbs))
        print(hb.toJSON())


class TestHbConsumer():

    @pytest.fixture(scope="class", autouse=True)
    def setup_class(self, request):
        Log.init_logging(__file__)
        request.cls.logger = logging.getLogger(TestHbConsumer.__name__)
        conf = { 
            'bootstrap.servers': IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 
            'client.id': "HbConsumerTest-Prod",
            'acks': 'all',
            "enable.idempotence": True,}
        request.cls.hbProducer = Producer(conf)

        request.cls.hb_listener = HbListner()

    def pushHb(self, hbm: HeartbeatMessage) -> None:
        assert hbm is not None
        hbMsgStr = hbm.toJSON()
        self.hbProducer.produce(topic=IasKafkaHelper.topics['hb'], value=hbMsgStr)
        self.hbProducer.flush()

    def test_get_hb_from_topic(self):
        # Setup the consumer
        id = "HbClient-"+str(uuid.uuid4())
        self.hb_listener.clear()
        hbConsumer = HbKafkaConsumer(IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
                                     id,
                                     id,
                                     self.hb_listener)
        # Starts the consumer and wait for the assignet to the topic
        assert hbConsumer.start(30)

        hb = Heartbeat(HeartbeatProducerType.CLIENT,"client_name","host_name")
        timestamp = Iso8601TStamp.now()
        hbm = HeartbeatMessage(tStamp=timestamp,
                               hbStringrepRepr=hb.stringRepr,
                               props=None,
                               hbStatus=HeartbeatStatus.STARTING_UP)
        
        self.pushHb(hbm)

        # Wait until the HB is received or timeout
        timeout = time.time()+30
        while len(self.hb_listener.get_recv_hbs())==0 and time.time()<timeout:
            print("Waiting HB...")
            time.sleep(.250)

        recv_hbs = self.hb_listener.get_recv_hbs()
        assert len(recv_hbs) == 1
        recvHb: HeartbeatMessage = recv_hbs[0]
        assert recvHb.timestamp == timestamp
        assert recvHb.state == HeartbeatStatus.STARTING_UP
        assert recvHb.hbStringrepresentation == hb.stringRepr
        assert recvHb.props is None
        hbConsumer.close()
