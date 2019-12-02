package org.eso.ias.command.test;

import org.eso.ias.command.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test serialization and deserialization of commands and replies
 */
public class TestCmdReplySerialization {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(TestCmdReplySerialization.class);

    @Test
    public void testCmdSerialization() throws Exception {
        CommandJsonSerializer serializer = new CommandJsonSerializer();

        CommandMessage cmd = new CommandMessage();
        cmd.setCommand(CommandType.PING);
        cmd.setSenderFullRunningId("SenderFullRunningId");
        cmd.setId(1L);
        cmd.setDestId("Recipient");
        List<String> param = new Vector<>();
        param.add("FirsParam");
        param.add("SecondParam");
        param.add("ThirdParam");
        cmd.setParams(param);
        long now = System.currentTimeMillis();
        cmd.setTimestamp(now);

        Map<String,String> props = new HashMap<>();
        props.put("Key1","Value1");
        props.put("Key2","Value2");
        cmd.setProperties(props);

        String jsonCmd = serializer.iasCmdToString(cmd);
        logger.info("JSON string of command {} is [{}]",cmd.toString(),jsonCmd);

        CommandMessage cmdFromJson = serializer.valueOf(jsonCmd);
        logger.info("Command from JSON [{}] is {}",jsonCmd,cmdFromJson.toString());
        assertEquals(cmd,cmdFromJson,"Commands differ");

    }

    @Test
    public void testReplySerialization() throws Exception {
        ReplyJsonSerializer serializer = new ReplyJsonSerializer();

        ReplyMessage reply = new ReplyMessage();
        reply.setSenderFullRunningId("SenderFullRunningId");
        reply.setDestFullRunningId("RecipientFullRunningId");
        reply.setId(1024L);
        reply.setCommand(CommandType.SHUTDOWN);
        reply.setExitStatus(CommandExitStatus.REJECTED);

        long now = System.currentTimeMillis();
        reply.setReceptionTStamp(now-100);
        reply.setProcessedTStamp(now);

        Map<String,String> props = new HashMap<>();
        props.put("Key1","Value1");
        props.put("Key2","Value2");
        props.put("Key3","Value3");
        reply.setProperties(props);

        String jsonCmd = serializer.iasReplyToString(reply);
        logger.info("JSON string of reply {} is [{}]",reply.toString(),jsonCmd);

        ReplyMessage replyFromJson = serializer.valueOf(jsonCmd);
        logger.info("Reply from JSON [{}] is {}",jsonCmd,replyFromJson.toString());
        assertEquals(reply,replyFromJson,"Repliess differ");

    }
}
