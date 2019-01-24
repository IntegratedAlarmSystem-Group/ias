package org.eso.ias.heartbeat;

/**
 * The type of the producer of a Heartbeat
 *
 * Such type includes plugins, converters, Supervisor, clients
 */
public enum HeartbeatProducerType {
    /**
     * Plugin
     */
    PLUGIN,
    /**
     * Converter
     */
    CONVERTER,
    /**
     * Supervisor
     */
    SUPERVISOR,

    /** Generic client like an engineering client.
     * Note necessarily the core monitors this kind of clients
     */
    CLIENT,
    /**
     * A consumer of IASIOs
     */
    SINK
}
