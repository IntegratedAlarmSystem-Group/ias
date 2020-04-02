package org.eso.ias.plugin.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eso.ias.cdb.pojos.PluginConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * The configuration of the plugin read from a JSON file differs from the provided in the CDB.
 * This class provides the missing fields.
 */
public class PluginFileConfig extends PluginConfigDao {

    /**
     * The logger
     */
    private final Logger logger = LoggerFactory.getLogger(PluginFileConfig.class);

    /**
     * The name of the server to send monitor point values
     * and alarms to
     */
    private String sinkServer;

    /**
     * The default time interval to automatically resend monitor point
     * if their values did not change
     */
    public static final int autoSendTimeIntervalDefault = 5;

    /**
     * The time interval (seconds) to automatically resend the monitor
     * points even if their values did not change.
     */
    private int autoSendTimeInterval = autoSendTimeIntervalDefault;

    /**
     * The default value of the frequency of the heartbeat
     */
    public static final int hbFrequencyDefault = 5;

    /**
     * The frequency of the heartbeat in seconds
     */
    private int hbFrequency = hbFrequencyDefault;

    /**
     * The port of the server to send monitor point values
     * and alarms to
     */
    private int sinkPort;

    /**
     * @return the sinkServer
     */
    public String getSinkServer() {
        return sinkServer;
    }

    /**
     * @param sinkServer the sinkServer to set
     */
    public void setSinkServer(String sinkServer) {
        this.sinkServer = sinkServer;
    }

    /**
     * @return the sinkPort
     */
    public int getSinkPort() {
        return sinkPort;
    }

    /**
     * @param sinkPort the sinkPort to set
     */
    public void setSinkPort(int sinkPort) {
        this.sinkPort = sinkPort;
    }

    public int getAutoSendTimeInterval() {
        return autoSendTimeInterval;
    }

    public void setAutoSendTimeInterval(int autoSendTimeINterval) {
        this.autoSendTimeInterval = autoSendTimeINterval;
    }

    public int getHbFrequency() {
        return hbFrequency;
    }

    public void setHbFrequency(int hbFrequency) {
        this.hbFrequency = hbFrequency;
    }

    /**
     * Check the correctness of the values contained in this objects:
     * <UL>
     * 	<LI>Non empty ID
     * 	<LI>Non empty sink server name
     * 	<LI>Valid port
     * 	<LI>Non empty list of values
     *  <LI>No duplicated ID between the values
     *  <LI>Each value is valid
     * </ul>
     *
     * @return <code>true</code> if the data contained in this object
     * 			are correct
     */
    @JsonIgnore
    public boolean isValid() {
        if (sinkServer==null || sinkServer.isEmpty()) {
            logger.error("Invalid null or empty sink server name");
            return false;
        }
        if (sinkPort<=0) {
            logger.error("Invalid sink server port number {}",sinkPort);
            return false;
        }

        if (autoSendTimeInterval<=0) {
            logger.error("Auto send time interval must be greater then 0");
            return false;
        }

        if (hbFrequency<=0) {
            logger.error("The frequency must be greater then 0");
            return false;
        }
        return super.isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PluginFileConfig that = (PluginFileConfig) o;
        return autoSendTimeInterval == that.autoSendTimeInterval &&
                hbFrequency == that.hbFrequency &&
                sinkPort == that.sinkPort &&
                sinkServer.equals(that.sinkServer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sinkServer, autoSendTimeInterval, hbFrequency, sinkPort);
    }
}
