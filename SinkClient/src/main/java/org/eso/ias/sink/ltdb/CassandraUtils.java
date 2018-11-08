package org.eso.ias.sink.ltdb;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * An helper to send commands to cassandra
 *
 * @author acaproni
 */
public class CassandraUtils {

    /**
     * The contact points to connect to cassandra DB
     */
    private final String contactPoints;

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CassandraUtils.class);

    /**
     * Cassandra cluster
     */
    private Cluster cluster;

    /**
     * Database session
     */
    private Session session;

    /**
     * A flag set if the conenction with Cassandra
     * has been correctly established
     */
    public volatile boolean initialized=false;

    /**
     * A flag set if the object has been closed
     */
    public volatile boolean closed=false;

    /**
     * Constructor
     *
     * @param contactPoints the contact points to connect to cassandra DB
     */
    public CassandraUtils(String contactPoints) {
        if (contactPoints==null || contactPoints.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty cassndra contact points");
        }
        this.contactPoints =contactPoints;
        CassandraUtils.logger.debug("Object built with contact poins {}",this.contactPoints);
    }

    /**
     * Establish the conenction witht the database and, optionall, use the
     * passed keyspace
     * @param keyspaceOpt The key space
     * @throws Exception in case of error connecting to the cassandra DB
     */
    public void start(Optional<String> keyspaceOpt) throws Exception {
        if (closed) {
            throw new Exception("Cannot initialized a closed conenction");
        }
        if (initialized) {
            CassandraUtils.logger.warn("Already initialized: skipping");
            return;
        }
        try {
            CassandraUtils.logger.debug("Building cluster");
            cluster = Cluster.builder().withClusterName("IAS-LTDB").addContactPoint(contactPoints).build();
            CassandraUtils.logger.info("Cluster built with {} contact point", contactPoints);

            CassandraUtils.logger.debug("Connecting session");
            if (keyspaceOpt.isPresent() && !keyspaceOpt.get().isEmpty()) {
                session = cluster.connect(keyspaceOpt.get());
                CassandraUtils.logger.info("Session instantiated with {} keyspace", keyspaceOpt.get());
            } else {
                session = cluster.connect();
                CassandraUtils.logger.info("Session instantiated with no keyspace");
            }
        } catch (Exception e) {
            CassandraUtils.logger.error("Error initiating cluster and/or session",e);
            CassandraUtils.logger.debug("Closing session and cluster");
            if (session!=null) {
                session.close();
            }
            if (cluster!=null) {
                cluster.close();
            }
            CassandraUtils.logger.info("Session and cluster closed");
        }
        initialized=true;

    }

    /**
     * Close the connection with cassandra
     */
    public void stop() throws Exception {
        if (!initialized) {
            throw new Exception("Cannot close a conection that has not been established");
        }
        if (closed) {
            CassandraUtils.logger.warn("Already closed");
            return;
        }
        if (session!=null) {
            CassandraUtils.logger.debug("Closing session");
            session.close();
            CassandraUtils.logger.info("Session closed");
        }

        if (cluster!=null) {
            CassandraUtils.logger.debug("Closing cluster");
            cluster.close();
            CassandraUtils.logger.info("Cluster closed");
        }
        CassandraUtils.logger.info("Closed");
        closed=true;
    }


    /**
     * Execute the passed statement
     *
     * @param statement The not null nor empty statement to execute
     * @return the result set or null if the connection has been closed
     */
    public ResultSet executeStatement(String statement) {
        if (statement==null || statement.isEmpty()) {
            throw new IllegalArgumentException("Cannot execute a null statement");
        }
        if (session.isClosed()) {
            CassandraUtils.logger.warn("Session is closed: {} will NOT execute the stament in the LTDB");
            return null;
        }
        if (closed) {
            return null;
        } else {
            return session.execute(statement);
        }
    }

}
