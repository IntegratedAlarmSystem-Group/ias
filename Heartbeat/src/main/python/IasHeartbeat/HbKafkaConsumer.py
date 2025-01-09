from IasKafkaUtils.IasKafkaConsumer import IasLogListener, IasLogConsumer
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IASLogging.logConf import Log
from IasHeartbeat.HearbeatMessage import HeartbeatMessage

class HeartbeatListener:
    """
    The callback invoked when a new HB is read from the kafka topic
    """
    def iasHbReceived(hb: HeartbeatMessage):
         """
         The method involekd to notify of a new Hb

         Must be overridden.

         Args:
            hb: The hearbeat received from the kafka topic
         """
         raise NotImplementedError("Must override this method to get events")

class HbKafkaConsumer(IasLogListener):
    """
    The listener of HBs from the kafka topic
    """    
    
    def __init__(self,
                 kafkabrokers: str,
                 clientid: str,
                 groupid: str,
                 listener: HeartbeatListener):
        """
        Constructor

        Args:
            kafakabroker: the URL of the kafka brokers
            clientid: the client id for the kafka consumer
            groupid: the group id for the kafka consumer
            listener: the listener to be notified of ne HBs
        """
        self.logger = Log.getLogger(__name__)
        self.listener = listener
        self.log_consumer = IasLogConsumer(self,
                                           kafkabrokers,
                                           IasKafkaHelper.topics['hb'],
                                           clientid,
                                           groupid)

    def __del__(self):
        """
        Destructor
        """
        self.close()

    def start(self, assgnemntTimeout = 0) -> bool:
        """
        Start getting HBs

        Args:
            assgnemntTimeout the time to wait for the consumer to be assigned to the topic
        Return:
            True if the consumer is subscribed to the topic
        """
        return self.log_consumer.start(assgnemntTimeout)

    def close(self):
        self.log_consumer.close()
        self.logger.debug("Closed")

    def iasLogReceived(self, log: str) -> None:
        """
        Callback invoked when a new string is read from the kafka topic

        Parse the string and invokes the callback
        """
        if not log or not self.listener:
            self.logger.debug("Empty kafka log for listner: HB even discarded")
            return
        self.logger.debug("Hb received: %s", log)
        try:
            hbm = HeartbeatMessage.fromJSON(log)
        except Exception as e:
            self.logger.error("Exception caught while parsing the JSON HB %s: %s", log, str(e))
            return
        try:
            self.listener.iasHbReceived(hbm)
        except Exception as e:
            self.logger.error("Error resturning the user defined callaback: %s", str(e))