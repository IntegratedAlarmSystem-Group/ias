package org.eso.ias.converter.test;

import org.eso.ias.converter.config.ConfigurationException;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.Filter.EnrichedSample;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A base class providing common utility methods used
 * by several test in Converter.
 * 
 * @author acaproni
 *
 */
public class ConverterTestBase {
	
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
		 * @param pluginProductionTSamp The timestamp when the plugin produced the value
		 * @param pluginSentTSamp The timestamp when the plugin sent the value to the converter
		 * @param iasType The type of the monitor point
		 */
		public MonitorPointDataHolder(
				String id, 
				Object value, 
				long pluginProductionTSamp,
				long pluginSentTSamp,
				IASTypes iasType) {
			super();
			this.id=id;
			this.value = value;
			this.pluginProductionTSamp = pluginProductionTSamp;
			this.pluginSentTSamp=pluginSentTSamp;
			this.iasType = iasType;

			logger.debug("MPDH {} of type {} and value={} built with pluginProdTSTamp={} and sentTStamp={}",
					id,
					iasType,
					value,
					org.eso.ias.utils.ISO8601Helper.getTimestamp(pluginProductionTSamp),
					org.eso.ias.utils.ISO8601Helper.getTimestamp(pluginSentTSamp));

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
		 * The time-stamp when the plugin produced the value
		 */
		public final long pluginProductionTSamp;
		
		/**
		 * The time-stamp when the plugin sent the value
		 * to the converter
		 */
		public final long pluginSentTSamp;
		
		/**
		 * The type of the monitor point
		 */
		public IASTypes iasType;
	}
	
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

		/** 
		 * @see org.eso.ias.converter.config.IasioConfigurationDAO#getConfiguration(java.lang.String)
		 */
		@Override
		public Optional<MonitorPointConfiguration> getConfiguration(String mpId) {
			MonitorPointDataHolder mpdh = mpDefs.get(mpId);
			if (mpdh==null) {
				return Optional.empty();
			} else {
				return Optional.ofNullable(new MonitorPointConfiguration(mpdh.iasType,Optional.empty(),Optional.empty()));
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
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(ConverterTestBase.class);
	
	/**
	 * The identifier of the converter
	 */
	protected final String converterID = "SimulatedConverterId";
	
	/**
	 * The serializer to serialize {@link IASValue} in a string
	 * and vice-versa
	 */
	protected final IasValueStringSerializer iasValueSerializer = new IasValueJsonSerializer();
	
	/**
	 * The ID of the plugin who sent the monitor point
	 */
	protected static final String pluginID = "TheIdOfThePlugin";
	
	/**
	 * The ID of the monitored system who produced the monitor point
	 */
	protected static final String monitoredSystemID = "TheIdOfTheMonitoredSys";
	
	/**
	 * A holder for type Long
	 */
	private final MonitorPointDataHolder mpLong = new MonitorPointDataHolder(
			"LongId",
			Long.valueOf(1234455667),
			1000L, 
			1050L,
			IASTypes.LONG);
	
	/**
	 * A holder for type Integer
	 */
	private final MonitorPointDataHolder mpInt= new MonitorPointDataHolder(
			"IntId",
			Integer.valueOf(321456),
			1100L,
			1150L,
			IASTypes.INT);
	
	/**
	 * A holder for type Short
	 */
	private final MonitorPointDataHolder mpShort = new MonitorPointDataHolder(
			"ShortId",
			Short.valueOf("121"),
			1200L,
			1250L,
			IASTypes.SHORT);
	
	/**
	 * A holder for type Byte
	 */
	private final MonitorPointDataHolder mpByte = new MonitorPointDataHolder(
			"ByteId",
			Byte.valueOf("10"), 
			1300L,
			1350L,
			IASTypes.BYTE);
	
	/**
	 * A holder for type Double
	 */
	private final MonitorPointDataHolder mpDouble = new MonitorPointDataHolder(
			"DoubleId",
			Double.valueOf(2234.6589), 
			1400L,
			1450L,
			IASTypes.DOUBLE);
	
	/**
	 * A holder for type Float
	 */
	private final MonitorPointDataHolder mpFloat = new MonitorPointDataHolder(
			"FloatId",
			Float.valueOf(554466.8702f), 
			1500L,
			1550L,
			IASTypes.FLOAT);
	
	/**
	 * A holder for type Boolean
	 */
	private final MonitorPointDataHolder mpBool = new MonitorPointDataHolder(
			"BoolId",
			Boolean.FALSE, 
			1600L,
			1650L,
			IASTypes.BOOLEAN);
	
	/**
	 * A holder for type Character
	 */
	private final MonitorPointDataHolder mpChar = new MonitorPointDataHolder(
			"CharId",Character.valueOf('X'), 
			1700L,
			1750L,
			IASTypes.CHAR);
	
	/**
	 * A holder for type String
	 */
	private final MonitorPointDataHolder mpString = new MonitorPointDataHolder(
			"StrId",
			"The string", 
			1800L,
			1850L,
			IASTypes.STRING);
	
	/**
	 * A holder for type Alarm
	 */
	private final MonitorPointDataHolder mpAlarm = new MonitorPointDataHolder(
			"AlarmId",
			Alarm.getInitialAlarmState().set(),
			1900L,
			1950L,
			IASTypes.ALARM);

	/**
	 * A holder for type Timestamp
	 */
	private final MonitorPointDataHolder mpTimestamp = new MonitorPointDataHolder(
			"TStampId",
			Long.valueOf(System.currentTimeMillis()),
			2000L,
			2050L,
			IASTypes.TIMESTAMP);

	/**
	 * A holder for type Arrray of long
	 */
	List<Long> longVals = Stream.of(1L,2L,3L).collect(Collectors.toList());
	NumericArray longNumVals = new NumericArray(NumericArray.NumericArrayType.LONG,longVals);
	private final MonitorPointDataHolder mpArrayOfLongs = new MonitorPointDataHolder(
			"LongArrayId",
			longVals,
			2100L,
			2150L,
			IASTypes.ARRAYOFLONGS);

	/**
	 * A holder for type Arrray of doubles
	 */
	List<Double	> doubleVals = Stream.of(0.3D,-0.15,13245.789).collect(Collectors.toList());
	NumericArray doubleNumVals = new NumericArray(NumericArray.NumericArrayType.DOUBLE,doubleVals);
	private final MonitorPointDataHolder mpArrayOfDoubles = new MonitorPointDataHolder(
			"DoubleArrayId",
			doubleVals,
			2200L,
			2250L,
			IASTypes.ARRAYOFDOUBLES);

	/**
	 * The holders: one for each type
	 */
	protected final MonitorPointDataHolder mpdHolders[] = 
		{
				mpLong, mpInt, mpShort, mpByte,
				mpDouble, mpFloat,
				mpBool,
				mpChar, mpString,
				mpTimestamp,
				mpArrayOfLongs, mpArrayOfDoubles,
				mpAlarm };
	
	
	
	/**
	 * Build a {@link MonitorPointData} from a {@link MonitorPointDataHolder}
	 * 
	 * @param mpHolder The MonitorPointDataHolder
	 * @return The MonitorPointData
	 */
	protected MonitorPointData buildMonitorPointData(MonitorPointDataHolder mpHolder) {
		Objects.requireNonNull(mpHolder);
		List<EnrichedSample> samples = new ArrayList<>();
		samples.add(new EnrichedSample(new Sample(mpHolder.value),true));
		FilteredValue fv = new FilteredValue(mpHolder.value, samples, mpHolder.pluginProductionTSamp);
		ValueToSend vts = new ValueToSend(
				mpHolder.id, 
				fv,
				OperationalMode.DEGRADED,
				IasValidity.RELIABLE);
		return new MonitorPointData(pluginID, monitoredSystemID, vts);
	}

	public ConverterTestBase() {
		// TODO Auto-generated constructor stub
	}

}
