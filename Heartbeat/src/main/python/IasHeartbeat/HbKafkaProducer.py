import logging
from confluent_kafka import Producer

from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasHeartbeat.Heartbeat import Heartbeat
from IasHeartbeat.HeartbeatStatus import HeartbeatStatus
from IasHeartbeat.HearbeatMessage import HeartbeatMessage

class HbKafkaProducer:
    """
    The kafka producer of HBs
    """

    # The name of the topic to push HBs into
    topic = IasKafkaHelper.topics['hb']

    def __init__(self,
                 kafkabrokers,
                 clientid):
        '''
        Constructor
        
        @param kafkabroker: the kafka servers to connect to
        @param clientId: the id of the kafka client
        '''
        if not kafkabrokers:
            raise ValueError("Invalid kafka brokers")
        if not clientid:
            raise ValueError('Invalid client ID')
        
        self._logger = logging.getLogger(self.__class__.__name__)
        self._logger.debug("Building HB kafka producer to connect to %s, with topic %s and id %s",
                          kafkabrokers, HbKafkaProducer.topic, clientid)
        
        self.client_id = clientid
        self.kafkabrokers = kafkabrokers

        prod_conf = { 'bootstrap.servers': kafkabrokers, 'client.id': clientid}
        self.producer = Producer(prod_conf)

        self.closed = False

    def send(self,
             hb: Heartbeat,
             hb_status: HeartbeatStatus,
             props: dict[str, str]|None = None,
             tstamp: str|None = None)->None:
        '''
        Async send an heartbeat to the kafka topic
        
        @param hb: the Heartbeat to publish
        @param hb_status The heartbeat status
        @param props optional additional properties to push
        @param tstamp ISO-8601 timestamp if not it is set to the actual time
        @return the feature to be informed when the value has been sent
        '''
        if self.closed:
            self._logger.warning(f"Producer closed: will not send this HB: {hb.stringRepr} with status {hb_status._name_}")
            return
        if not hb:
            raise ValueError("Invalid Heartbeat to publish")
        if not hb_status:
            raise ValueError("Invalid HB status to publish")
        
        msg = self._serialize(hb, hb_status, props, tstamp)
        
        self.producer.produce(topic=self.topic, value=msg)
        self._logger.debug(f"HB sent {msg}")
        self.producer.flush()

    def _serialize(self,
                   hb: Heartbeat,
                   hb_status: HeartbeatStatus,
                   props: dict[str, str]|None = None,
                   tstamp: str|None = None)->str:
        """
        Serialize the message to be sent to the topic

        @param hb: the Heartbeat to publish
        @param hb_status The heartbeat status
        @param props optional additional properties to push
        @param tstamp ISO-8601 timestamp if None it is set to the actual time
        @return The string to publish in the kafka topic
        """
        if not tstamp:
            tstamp = Iso8601TStamp.now()

        hb_msg = HeartbeatMessage(
            hbStringrepRepr=hb.stringRepr,
            hbStatus=hb_status,
            tStamp=tstamp,
            props=props)

        return hb_msg.toJSON()

    def flush(self):
        '''
        Flush: delegates to the kafka producer
        '''
        if self.producer is not None:
            self.producer.flush()
            self._logger.debug("Flushed")
        
    def close(self):
        '''
        Close the producer: delegates to the kafka producer
        '''
        if self.closed:
            self._logger.warning("Already closed")
            return
        self.closed = True
        self._logger.debug("Closing")

        if self.producer is not None:
            self._logger.debug("Closing")
            self.producer.flush()   
            # The Producer has no close method so we just force a flush
            # But the close() exists in newer version of confluent Kakfa 
            # so swe will need to uncomment in future
            # self.producer.close()
        self._logger.info("Closed")
