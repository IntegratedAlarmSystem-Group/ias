from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IASLogging.logConf import Log

from IasCmdReply.IasCmdExitStatus import IasCmdExitStatus
from IasCmdReply.IasCommand import IasCommand
from IasCmdReply.IasCommandType import IasCommandType
from IasCmdReply.IasCommandSender import IasCommandSender
from IasCmdReply.IasCmdManagerKafka import IasCmdManagerKafka, IasCommandListener

class TestCmdListener(IasCommandListener):
    def __init__(self) -> None:
        super().__init__()
        print("IasCommandListener built")

    def cmdReceived(self, cmd: IasCommand) -> IasCmdExitStatus:
        print(" Command received:", cmd.toJSonString())
        if cmd.params == [ "PAR1", "PAR2"]:
            return IasCmdExitStatus.OK
        else:
            print("Got params", cmd.params)
            return IasCmdExitStatus.ERROR

class TestCmdManager():
    """
    Test the IasCommandSender and the IasCommandManager together:
        - the command sender sends messages that are read buy the command manager
        - the command manager sends the comand to the listener for processing
        - the command manager sends the reply of the execution of the command to the command sender
        - the command sender gets the reply
    """
    
    def test_send_async_reply(self):
        print("Prepare the command")
        cmd_mgr_id = "CmdManagerId"
        cmd_mgr = IasCmdManagerKafka(cmd_mgr_id, TestCmdListener())
        cmd_mgr.start()

        # Send a command and wait for reply
        dest = cmd_mgr_id
        cmd = IasCommandType.SET_LOG_LEVEL
        params = [ "PAR1", "PAR2"]
        props = { "p1":1, "p2":122}
        
        sender_frId = "FullRuningIdSender"
        cmd_sender = IasCommandSender(sender_frId, "sender_id_test", IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS)
        cmd_sender.set_up()
        reply = cmd_sender.send_sync(dest, cmd, params, props,timeout=30)
        assert reply is not None
        assert reply.id == '1'
        assert reply.command == cmd
        assert reply.exitStatus == IasCmdExitStatus.OK

        # Send a command but not interestyed in reply
        cmd = IasCommandType.PING
        reply = cmd_sender.send_async(dest, cmd, params, props)

        # Send another command and wait to check if we got the right reply
        cmd = IasCommandType.TF_CHANGED
        reply = cmd_sender.send_sync(dest, cmd, params, props,timeout=30)
        assert reply is not None
        assert reply.id == '3'
        assert reply.command == cmd
        assert reply.exitStatus == IasCmdExitStatus.OK

        # Send another command with no params
        # To check if params are correctly received, the listener of command sets an exit code
        cmd = IasCommandType.TF_CHANGED
        reply = cmd_sender.send_sync(dest, cmd, [], props,timeout=30)
        assert reply is not None
        assert reply.id == '4'
        assert reply.command == cmd
        assert reply.exitStatus == IasCmdExitStatus.ERROR
