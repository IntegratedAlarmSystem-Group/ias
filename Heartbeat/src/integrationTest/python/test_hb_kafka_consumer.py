import uuid
import time

from confluent_kafka import Producer
from IasHeartbeat.HearbeatMessage import HeartbeatMessage
from IasHeartbeat.HbKafkaConsumer import HeartbeatListener, HbKafkaConsumer
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasHeartbeat.IasHeartbeat import IasHeartbeat
from IasHeartbeat.IasHeartbeatProducerType import IasHeartbeatProducerType
from IasHeartbeat.IasHeartbeatStatus import IasHeartbeatStatus
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp

class HbListner(HeartbeatListener):
    def __init__(self):
        """
        Constructor
        """
        # The HBs read from the topic
        self.hbs: list[HeartbeatMessage] = []

    def iasHbReceived(self, hb: HeartbeatMessage):
        """
        The callback
        """
        self.hbs.append(hb)
        print(hb.toJSON())


class HbConsumerTest():

    # The Kafka producer of HBs
    hbProducer: Producer = None

    @classmethod
    def setup_class(cls):
        conf = { 'bootstrap.servers': IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, 'client.id': "HbConsumerTest-Prod"}
        HbConsumerTest.hbProducer = Producer(conf)

    @classmethod
    def pushHb(cls, hbm: HeartbeatMessage) -> None:
        assert hbm is not None
        hbMsgStr = hbm.toJSON()
        HbConsumerTest.hbProducer.produce(topic=IasKafkaHelper.topics['hb'], value=hbMsgStr)
        HbConsumerTest.hbProducer.flush()

    def test_get_hb_from_topic(self):
        listener = HbListner()
        # Setup the consumer
        id = "HbClient-"+str(uuid.uuid4())
        hbConsumer = HbKafkaConsumer(IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
                                     id,
                                     id,
                                     listener)
        # Starts the consumer and wait for the assignet to the topic
        assert hbConsumer.start(30)

        hb = IasHeartbeat(IasHeartbeatProducerType.CLIENT,"client_name","host_name")
        timestamp = Iso8601TStamp.now()
        hbm = HeartbeatMessage(tStamp=timestamp,
                               hbStringrepRepr=hb.stringRepr,
                               props=None,
                               hbStatus=IasHeartbeatStatus.STARTING_UP)
        
        HbConsumerTest.pushHb(hbm)

        # Wait until the HB is received or timeout
        timeout = time.time()+30
        while len(listener.hbs)==0 and time.time()<timeout:
            print("Waiting HB...")
            time.sleep(.250)

        assert len(listener.hbs) == 1
        recvHb: HeartbeatMessage = listener.hbs[0]
        assert recvHb.timestamp == timestamp
        assert recvHb.state == IasHeartbeatStatus.STARTING_UP
        assert recvHb.hbStringrepresentation == hb.stringRepr
        assert recvHb.props is None
        hbConsumer.close()
