import uuid
from confluent_kafka import Producer

import logging

from IasCmdReply.IasCommandSender import IasCommandSender
from IasCmdReply.IasCommandType import IasCommandType
from IasCmdReply.IasCmdExitStatus import IasCmdExitStatus

class AlarmAck:
    """
    AlarmAck support Acknowledging of alarms by sending commands to the Supervisor.

    AlarmAck sends commands to the Supervisor using IasCommandSender that registers a consumer 
    to get the replies.

    To ACK alkarms, AlarmAck must be started (start() method) and closed (close() method) when done.
    A closed AlarmAck cannot be used anymore.

    At the time of writing, it is foreseeing to use objects of this class from a command line tool
    and from the Alarm GUI.
    """
    def __init__(self, 
                 full_running_id: str,
                 command_sender: IasCommandSender) -> None:
        """
        Constructor
        :param full_running_id: The full running ID of the tool acknowledging the alarms
        :param command_sender: The IasCommandSender to use to send commands
        :type command_sender: IasCommandSender
        """
        if not full_running_id:
            raise ValueError("Invalid null/empty full running ID")
        self.full_running_id = full_running_id

        if not command_sender:
            raise ValueError("Invalid null command sender")
        if not isinstance(command_sender, IasCommandSender):
            raise ValueError("Invalid command sender: expected an instance of IasCommandSender")

        self.logger = logging.getLogger(__name__)

        self.command_sender = command_sender
        
    def start(self) -> None:
        """
        Start the AlarmAck object (starts the internal command sender)
        """
        self.command_sender.set_up()

    def close(self) -> None:
        """
        Close the AlarmAck object (closes the internal command sender)
        """
        self.command_sender.close()

    def ack(self, 
            alarm_id: str, 
            supervisor_id: str,
            comment: str,
            timeout: float=0.0) -> bool:
        """
        Acknowledge the alarm with the given alarm ID.

        :param alarm_id: The full running ID of the alarm to acknowledge
        :type alarm_id: str
        :param supervisor_id: The ID of the supervisor to send the acknowledgment command to
        :type supervisor_id: str
        :param comment: A comment to include with the acknowledgment
        :type comment: str
        :param timeout: The timeout to wait for the reply if 0, returns immediately 
        :type timeout float
        :rtype: bool
        """
        if not supervisor_id:
            raise ValueError("Invalid null/empty supervisor ID")
        if not alarm_id:
            raise ValueError("Invalid null/empty alarm ID")
        
        self.logger.debug(f"Acknowledging alarm with ID {alarm_id} and comment'{comment}'")

        if timeout>0:
            reply = self.command_sender.send_sync(
                dest_id=supervisor_id,
                command=IasCommandType.ACK,
                params=[alarm_id, comment],
                timeout=timeout)
            
            if reply is None:
                self.logger.error(f"No reply received from supervisor {supervisor_id} for ACK command for alarm {alarm_id}")
                return False
            if reply.exitStatus != IasCmdExitStatus.OK:
                self.logger.error(f"Failed to acknowledge alarm {alarm_id} using supervisor {supervisor_id}. Exit status: {reply.exitStatus}")
                return False
            else:
                return True
            
        else:
            self.command_sender.send_async(
                dest_id=supervisor_id,
                command=IasCommandType.ACK,
                params=[alarm_id, comment])
            return True
