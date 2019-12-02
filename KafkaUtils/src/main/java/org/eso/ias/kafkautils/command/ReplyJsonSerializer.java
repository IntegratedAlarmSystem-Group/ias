package org.eso.ias.kafkautils.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public class ReplyJsonSerializer implements ReplyStringSerializer {

    /** The jackson 2 mapper */
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public String iasReplyToString(ReplyMessage reply) throws StringSerializerException {
        Objects.requireNonNull(reply, "Cannot serialize an empty reply");
        try {
            return jsonMapper.writeValueAsString(reply);
        } catch (JsonProcessingException e) {
            throw new StringSerializerException("Error serializing reply"+reply,e);
        }
    }

    @Override
    public ReplyMessage valueOf(String str) throws StringSerializerException {
        if (str==null || str.isEmpty()) {
            throw  new IllegalArgumentException("Cannot de-serialize an empty reply");
        }
        try {
            return jsonMapper.readValue(str, ReplyMessage.class);
        } catch (Exception e) {
            throw new StringSerializerException("Error de-serializing a JSON reply string: "+str,e);
        }
    }
}
