package org.eso.ias.converter.test;

import org.eso.ias.converter.ValueMapper;
import org.eso.ias.converter.config.ConfigurationException;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.Filter;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.types.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TemplatedConversionTest {

    class TestConfigDao implements IasioConfigurationDAO {

        /** The cache with the configurations */
        private final Map<String, MonitorPointConfiguration> configMap = new HashMap<>();

        /**
         * True if the DAO has been initialized
         */
        private volatile boolean inited =false;

        /**
         * True if the DAO has been closed
         */
        private volatile boolean closed =false;

        /**
         * @see org.eso.ias.converter.config.IasioConfigurationDAO#initialize()
         */
        @Override
        public void initialize() throws ConfigurationException {
            inited=true;
        }

        /**
         * @see org.eso.ias.converter.config.IasioConfigurationDAO#isInitialized()
         */
        @Override
        public boolean isInitialized() {
            return inited;
        }

        @Override
        public Optional<MonitorPointConfiguration> getConfiguration(String mpId) {
            return Optional.ofNullable(configMap.get(mpId));
        }

        @Override
        public void close() throws ConfigurationException {
            closed=true;
        }

        @Override
        public boolean isClosed() {  return closed;   }

        /**
         * Adds a new configuration
         *
         * @param id The ID of the monitor point
         * @param mpType The type of the monitor point
         * @param min The min allowed instance (or null if not templated)
         * @param max The max allowed instance (or null if not templated)
         */
        public void addConfig(String id, IASTypes mpType, Integer min, Integer max) {
            MonitorPointConfiguration conf = new MonitorPointConfiguration(
                    mpType,
                    Optional.ofNullable(min),
                    Optional.ofNullable(max));
            configMap.put(id, conf);
        }
    }

    /** The logger */
    private static final Logger logger = LoggerFactory.getLogger(TemplatedConversionTest.class);

    String convertId = "CoverterId";
    String pluginId = "PluginId";
    String monSysId ="MonitoredSystemId";
    String idOfTemplatedMp = "TemplateId";

    /** Configuration DAO */
    TestConfigDao configDao;

    ValueMapper valueMapper;

    /** The IASValue serializer */
    IasValueJsonSerializer serializer = new IasValueJsonSerializer();

    /**
     * Build the String received by the ValueMapper.apply()
     *
     * @param id The ID of the monitor point
     * @param alarm The alarm to send
     * @param instance The number of instance (defined only if templated)
     *
     **/
    String buildMPDataString(String id, Alarm alarm,Optional<Integer> instance) throws  Exception {
        Sample s = new Sample(alarm);
        Filter.EnrichedSample sample = new Filter.EnrichedSample(s,true);
        List<Filter.EnrichedSample> samples = new LinkedList<>();
        samples.add(sample);

        String idToSend = (instance.isPresent())? id+Identifier.templatedIdPrefix()+instance.get()+Identifier.templateSuffix(): id;


        ValueToSend value = new ValueToSend(
                idToSend,
                alarm,
                samples,
                System.currentTimeMillis(),
                OperationalMode.OPERATIONAL,
                IasValidity.RELIABLE);

        MonitorPointData mpd = new MonitorPointData(pluginId,monSysId,value);
        return mpd.toJsonString();
    }

    @BeforeEach
    void setUp() throws Exception {
        configDao = new TestConfigDao();
        valueMapper = new ValueMapper(configDao,serializer,convertId);
        configDao.initialize();
        logger.info("Setup completed");
    }

    @Test
    public void testNonTemplatedTranslation() throws Exception {
        logger.info("Running testNonTemplatedTranslation");

        configDao.addConfig("NotTemplatedId",IASTypes.ALARM,null,null);
        configDao.addConfig(idOfTemplatedMp,IASTypes.ALARM,3,6);

        String nonTemplatedStr = buildMPDataString("NotTemplatedId",Alarm.SET_HIGH,Optional.empty());
        String generatedIasValue = valueMapper.apply(nonTemplatedStr);
        assertNotNull(generatedIasValue);
    }

    @Test
    public void templatedTranslation() throws Exception {
       logger.info("Running templatedTranslation");

       configDao.addConfig("NotTemplatedId",IASTypes.ALARM,null,null);
       configDao.addConfig(idOfTemplatedMp,IASTypes.ALARM,3,6);
       String templatedIdemplatedStr = buildMPDataString(idOfTemplatedMp,Alarm.SET_HIGH,Optional.of(5));
       String templatedIasValue = valueMapper.apply(templatedIdemplatedStr);
       assertNotNull(templatedIasValue);

    }

    @Test
    public void templateWrongInstance() throws Exception {
       logger.info("Running templateWrongInstance");

       configDao.addConfig("NotTemplatedId",IASTypes.ALARM,null,null);
       configDao.addConfig(idOfTemplatedMp,IASTypes.ALARM,3,6);

       String templatedIdemplatedStr = buildMPDataString(idOfTemplatedMp,Alarm.SET_HIGH,Optional.of(0));
       String templatedIasValue = valueMapper.apply(templatedIdemplatedStr);
       assertNull(templatedIasValue);

    }

    @Test
    public void testAMpThatDoesNotExist() throws Exception {
       logger.info("Running testAMpThatDoesNotExist");

       configDao.addConfig("NotTemplatedId",IASTypes.ALARM,null,null);
       configDao.addConfig(idOfTemplatedMp,IASTypes.ALARM,3,6);

       String templatedIdemplatedStr = buildMPDataString("NotFoundID",Alarm.SET_HIGH,Optional.of(4));
       String templatedIasValue = valueMapper.apply(templatedIdemplatedStr);
       assertNull(templatedIasValue);
    }


}
