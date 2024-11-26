from queue import Queue, Empty
from threading import Thread
import time

from confluent_kafka import Producer

from IASLogging.logConf import Log
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasKafkaUtils.IasKafkaConsumer import IasLogConsumer, IasLogListener
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

from IasCmdReply.IasCommand import IasCommand
from IasCmdReply.IasReply import IasReply
from IasCmdReply.IasCmdExitStatus import IasCmdExitStatus

class IasCommandListener:
    """
    The listener of commands read from IAS cmd topic
    """

    def cmdReceived(self, cmd: IasCommand) -> IasCmdExitStatus:
        """
        The callback to notify of a new new command to process
        
        The implementation must override this method

        Args:
           cmd received from th ekafka topic
        Returns:
            the exit code afetr processing the command to be set in the reply
        """
        raise NotImplementedError("Must override this method to get commands")

class KafkaLogListener(IasLogListener):
    """
    The listener of comands from the kafka topic

    The listener discards the logs that are not for this process (identified
    by the full running ID).
    It stores the accpted logs in the queue together with the timestamp.
    The logs will be fetched and processed by the tread of the IasCommandManagerKafka object
    """
    def __init__(self, logs: Queue, full_run_id: str):
        """
        Constructor

        Params:
            logs: The queue to store IasCommands into
            full_run_id: the full runing ID of the process
        """
        self.logger = Log.getLogger(__name__)
        self.kafka_logs = logs
        self.fullRunningId = full_run_id

    def iasLogReceived(self, log: str) -> None:
        self.logger.debug(f"Received log from BSDB: {log}")
        cmd = IasCommand.fromJSon(log)
        if cmd.destId==self.fullRunningId or cmd.destId==IasCommand.BROADCAST_ADDRESS:
            self.kafka_logs.put((Iso8601TStamp.now(),cmd))
            self.logger.debug(f"Accepted log for processing: {log}")
        else:
            self.logger.debug(f"Log discared as it is not for this process: {log}")

class IasCmdManagerKafka(Thread):
    """
    This is the python equivalent of the java CommandManagerKafkaIMpl 
    but with rduced functionalities as up to now there are no python clients that get comamnds
    and send replies.

    This class is the counter part of IasCommandsSender:
     -  gets commands from the kafka command topic, 
     -  forwards them to the listener for precessing
     -  sends replies to the kafka reply topic
    """

    def __init__(self,
                 full_run_id,
                 listener: IasCommandListener,
                 kbrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
                 replyProducer: Producer|None=None,
                 kclient_id=__name__+Iso8601TStamp.now(),
                 kgroup_id=__name__+Iso8601TStamp.now()):
        """
        Constructor

        Params:
            full_run_id: the full runing ID of the process
            listener: the listener of commands
            replyProducer the producer of replies, if None a new one is built
            kbrokers Kafka brokers
            kclient_id The ID of the Kafka client
            kgroup_id The id of the Kafka group
        """
        if not full_run_id:
            raise ValueError("The full running ID cannot be None")
        if not listener:
            raise ValueError("The listerner of comamnds cannot be None")
        super().__init__()
        self.logger = Log.getLogger(__name__)
        self.fullRunningId = full_run_id
        self.cmd_listener = listener
        # The queue of commands received from the topic
        self.cmd_queue = Queue()
        self.kafka_log_listener = KafkaLogListener(self.cmd_queue, self.fullRunningId)
        self.cmd_consumer = IasLogConsumer(
            listener=self.kafka_log_listener,
            kafkabrokers=kbrokers,
            topic=IasKafkaHelper.topics['cmd'],
            clientid=kclient_id,
            groupid=kgroup_id)
        
        # Kafka producer of replies
        conf = { 'bootstrap.servers': kbrokers, 'client.id': self.fullRunningId}
        if replyProducer is None:
            self.reply_producer = Producer(conf)
        else:
            self.reply_producer = replyProducer
        
        # Signal the thread to terminate
        self.terminate = False

    def _publishReply(self, reply: IasReply) -> None:
        """
        Publish the reply in the kafk topic
        """
        str = reply.toJSonString()
        self.reply_producer.produce( IasKafkaHelper.topics['reply'],
            value=str,
            key=reply.id)
        self.reply_producer.flush()
        
    def run(self):
        """
        The method executed by the tread that sends commands to the lsitener
        and pushes replies in the topic
        """
        while not self.terminate:
            self.logger.debug("Thread to get comamnds started")
            try:
                (recv_tstamp_str, cmd) = self.cmd_queue.get(block=True, timeout=0.5)
            except Empty: # Timeout
                continue
            try:
                cmd_exit_status :IasCmdExitStatus = self.cmd_listener.cmdReceived(cmd)
            except Exception as e:
                # set the reply to error
                cmd_exit_status = IasCmdExitStatus.ERROR
            finally:
                # Publish reply
                reply = IasReply(
                    self.fullRunningId, 
                    cmd.senderFullRunningId, 
                    cmd.command, 
                    cmd.id, 
                    cmd_exit_status, 
                    recv_tstamp_str, 
                    Iso8601TStamp.now(), 
                    properties=None)
                self._publishReply(reply)
                self.cmd_queue.task_done()
        self.logger.debug("Thread to get commands terminated")
        
    def start(self):
        """
        Start getting commands
        """
        self.logger.debug("Starting")
        self.daemon = True
        self.name = __name__+"-thread"
        super().start()
        self.cmd_consumer.start()
        # Wait until the consumer is subscribed
        timeout = 60 # seconds
        iteration = 0
        while not self.cmd_consumer.isSubscribed() and iteration<2*timeout:
            time.sleep(0.50)
            iteration = iteration+1
        if not self.cmd_consumer.isSubscribed():
            raise RuntimeError("Failed to subscribe to kafka topic")

        self.logger.debug("Started")

    def close(self):
        """
        Stop getting commands
        """
        self.terminate = True
        self.cmd_consumer.close()
        self.logger.debug("Closed")

