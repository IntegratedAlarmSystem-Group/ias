package org.eso.ias.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eso.ias.utils.ISO8601Helper;

import java.util.Map;
import java.util.Objects;

/**
 * The java POJO to send replies over the kafka topic.
 *
 * This POJO is serialized into a JSON string to be published in the
 * kafka topic
 *
 * @author acaproni
 */
public class ReplyMessage {

    /** The full running ID of the sender of the reply */
    private String senderFullRunningId;

    /**
     * The full running ID of the receiver of the command.
     *
     * This full running ID has been received with the command
     */
    private String destFullRunningId;

    /**
     * The unique identifier (in the context of the sender) of the command.
     *
     * It has been received together with the command
     */
    private long id;

    /**
     * The command just executed
     *
     * It has been received together with the command
     */
    private CommandType command;

    /** The exit status of the command */
    private CommandExitStatus exitStatus;

    /** The point in time when the command has been received from the kafka topic (ISO 8601)*/
    private String receptionTStamp;

    /** The point in time when the command has been received from the kafka topic (milliseconds) */
    @JsonIgnore
    private long receptionTStampMillis;

    /** The point in time when the execution of the command terminated (ISO 8601) */
    private String processedTStamp;

    /** The point in time when the execution of the command terminated (milliseconds) */
    @JsonIgnore
    private long processedTStampMillis;

    /** Additional properties, if any  */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String,String> properties;

    /** Empty constructor */
    public ReplyMessage() {}

    /**
     * Constructor
     *
     * @param senderFullRunningId The full running ID of the sender of the reply
     * @param destFullRunningId The full running ID of the receiver of the command
     * @param id The unique identifier (in the context of the sender) of the command
     * @param command The command just executed
     * @param exitStatus  The exit status of the command
     * @param receptionTStamp The point in time when the command has been received from the kafka topic
     * @param processedTStamp The point in time when the execution of the command terminated
     * @param properties Additional properties, if any
     */
    public ReplyMessage(
            String senderFullRunningId,
            String destFullRunningId,
            long id,
            CommandType command,
            CommandExitStatus exitStatus,
            long receptionTStamp,
            long processedTStamp,
            Map<String, String> properties) {
        assert receptionTStamp<=processedTStamp : "Unordered timestamps";
        this.senderFullRunningId = senderFullRunningId;
        this.destFullRunningId = destFullRunningId;
        this.id = id;
        this.command = command;
        this.exitStatus = exitStatus;
        setReceptionTStampMillis(receptionTStamp);
        setProcessedTStampMillis(processedTStamp);
        this.properties = properties;
    }

    public String getSenderFullRunningId() {
        return senderFullRunningId;
    }

    public void setSenderFullRunningId(String senderFullRunningId) {
        this.senderFullRunningId = senderFullRunningId;
    }

    public String getDestFullRunningId() {
        return destFullRunningId;
    }

    public void setDestFullRunningId(String destFullRunningId) {
        this.destFullRunningId = destFullRunningId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public CommandType getCommand() {
        return command;
    }

    public void setCommand(CommandType command) {
        this.command = command;
    }

    public CommandExitStatus getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(CommandExitStatus exitStatus) {
        this.exitStatus = exitStatus;
    }

    public String getReceptionTStamp() {
        return receptionTStamp;
    }

    public long getReceptionTStampMillis() {
        return receptionTStampMillis;
    }

    public void setReceptionTStamp(String receptionTStamp) {
        this.receptionTStamp = receptionTStamp;
        this.receptionTStampMillis=ISO8601Helper.timestampToMillis(receptionTStamp);
    }

    public void setReceptionTStampMillis(long receptionTStamp) {
        this.receptionTStampMillis = receptionTStamp;
        this.receptionTStamp=ISO8601Helper.getTimestamp(receptionTStamp);
    }

    public String getProcessedTStamp() {

        return processedTStamp;
    }

    public long getProcessedTStampMillis() {
        return processedTStampMillis;
    }

    public void setProcessedTStamp(String processedTStamp) {
        this.processedTStamp=processedTStamp;
        this.processedTStampMillis=ISO8601Helper.timestampToMillis(processedTStamp);
    }

    public void setProcessedTStampMillis(long processedTStamp) {

        this.processedTStampMillis = processedTStamp;
        this.processedTStamp = ISO8601Helper.getTimestamp(processedTStamp);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "ReplyMessage{" +
                "senderFullRunningId='" + senderFullRunningId + '\'' +
                ", destFullRunningId='" + destFullRunningId + '\'' +
                ", id=" + id +
                ", command=" + command +
                ", exitStatus=" + exitStatus +
                ", receptionTStamp=" + receptionTStamp + " ("+receptionTStampMillis+ ")" +
                ", processedTStamp=" + processedTStamp + " ("+processedTStampMillis+ ")" +
                ", properties=" + properties +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplyMessage that = (ReplyMessage) o;
        return id == that.id &&
                receptionTStamp.equals(that.receptionTStamp) &&
                processedTStamp.equals(that.processedTStamp) &&
                senderFullRunningId.equals(that.senderFullRunningId) &&
                destFullRunningId.equals(that.destFullRunningId) &&
                command == that.command &&
                exitStatus == that.exitStatus &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderFullRunningId, destFullRunningId, id, command, exitStatus, receptionTStamp, processedTStamp, properties);
    }
}
