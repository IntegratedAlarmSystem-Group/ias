package org.eso.ias.monitor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Basic;
import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Java pojo with the configuration of the IASMonitor tool
 */
public class IasMonitorConfig {

    /**
     * The threshold to invalidate the HBs
     */
    @Basic(optional=false)
    public Long threshold;

    /**
     * The IDs of the supervisors that must NOT be monitored
     *
     * The Ids of the supervisors are read from the CDB and the monitor
     * checks the HBs published by each of them unless their ID is in
     * this set
     */
    @Basic(optional=true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<String> excludedSupervisorIds = new HashSet<>();

    /**
     * The IDs of the plugins to monitor
     */
    @Basic(optional=true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<String> pluginIds = new HashSet<>();

    /**
     * Empty constructor
     */
    public IasMonitorConfig() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IasMonitorConfig that = (IasMonitorConfig) o;
        return threshold.equals(that.threshold) &&
                Objects.equals(excludedSupervisorIds, that.excludedSupervisorIds) &&
                Objects.equals(pluginIds, that.pluginIds) &&
                Objects.equals(converterIds, that.converterIds) &&
                Objects.equals(clientIds, that.clientIds) &&
                Objects.equals(sinkIds, that.sinkIds) &&
                Objects.equals(kafkaSinkConnectors, that.kafkaSinkConnectors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, excludedSupervisorIds, pluginIds, converterIds, clientIds, sinkIds, kafkaSinkConnectors);
    }

    /**
     * The IDs of the converters to monitor
     */
    @Basic(optional=true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<String> converterIds = new HashSet<>();

    /**
     * The IDs of the clients to monitor
     */
    @Basic(optional=true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<String> clientIds = new HashSet<>();

    /**
     * The IDs of the sink client to monitor
     *
     * These are IAS clients not kafka sink connectors
     */
    @Basic(optional=true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<String> sinkIds = new HashSet<>();

    /**
     * The KAFKA Sink connectors to monitor
     *
     * These are monitored though the REST API
     */
    public Set<KafkaSinkConnectorConfig> kafkaSinkConnectors = new HashSet<>();

    public Long getThreshold() {
        return threshold;
    }

    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }

    public Set<String> getExcludedSupervisorIds() {
        return excludedSupervisorIds;
    }

    public void setExcludedSupervisorIds(Set<String> excludedSupervisorIds) {
        this.excludedSupervisorIds = excludedSupervisorIds;
    }

    public Set<String> getPluginIds() {
        return pluginIds;
    }

    public void setPluginIds(Set<String> pluginIds) {
        this.pluginIds = pluginIds;
    }

    public Set<String> getConverterIds() {
        return converterIds;
    }

    public void setConverterIds(Set<String> converterIds) {
        this.converterIds = converterIds;
    }

    public Set<String> getClientIds() {
        return clientIds;
    }

    public void setClientIds(Set<String> clientIds) {
        this.clientIds = clientIds;
    }

    public Set<String> getSinkIds() {
        return sinkIds;
    }

    public void setSinkIds(Set<String> sinkIds) {
        this.sinkIds = sinkIds;
    }

    public Set<KafkaSinkConnectorConfig> getKafkaSinkConnectors() {
        return kafkaSinkConnectors;
    }

    public void setKafkaSinkConnectors(Set<KafkaSinkConnectorConfig> kafkaSinkConnectors) {
        this.kafkaSinkConnectors = kafkaSinkConnectors;
    }

    /**
     * Serialize this configuration into a JSON string
     */
    public String toJsonString() throws Exception {
        return IasMonitorConfig.configToJsonString(this);
    }

    /**
     * Build the {@link IasMonitorConfig} from the passed JSON string
     *
     * @param jsonString The JSON string with the configuration
     * @return The IasMonitorConfig object
     * @throws Exception in case of error parsing the JSON string
     */
    public static IasMonitorConfig valueOf(String jsonString) throws Exception {
        if (jsonString==null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid configuration string");
        }
        // The jackson 2 mapper
        ObjectMapper jsonMapper = new ObjectMapper();
        IasMonitorConfig jsonPojo = null;
        try {
            jsonPojo = jsonMapper.readValue(jsonString, IasMonitorConfig.class);
        } catch (Exception e) {
            throw new Exception("Error parsing the configuration string",e);
        }
        return jsonPojo;
    }

    /**
     * Serialize the passed configuration into a JSON string
     *
     * @param config The configuration to serialize
     * @return the JSON string representing the passed object
     * @throws Exception in case of error serializing into a JSON string
     */
    public static String configToJsonString(IasMonitorConfig config) throws  Exception {
        Objects.requireNonNull(config);
        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            return jsonMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new Exception("Error serializing the configuration into a JSON string",e);
        }
    }

    /**
     * Reads the configuration from the passed file
     *
     * @param f The file with the configuration
     * @return The IasMonitorConfig object
     * @throws Exception in case of error parsing the JSON string
     */
    public static IasMonitorConfig fromFile(File f) throws  Exception {
        Objects.requireNonNull(f);
        // The jackson 2 mapper
        ObjectMapper jsonMapper = new ObjectMapper();
        IasMonitorConfig jsonPojo = null;
        try {
            jsonPojo=jsonMapper.readValue(f, IasMonitorConfig.class);
        } catch (Exception e) {
            throw new Exception("Error parsing the configuration from file "+f.getAbsolutePath(),e);
        }
        return jsonPojo;
    }
}
