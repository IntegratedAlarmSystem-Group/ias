from confluent_kafka import Producer
from queue import Queue, Empty
from typing import List, Dict
import time
import traceback

from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IASLogging.logConf import Log
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasKafkaUtils.IasKafkaConsumer import IasLogConsumer, IasLogListener
from IasCmdReply.IasCommandType import IasCommandType
from IasCmdReply.IasCommand import IasCommand
from IasCmdReply.IasReply import IasReply

class IasCommandSender(IasLogListener):
    """
    Objects of this class send commands and optionally wait for the reply through the kafka topics.

    Methods to send commands are synchronized i.e. it is not possible to send a command before the
    previous send terminates. This is in particular true for the request/reply
    
    This forces the sender to serialize requests and replies and can be a limitation if the sender needs
    to send a bounce of commands. There is room for improvement, but at the present it is all the IAS needs.

    """

    def __init__(self, sender_full_running_id: str, sender_id: str, brokers: str, string_producer: Producer|None=None):
        """
        Constructor

        Params:
            senderFullRuningId The full runing id of the sender
            stringProducer The string producer to publish commands
                           (if None builds a new producer)
            senderId The id of the sender
            brokers URL of kafka brokers
        """
        if not sender_full_running_id:
            raise ValueError("Invalid null/empty full running ID of the sender")
        self.logger = Log.getLogger(__name__)
        self.sender_full_running_id = sender_full_running_id
        self.sender_id = sender_id

        # Kafka producer of comands
        conf = { 'bootstrap.servers': brokers, 'client.id': sender_id}
        if string_producer is None:
            self.cmd_producer = Producer(conf)
        else:
            self.cmd_producer = string_producer

        # The consumer of replies
        self.reply_consumer = IasLogConsumer(
            listener=self,
            kafkabrokers=brokers,
            topic=IasKafkaHelper.topics['reply'],
            clientid=sender_id,
            groupid=sender_id)
        
        self.cmd_id = 0
        self.request_reply_in_progress = False
        self.id_to_wait = None
        self.replies_queue = Queue()
        self.closed = False
        self.initialized=False

    def set_up(self):
        if self.closed:
            raise RuntimeError("Cannot initialized a closed object")
        if not self.initialized:
            self.reply_consumer.start()
            # Wait until the consumer is subscribed
            timeout = 60 # seconds
            iteration = 0
            while not self.reply_consumer.isSubscribed() and iteration<2*timeout:
                time.sleep(0.50)
                iteration = iteration+1
            if not self.reply_consumer.isSubscribed():
                raise RuntimeError("Failed to subscribe to reply kafka topic")
            self.initialized = True
        else:
            self.logger.warning("Already initialized")
        

    def close(self):
        if not self.closed:
            self.logger.debug("Closing...")
            self.reply_consumer.close()
            self.closed = True
            self.logger.info("Closed")
        else:
            self.logger.warning("Already closed!")

    def _publish_cmd(
            self,
            id: int,
            dest_id: str, 
            command: IasCommandType, 
            params: List[str]|None,
            properties: Dict[str, str]|None,):
        """
        Publish a command in the kafka command topic

        Params:
            id: the identifier of the command
            destId The id of the destination of the command (cannot be BROADCAST)
            command The command to send
            params The optional parameters of the command
            properties The optional properties of the command
        """
        ias_command = IasCommand(
            dest=dest_id,
            sender=self.sender_full_running_id,
            cmd=command,
            id=id,
            tStamp=Iso8601TStamp.now(),
            params=params,
            props=properties)
        
        # JSON string to publish
        ias_cmd_str = ias_command.toJSonString()

        self.cmd_producer.produce(
            IasKafkaHelper.topics['cmd'],
            value=ias_cmd_str,
            key=str(id))
        self.logger.debug("Cmd with ID %d published in the kafka topic", id)

    def send_sync(
            self,
            dest_id: str, 
            command: IasCommandType, 
            params: List[str]|None=None,
            properties: Dict[str, str]|None=None,
            timeout: float=0) -> IasReply|None:
        """
        Send a command synchronously,

        This method sends the commands and, optionally, waits for the reply.

        Send-reply is not available for broadcast.

        Delegates the publising in the kafka topic to self._publish_cmd.

        Params:
            destId The id of the destination of the command (cannot be BROADCAST)
            command The command to send
            params The optional parameters of the command
            properties The optional properties of the command
            timeout the time interval (>=0) for the timeout getting the reply
                    (if 0 does not wait for the reply) 
        Return:
            the reply received by the destinator of the command or 
            None if the waiting time elapsed before getting the reply
        """

        if self.closed:
            self.logger.error("Cannot send commands from a closed sender: command discarded")
        if not self.initialized:
            self.logger.error("Cannot send commands from an uninitialized sender: command discarded")

        if dest_id == "BROADCAST":
            raise ValueError("BROADCAST cannot be used for send-reply")
        
        if self.request_reply_in_progress:
            raise RuntimeError("Cannt process two commands at the same time")
        self.request_reply_in_progress = True

        try:
            self.logger.debug(f"Sending sync command {command} to {dest_id}")

            if timeout<0:
                raise ValueError(f"Invalid timeout {timeout}: must be >0")

            self.cmd_id += 1
            self.id_to_wait = self.cmd_id
            self._publish_cmd(self.cmd_id, dest_id,command, params, properties)
            self.cmd_producer.flush() # sync

            if timeout>0:
                self.logger.debug(f"Waiting for reply with id {self.id_to_wait} from {dest_id}")
                try:
                    reply = self.replies_queue.get(True, timeout)
                    self.replies_queue.task_done()
                    return reply
                except Empty as to:
                    # Timeout!
                    return None
            else:
                self.logger.debug(f"Will not wait for the reply from {dest_id}")
                # TODO:
                # Check if the reply is accepted anyhow as in this case it must be removed from the queue
                # or better must not be put in the queue

                return None
        finally:
            self.request_reply_in_progress = False

    def send_async(
            self, 
            dest_id: str, 
            command: IasCommandType, 
            params: List[str]|None=None, 
            properties: Dict[str, str]|None=None) -> None:
        """
        Send a command asynchronously (i.e. do not wait for the reply)
        
        Delegates the publising in the kafka topic to self._publish_cmd.

        Params:
            destId The id of the destination of the command (cannot be BROADCAST)
            command The command to send
            params The optional parameters of the command
            properties The optional properties of the command
        """
        if self.closed:
            self.logger.error("Cannot send commands from a closed sender: command discarded")
        if not self.initialized:
            self.logger.error("Cannot send commands from an uninitialized sender: command discarded")

        if self.request_reply_in_progress:
            raise RuntimeError("Cannot process two commands at the same time")
        self.request_reply_in_progress= True
        try:
            self.cmd_id += 1
            self.id_to_wait = None
            self._publish_cmd(self.cmd_id, dest_id,command, params, properties)
        finally:
            self.request_reply_in_progress = False
        
    
    def iasLogReceived(self, log: str) -> None:
        """
        Overrides IasLogListener.iasLogReceived to get replies

        Put the replies in the queue
        """
        if str:
            self.logger.debug("Got a reply %s", log)
            try:
                reply = IasReply.fromJSon(log)
                if not self.id_to_wait:
                    self.logger.debug("Discarded reply as not waiting for replies %s", reply.destFullRunningId)
                    return
                if reply.destFullRunningId==self.sender_full_running_id:
                    if reply.id==str(self.id_to_wait):
                        self.replies_queue.put(reply)
                    else:
                        self.logger.debug("Discarded reply whith id %d while waiting for id %d", reply.id, self.id_to_wait)
                else:
                    self.logger.debug("Discarded reply whose destination is %s", reply.destFullRunningId)
            except Exception as ex:
                self.logger.error(f"Malformed JSON string representing a reply: [{log}]")
                traceback.print_exception(ex)

