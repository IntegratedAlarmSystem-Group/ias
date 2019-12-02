package org.eso.ias.kafkautils.command;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The java POJO representing a command to be published in the command kafka topic
 *
 * This POJO is serialized into a JSON string to be published in the
 * kafka topic
 */
public class CommandMessage {

    /** The full running ID of the sender of the command */
    private String senderFullRunningId;

    /** The identifier of the receiver  (destination) of the command  */
    private String destId;

    /** The command to execute  */
    private CommandType command;

    /**
     * The unique identifier (in the context of the sender) of the command.
     *
     * The id can be used by the sender to check if it has been executed because the same
     * id is sent back in the reply by the destination
     */
    private long id;

    /** The parameters of the command, if any */
    private List<String> params;

    /** The timestamp when the command has been published */
    private long timestamp;

    /** Additional properties, if any  */
    private Map<String,String> properties;

    /** Empty constructor */
    public CommandMessage() {}


    public String getSenderFullRunningId() {
        return senderFullRunningId;
    }

    public void setSenderFullRunningId(String senderFullRunningId) {
        this.senderFullRunningId = senderFullRunningId;
    }

    public String getDestId() {
        return destId;
    }

    public void setDestId(String destId) {
        this.destId = destId;
    }

    public CommandType getCommand() {
        return command;
    }

    public void setCommand(CommandType command) {
        this.command = command;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandMessage that = (CommandMessage) o;
        return id == that.id &&
                timestamp == that.timestamp &&
                senderFullRunningId.equals(that.senderFullRunningId) &&
                destId.equals(that.destId) &&
                command == that.command &&
                Objects.equals(params, that.params) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderFullRunningId, destId, command, id, params, timestamp, properties);
    }

    @Override
    public String toString() {
        return "CommandMessage{" +
                "senderFullRunningId='" + senderFullRunningId + '\'' +
                ", destId='" + destId + '\'' +
                ", command=" + command +
                ", id=" + id +
                ", params=" + params +
                ", timestamp=" + timestamp +
                ", properties=" + properties +
                '}';
    }
}
