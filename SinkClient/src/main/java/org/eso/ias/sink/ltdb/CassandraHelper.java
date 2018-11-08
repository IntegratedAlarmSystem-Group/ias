package org.eso.ias.sink.ltdb;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.utils.ISO8601Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collection;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The helper to interact with cassandra.
 */
public class CassandraHelper {

    /**
     * Time to leave in seconds
     * If <=0 no time to leave will be set
     */
    private long ttl=0;

    /**
     * Signal that the connection ha sbeen closed
     */
    public volatile boolean closed=false;

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CassandraHelper.class);

    /**
     * Cassandra cluster
     */
    private Cluster cluster;

    /**
     * Database session
     */
    private Session session;

    /**
     * The serializer to convert IASValues to/from Strings
     */
    private final IasValueJsonSerializer jsonSerializer = new IasValueJsonSerializer();

    /**
     * Record the number of errors (for statistics)
     */
    private final AtomicLong numOfErrors = new AtomicLong(0);

    /**
     * Record the number of IASValues effectively stored in the LTDB (for statistics)
     */
    private final AtomicLong numValuesStoredInLTDB = new AtomicLong(0);

    /**
     * Connect to cassandra and allocate resources
     *
     * @param contactPoints Cassandra contact points
     * @param keyspace keyspace
     * @param ttl the time to leave in hours (if <=0, no TTL)
     */
    public void start(String contactPoints, String keyspace, long ttl) {
        Objects.requireNonNull(contactPoints);
        Objects.requireNonNull(keyspace);

        this.ttl= TimeUnit.SECONDS.convert(ttl,TimeUnit.HOURS);

        try {
            CassandraHelper.logger.debug("Building cluster");
            cluster = Cluster.builder().withClusterName("IAS-LTDB").addContactPoint(contactPoints).build();
            CassandraHelper.logger.info("Cluster built with {} contact point", contactPoints);

            CassandraHelper.logger.debug("Connecting session");
            session = cluster.connect(keyspace);
            CassandraHelper.logger.info("Session instantiated with {} keyspace", keyspace);
        } catch (Exception e) {
            CassandraHelper.logger.error("Error initiating cluster and/or session",e);
            CassandraHelper.logger.debug("Closing session and cluster");
            if (session!=null) {
                session.close();
            }
            if (cluster!=null) {
                cluster.close();
            }
            CassandraHelper.logger.info("Session and cluster closed");
        }
    }

    /**
     * Close the connection with cassandra
     */
    public void stop() {
        if (session!=null) {
            CassandraHelper.logger.debug("Closing session");
            session.close();
            CassandraHelper.logger.info("Session closed");
        }

        if (cluster!=null) {
            CassandraHelper.logger.debug("Closing cluster");
            cluster.close();
            CassandraHelper.logger.info("Cluster closed");
        }
        CassandraHelper.logger.info("Closed");
    }

    /**
     * Build the timetsamp string for inserting the value in the LTDB
     * with a format like 2018-11-07T08:48
     *
     * @param timestamp the timestamp
     * @return the date for the INSERT
     */
    private String buildTimestampForLTDB(long timestamp) {
        Calendar calendar = new Calendar.Builder().
                setInstant(timestamp).
                setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")))
                .build();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH)+1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);

        return String.format("%d-%02d-%02dT%02d:%02d",year,month,day,hour,min);
    }

    /**
     * Store a IASValue in the LTDB.
     *
     * @param iasValue the JSON string encoding a IASValue
     */
    private void store(IASValue<?> iasValue) {
        Objects.requireNonNull(iasValue);
        if (session.isClosed()) {
            CassandraHelper.logger.warn("Session is closed: {} will NOT be stored in the LTDB",iasValue.id);
            numOfErrors.incrementAndGet();
            return;
        }

        Long prodTime;
        if (iasValue.dasuProductionTStamp.isPresent()) {
            prodTime=iasValue.dasuProductionTStamp.get();
        } else if (iasValue.pluginProductionTStamp.isPresent()) {
            prodTime=iasValue.pluginProductionTStamp.get();
        } else {
            CassandraHelper.logger.error("No DASU prod timestamp neither plugin prod timestamp defined for {}: value will NOT be stored in theLTDB",iasValue.id);
            numOfErrors.incrementAndGet();
            return;
        }

        String prodTStamp = ISO8601Helper.getTimestamp(prodTime);

        String id = iasValue.id;
        String date = buildTimestampForLTDB(prodTime);

        String json;
        try {
            json=jsonSerializer.iasValueToString(iasValue);
        } catch (Exception e) {
            CassandraHelper.logger.error("Error converting IASValue {} in a JSON string: will NOT be stored in the LTDB!",iasValue.id,e);
            numOfErrors.incrementAndGet();
            return;
        }

        storeOnCassandra(date,id,prodTStamp,json);
    }


    /**
     * Insert a row in cassandra
     *
     * @param date The date of the IASValue
     * @param id The id of the IASVAlue
     * @param eventTime the DASU or plugin production time
     * @param json the json string encoding the IASValue
     */
    private void storeOnCassandra(String date, String id, String eventTime, String json) {
        if (!closed) {
            StringBuilder insert = new StringBuilder("INSERT INTO iasio_by_day JSON '{");
            insert.append("\"iasio_id\":\"");
            insert.append(id);
            insert.append("\", \"date\":\"");
            insert.append(date);
            insert.append("\", \"event_time\":\"");
            insert.append(eventTime);
            insert.append("\", \"value\":");
            insert.append(json);
            insert.append("}'");

            if (ttl>0) {
                insert.append(" USING TTL ");
                insert.append(ttl);
            }
            insert.append(';');

            CassandraHelper.logger.debug(insert.toString());

            ResultSet rs = session.execute(insert.toString());
            if (!rs.wasApplied()) {
                CassandraHelper.logger.error("INSERT was not executed: value {} NOT stored in the LTDB",id);
                numOfErrors.incrementAndGet();
            }
            numValuesStoredInLTDB.incrementAndGet();
        }
    }

    /**
     * Store the IASValues encoded by the passed JSON strings
     * by delegating to {@link #store(IASValue)}
     *
     * @param jsonStrings a collection of JSON strings encoding IASValues
     */
    public synchronized void store(Collection<String> jsonStrings) {
        if (jsonStrings!=null && !closed) {
            for (String jsonString: jsonStrings) {
                store(jsonString);
            }
        }
    }

    /**
     * Store the IASValue encoded by the passed JSON string
     * by delegating to {@link #store(IASValue)}
     *
     * @param jsonString the JSON string encoding a IASValue
     */
     public synchronized void store(String jsonString) {
         IASValue<?> value;
         try {
             value= jsonSerializer.valueOf(jsonString);
         } catch (Exception e) {
             CassandraHelper.logger.error("Error converting {} into a IASValue: will NOT be stored in the LTDB!",jsonString,e);
             numOfErrors.incrementAndGet();
             return;
         }
         store(value.updateReadFromBsdbTime(System.currentTimeMillis()));
     }

    /**
     * Return the number of errors recorded so far
     *
     * @param reset if true reset the number of errors recorded so far
     * @return the number of errors
     */
    public long getErrors(boolean reset) {
        if (reset) {
            return numOfErrors.getAndSet(0);
        } else {
            return numOfErrors.get();
        }
    }

    /**
     * Return the number of IASValues stored in the LTDB
     *
     * @param reset if true reset the number of IASValues stored in the LTDB so far
     * @return the number of IASValues stored in the LTDB
     */
    public long getValuesStored(boolean reset) {
        if (reset) {
            return numValuesStoredInLTDB.getAndSet(0);
        } else {
            return numValuesStoredInLTDB.get();
        }
    }
}
