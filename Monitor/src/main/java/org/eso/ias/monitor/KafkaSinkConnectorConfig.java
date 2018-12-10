package org.eso.ias.monitor;

import javax.persistence.Basic;
import java.util.Objects;

/**
 * The configuration of kafka sink conenctor to monitor
 *
 * These kind of connectors do not pubish HBs and must be
 * monitored with the REST API provided by kafka
 */
public class KafkaSinkConnectorConfig {

    /**
     * The host name to contact with the REST API
     */
    @Basic(optional=false)
    private String hostName;

    /**
     * The port to query the REST API
     */
    @Basic(optional=false)
    private int port;

    /**
     * The ID of the kafka sink connector
     */
    @Basic(optional=false)
    private String id;

    /**
     * Empty constructor
     */
    public KafkaSinkConnectorConfig() {}

    /**
     * Construcotr
     * @param hostName The host name to contact with the REST API
     * @param port The port to query the REST API
     * @param id The ID of the kafka sink connector
     */
    public KafkaSinkConnectorConfig(String hostName, int port, String id) {
        if (hostName==null || hostName.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty/null hostname");
        }
        if (port<=0) {
            throw new IllegalArgumentException("Invalid port "+port);
        }
        if (id==null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty/null id of the connector");
        }
        this.hostName=hostName;
        this.port=port;
        this.id=id;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaSinkConnectorConfig that = (KafkaSinkConnectorConfig) o;
        return port == that.port &&
                hostName.equals(that.hostName) &&
                id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostName, port, id);
    }

    @Override
    public String toString() {
        return "KafkaSinkConnectorConfig{" +
                "hostName='" + hostName + '\'' +
                ", port=" + port +
                ", id='" + id + '\'' +
                '}';
    }
}
