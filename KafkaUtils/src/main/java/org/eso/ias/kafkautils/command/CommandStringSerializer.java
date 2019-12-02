package org.eso.ias.kafkautils.command;

/**
 * Interface to serialize/deserializer the {@link CommandMessage} to/from strings
 */
public interface CommandStringSerializer {

    /**
     * Convert the passed {@link CommandMessage} to a String
     *
     * @param cmd The command to serialize
     * @return A string representation of the passed command
     * @throws StringSerializerException In case of error creating the string from the passed command
     */
    public String iasCmdToString(CommandMessage cmd) throws StringSerializerException;

    /**
     * Return the command by parsing the passed string
     *
     * @param str The string describing the {@link CommandMessage}
     * @return the command obtained unmasrhalling the passed string
     * @throws StringSerializerException In case of error converting the string
     */
    public CommandMessage valueOf(String str)throws  StringSerializerException;
}
