package org.eso.ias.command.test;

import org.eso.ias.command.CommandSender;
import org.eso.ias.command.CommandType;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.IdentifierType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.Option;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestCommandSender {

    /**
     * The Identifier of this tool
     */
    public static final Identifier id = new Identifier("senderID", IdentifierType.CLIENT, Option.empty());

    /**
     * The {@link CommandSender} to test
     */
    public static CommandSender cmdSender;

    private static final String destId = "CmdExeId";

    /**
     * The Identifier of the command executor
     */
    public static final Identifier executorId = new Identifier(destId, IdentifierType.CLIENT, Option.empty());

    @BeforeAll
    public static void setUpAll() throws Exception {
        cmdSender = new CommandSender(id, KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS);
        cmdSender.setUp();

    }

    /**
     * @see AutoCloseable
     */
    @AfterEach
    public void close() throws Exception {
    }

    @AfterAll
    public static void tearDownAll() throws Exception {
        cmdSender.close();
    }


    /**
     * Send a synch command to a non existent destination and wait for the reply that must never arrive
     *
     * @throws Exception
     */
    @Test
    public void testMissingReply() throws Exception {
        String dest = "DoesNotExist";

        boolean ret =cmdSender.sendSync(dest, CommandType.PING,null,null,3, TimeUnit.SECONDS);
        assertFalse(ret);
    }
}
