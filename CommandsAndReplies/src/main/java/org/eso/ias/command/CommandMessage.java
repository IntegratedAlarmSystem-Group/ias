package org.eso.ias.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eso.ias.utils.ISO8601Helper;

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

    /** The address to send a message to all the IAS tools */
    public static final String BROADCAST_ADDRESS = "*";

    /** The full running ID of the sender of the command */
    private String senderFullRunningId;

    /**
     * The identifier of the receiver  (destination) of the command
     * or the {@link #BROADCAST_ADDRESS} if the command is for all the IAS processes
     */
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> params;

    /** The timestamp when the command has been published (in ISO 8601 format) */
    private String timestamp;

    /** The timestamp in milliseconds that corresponds to {@link #timestamp} */
    @JsonIgnore
    private long timestampMillis;

    /** Additional properties, if any  */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String,String> properties;

    /** Empty constructor */
    public CommandMessage() {}

    /**
     * Constructor
     *
     * @param senderFullRunningId The full running ID of the sender of the command
     * @param destId The identifier of the receiver
     * @param command The command to execute
     * @param id The unique identifier (in the context of the sender) of the command
     * @param params The parameters of the command, if any
     * @param timestamp he timestamp when the command has been published
     * @param properties Additional properties, if any
     */
    public CommandMessage(
            String senderFullRunningId,
            String destId,
            CommandType command,
            long id,
            List<String> params,
            long timestamp,
            Map<String, String> properties) {

        if (senderFullRunningId==null || senderFullRunningId.isEmpty()) {
            throw new IllegalArgumentException(("The sender full running Id can't be null nor empty"));
        }
        this.senderFullRunningId = senderFullRunningId;
        if (destId==null || destId.isEmpty()) {
            throw new IllegalArgumentException(("The ID to the destination can't be null nor empty"));
        }
        this.destId = destId;
        Objects.requireNonNull(command, "The command can't be null");
        this.command = command;
        this.id = id;
        this.params = params;
        setTimestampMillis(timestamp);
        this.properties = properties;
    }

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

    public String getTimestamp() {
        return timestamp;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public void setTimestampMillis(long timestamp) {
        String tStamp = ISO8601Helper.getTimestamp(timestamp);
        this.timestampMillis = timestamp;
        this.timestamp = tStamp;
    }

    /** Set the timestamp to the passed ISO 8601 timestamp */
    public void setTimestamp(String isoTStamp) {
        long tStamp = ISO8601Helper.timestampToMillis(isoTStamp);
        this.timestampMillis = tStamp;
        this.timestamp = isoTStamp;
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
                timestamp.equals(that.timestamp) &&
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
                ", timestamp=" + timestamp + " ("+timestampMillis+")" +
                ", properties=" + properties +
                '}';
    }
}
