package org.eso.ias.kafkautils.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public class CommandJsonSerializer implements CommandStringSerializer {

    /** The jackson 2 mapper */
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public String iasCmdToString(CommandMessage cmd) throws StringSerializerException {
        Objects.requireNonNull(cmd, "Cannot serialize an empty command");
        try {
            return jsonMapper.writeValueAsString(cmd);
        } catch (JsonProcessingException e) {
            throw new StringSerializerException("Error serializing command "+cmd,e);
        }
    }

    @Override
    public CommandMessage valueOf(String str) throws StringSerializerException {
        if (str==null || str.isEmpty()) {
            throw  new IllegalArgumentException("Cannot de-serialize an empty command");
        }
        try {
            return jsonMapper.readValue(str, CommandMessage.class);
        } catch (Exception e) {
            throw new StringSerializerException("Error de-serializing a JSON command string: "+str,e);
        }
    }
}
