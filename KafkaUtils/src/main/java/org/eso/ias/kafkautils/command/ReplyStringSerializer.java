package org.eso.ias.kafkautils.command;

/**
 * Interface to serialize/deserializer the {@link ReplyMessage} to/from strings
 */
public interface ReplyStringSerializer {
    /**
     * Convert the passed {@link ReplyMessage} to a String
     *
     * @param reply The command to serialize
     * @return A string representation of the passed command
     * @throws StringSerializerException In case of error creating the string from the passed reply
     */
    public String iasReplyToString(ReplyMessage reply) throws StringSerializerException;

    /**
     * Return the command by parsing the passed string
     *
     * @param str The string describing the {@link ReplyMessage}
     * @return the reply obtained unmasrhalling the passed string
     * @throws StringSerializerException In case of error converting the string
     */
    public ReplyMessage valueOf(String str)throws  StringSerializerException;
}
