package org.eso.ias.converter.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eso.ias.converter.ValueMapper;
import org.eso.ias.converter.config.ConfigurationException;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the mapper function i.e. the function that gets
 * strings received from plugins (i.e. MonitorPointData)
 * and produces string to send to the core of the IAS (i.e. IASValue)
 * 
 * @author acaproni
 *
 */
public class MapperTester {
	
	/**
	 * The mock DAO for testing
	 * @author acaproni
	 *
	 */
	public class TesterConfigDao implements IasioConfigurationDAO {
		
		/**
		 * The DAO is stored in this map
		 */
		public final Map<String, MonitorPointDataHolder> mpDefs = new HashMap<>();
		
		/**
		 * Constructor
		 * 
		 * @param mpdHolders The holders to create the simulated DAO
		 */
		public TesterConfigDao(MonitorPointDataHolder[] mpdHolders) {
			Objects.requireNonNull(mpdHolders);
			for (MonitorPointDataHolder mpdh: mpdHolders) {
				mpDefs.put(mpdh.id, mpdh);
			}
		}
		
		/**
		 * True if the DAO has been initializes
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

		/** 
		 * @see org.eso.ias.converter.config.IasioConfigurationDAO#getConfiguration(java.lang.String)
		 */
		@Override
		public MonitorPointConfiguration getConfiguration(String mpId) {
			MonitorPointDataHolder mpdh = mpDefs.get(mpId);
			if (mpdh==null) {
				return null;
			} else {
				return new MonitorPointConfiguration(mpdh.iasType);
			}
		}

		/**
		 * @see org.eso.ias.converter.config.IasioConfigurationDAO#close()
		 */
		@Override
		public void close() throws ConfigurationException {
			closed=true;
		}

		/**
		 * @see org.eso.ias.converter.config.IasioConfigurationDAO#isClosed()
		 */
		@Override
		public boolean isClosed() {
			return closed;
		}
		
	}
	
	/**
	 * Helper class for holding values of a monitor point.
	 * 
	 * @author acaproni
	 */
	public class MonitorPointDataHolder {
		
		/**
		 * Constructor
		 * 
		 * @param id The identifier of the monitor point
		 * @param value The value of the monitor point
		 * @param timestamp The timestamp
		 * @param iasType The type of the monitor point
		 */
		public MonitorPointDataHolder(String id, Object value, long timestamp, IASTypes iasType) {
			super();
			this.id=id;
			this.value = value;
			this.timestamp = timestamp;
			this.iasType = iasType;
		}
		
		/**
		 * The identifier of the monitor point
		 */
		public final String id;

		/**
		 * The value of the monitor point
		 */
		public final Object value;
		
		/**
		 * The timestamp
		 */
		public final long timestamp;
		
		/**
		 * The type of the monitor point
		 */
		public IASTypes iasType;
	}
	
	/**
	 * The ID of the plugin who sent the monitor point
	 */
	private static final String pluginID = "TheIdOfThePlugin";
	
	/**
	 * The ID of the monitored system who produced the monitor point
	 */
	private static final String monitoredSystemID = "TheIdOfTheMonitoredSys";
	
	/**
	 * The object to test
	 */
	private ValueMapper mapper;
	
	
	private final MonitorPointDataHolder mpLong = new MonitorPointDataHolder("LongId",Long.valueOf(1234455667),1000, IASTypes.LONG);
	private final MonitorPointDataHolder mpInt= new MonitorPointDataHolder("IntId",Integer.valueOf(321456),1100, IASTypes.INT);
	private final MonitorPointDataHolder mpShort = new MonitorPointDataHolder("ShortId",Short.valueOf("121"),1200,IASTypes.SHORT);
	private final MonitorPointDataHolder mpByte = new MonitorPointDataHolder("ByteId",Byte.valueOf("10"), 1300,IASTypes.BYTE);
	private final MonitorPointDataHolder mpDouble = new MonitorPointDataHolder("DoubleId",Double.valueOf(2234.6589), 1400, IASTypes.DOUBLE);
	private final MonitorPointDataHolder mpFloat = new MonitorPointDataHolder("FloatId",Float.valueOf(554466.8702f), 1500,IASTypes.FLOAT);
	private final MonitorPointDataHolder mpBool = new MonitorPointDataHolder("BoolId",Boolean.FALSE, 1600, IASTypes.BOOLEAN);
	private final MonitorPointDataHolder mpChar = new MonitorPointDataHolder("CharId",Character.valueOf('X'), 1700, IASTypes.CHAR);
	private final MonitorPointDataHolder mpString = new MonitorPointDataHolder("StrId","The string", 1800, IASTypes.STRING);
	private final MonitorPointDataHolder mpAlarm = new MonitorPointDataHolder("AlarmId",AlarmSample.SET, 1900, IASTypes.ALARM);
	
	/**
	 * The holders
	 */
	public final MonitorPointDataHolder mpdHolders[] = 
		{ mpLong, mpInt, mpShort, mpByte, mpDouble, mpFloat, mpBool, mpChar, mpString, mpAlarm };
	
	/**
	 * The identifier of the converter
	 */
	public final String converterID = "SimulatedConverterId";
	
	/**
	 * The serializer to serialize {@link IASValue} in a string
	 * and vice-versa
	 */
	private final IasValueStringSerializer iasValueSerializer = new IasValueJsonSerializer();
	
	/**
	 * Build a {@link MonitorPointData} from a {@link MonitorPointDataHolder}
	 * 
	 * @param mpHolder The MonitorPointDataHolder
	 * @return The MonitorPointData
	 */
	private MonitorPointData buildMonitorPointData(MonitorPointDataHolder mpHolder) {
		Objects.requireNonNull(mpHolder);
		List<Sample> samples = new ArrayList<>();
		samples.add(new Sample(mpHolder.value));
		FilteredValue fv = new FilteredValue(mpHolder.value, samples, mpHolder.timestamp);
		ValueToSend vts = new ValueToSend(mpHolder.id, fv);
		return new MonitorPointData(pluginID, monitoredSystemID, vts);
	}
	
	@Before
	public void setUp() {
		// Ensures we are going to test all implemented types
		assertTrue(IASTypes.values().length==mpdHolders.length);
		
		IasioConfigurationDAO testerDao = new TesterConfigDao(mpdHolders);
		
		mapper = new ValueMapper(testerDao, iasValueSerializer, converterID);
	}
	
	@After
	public void tearDown() {
		
	}
	
	/**
	 * Test the behavior of the mapper when tries to translate a unconfigured
	 * monitor point.
	 * <P>
	 * If the monitor point is unconfigured, the translation is
	 * not possible and the mapper return <code>null</code>.
	 * @throws Exception
	 */
	@Test
	public void testUnconfiguredMPD() throws Exception {
		MonitorPointDataHolder unconfiguredMpdh = 
				new MonitorPointDataHolder("Unrecognized", AlarmSample.CLEARED, System.currentTimeMillis(), IASTypes.ALARM);
		
		MonitorPointData mpd = buildMonitorPointData(unconfiguredMpdh);
		
		String ret = mapper.apply(mpd.toJsonString());
		assertNull(ret);
		
		// Translate another mp to be shure the previous error
		// did not brake the processor
		assertNotNull(mapper.apply(buildMonitorPointData(mpdHolders[0]).toJsonString()));
	}
	
	/**
	 * Test the translation of each type in the correct {@link IASValue}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMapping() throws Exception {
		for (MonitorPointDataHolder mpdh: mpdHolders) {
			MonitorPointData mpd = buildMonitorPointData(mpdh);
			String iasValueStr = mapper.apply(mpd.toJsonString());
			assertNotNull(iasValueStr);
			
			IASValue<?> iasValue =  iasValueSerializer.valueOf(iasValueStr);
			
			assertEquals(mpdh.iasType, iasValue.valueType);
			assertEquals(mpdh.timestamp, iasValue.timestamp);
			assertEquals(mpdh.id, iasValue.id);
			assertEquals(mpdh.value,iasValue.value);
		}
	}

}
