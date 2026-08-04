from queue import Queue
import time
import uuid

from IasLogging.log import Log
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp

"""
Test the sending of commands by listening at the cmd kafka topic
"""
from IasCmdReply.IasCommandSender import IasCommandSender
from IasCmdReply.IasCommand import IasCommand
from IasCmdReply.IasCommandType import IasCommandType
from IasKafkaUtils.IasKafkaConsumer import IasLogConsumer, IasLogListener
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

class CmdListener(IasLogListener):
    def __init__(self, logs: Queue):
        self.logs = logs

    def iasLogReceived(self, log: str) -> None:
        print("Cmd ",log)
        cmd = IasCommand.fromJSon(log)
        self.logs.put(cmd)

class TestCommandSender():
    '''
    Test sending of comamnds from IasCommandSender.

    This test does not test the sending of replies that is tested in testCommandManeger

    Reply is not tested in this context
    '''


    @classmethod
    def setup_class(cls):
        Log.init_logging(__file__)
        cls.received_cmds = Queue()
        cls.listener = CmdListener(cls.received_cmds)
        cls.cmd_consumer = IasLogConsumer(
            listener=cls.listener,
            kafkabrokers=IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
            topic=IasKafkaHelper.topics['cmd'],
            clientid="TestCommandSender.cli"+str(uuid.uuid4()),
            groupid="TestCommandSender.grp"+str(uuid.uuid4()))
        cls.cmd_consumer.start(waitAssigmentTimeout=30)
        assert cls.cmd_consumer.isGettingLogs()
        print("Consumer subscribed")
    
    def test_send_async_command(self):
        print("Prepare the command")
        dest = "CmdDest"
        cmd = IasCommandType.SET_LOG_LEVEL
        params = [ "PAR1", "PAR2"]
        props = { "p1":1, "p2":122}
        
        sender_frId = "FullRuningIdeSender"
        cmd_sender = IasCommandSender(sender_frId, "sender_id_test", IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
        print("Iitializing the IasCommandSender")
        cmd_sender.set_up()
        print("IasCommandSender initialized")
        print("Sending command",cmd)
        cmd_sender.send_async(dest, cmd, params, props)
        print(" Command sent")

        # Wait for the cmd: raise exception if no cmd is received in time
        print(" Waiting for the replay")
        recv_cmd = TestCommandSender.received_cmds.get(block=True, timeout=60)
        print("Reply received")
        assert recv_cmd.command == cmd
        assert recv_cmd.destId == dest
        assert recv_cmd.senderFullRunningId == sender_frId
        assert int(recv_cmd.id) == 1

