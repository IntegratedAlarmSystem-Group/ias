package org.eso.ias.converter;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.converter.config.ConfigurationException;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.IasioConfigurationDaoImpl;
import org.eso.ias.heartbeat.HbEngine;
import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.HeartbeatStatus;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.IdentifierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * The tool to convert raw monitor point values and alarms
 * coming from plugins into IAS data type to be processed by the core.
 * <P>
 * The converter runs a never-ending loop
 * that consists of:
 * <UL>
 * 	<LI>get one value produced by the remote system
 * 	<LI>convert the value in the corresponding IAS data type
 * 	<LI>send the value to the core for processing
 * </ul>
 *
 * <P>
 * The conversion needs to access the configuration database.
 * <P>
 * Spring constructor-dependency injection is used mainly for testing purposes.
 *
 * @author acaproni
 *
 */
public class Converter {

	/**
	 * The identifier of the converter
	 */
	public final String converterId;

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(Converter.class);

	/**
	 * Signal the thread to terminate
	 */
	private AtomicBoolean closed=new AtomicBoolean(false);

	/**
	 * The DAO to get the configuration of monitor points
	 */
	private IasioConfigurationDAO configDao;

	/**
	 * The serializer to transform IASValues into strings
	 * to send to the core of the IAS
	 */
	private final IasValueStringSerializer iasValueStrSerializer;

	/**
	 * The stream to get values from the plugins and
	 * sends them to the core of the IAS in the proper format
	 */
	private final ConverterStream converterStream;

	/**
	 * The function to map the json
	 */
	private final Function<String, String> mapper;
	
	/**
	 * The engine to send HBs 
	 */
	private final HbEngine hbEngine;

	/**
	 * The shutdown thread for a clean exit
	 */
	private Thread shutDownThread=new Thread("Converter shutdown thread") {
		@Override
		public void run() {
			Converter.this.tearDown();
		}
	};

	/**
	 * Constructor.
	 * <P>
	 * Dependency injection with spring take place here.
	 *
	 * @param id The not <code>null</code> nor empty ID of the converter
	 * @param cdbReader The DAO of the configuration database
	 * @param converterStream The stream to convert monitor point data into IAS values
	 * @param hbProducer the sender of HB messages
	 */
	public Converter(
			String id,
			CdbReader cdbReader,
			ConverterStream converterStream,
			HbProducer hbProducer) throws ConfigurationException {
		Objects.requireNonNull(id);
		if (id.trim().isEmpty()) {
			throw new InvalidParameterException("The ID of the converter can't be empty");
		}
		converterId = id.trim();
		
		Objects.requireNonNull(cdbReader);
		
		Optional<IasDao> iasDao = null;
		
		try {
			iasDao=cdbReader.getIas();
		} catch (IasCdbException cdbExcp) {
			throw new ConfigurationException("Error getting the configuration of the IAS",cdbExcp);
		}
		if (!iasDao.isPresent()) {
			throw new ConfigurationException("Got and empty IAS configuration from the CDB reader");
		}
		
		int hbFrequency = iasDao.get().getHbFrequency();
		if (hbFrequency<=0) {
			throw new ConfigurationException("Invalid HB frequency: "+hbFrequency);
		}
		
		Objects.requireNonNull(hbProducer,"The producer to publish HBs can't be null");
		hbEngine = new HbEngine(
				converterId, 
				hbFrequency, 
				hbProducer);
		
		this.configDao= new IasioConfigurationDaoImpl(cdbReader);
		try {
			cdbReader.shutdown();
		} catch (IasCdbException e) {
			logger.warn("Exception got closing the CDB",e);
		}

		this.iasValueStrSerializer=	new IasValueJsonSerializer();

		mapper = new ValueMapper(this.configDao, this.iasValueStrSerializer,id);

		Objects.requireNonNull(converterStream);
		this.converterStream=converterStream;
	}

	/**
	 * Initialize the converter and start the loop
	 *
	 * @throws ConfigurationException in case of error in the configuration
	 */
	public void setUp() throws ConfigurationException {
		logger.info("Converter {} initializing...", converterId);
		
		hbEngine.start();
		
		configDao.initialize();
		Runtime.getRuntime().addShutdownHook(shutDownThread);
		// Init the stream
		try {
			converterStream.initialize(mapper);
		} catch (ConverterStreamException cse) {
			throw new ConfigurationException("Error initializing the stream",cse);
		}
		// Start the thread
		try {
			converterStream.start();
		} catch (Exception e) {
			throw new ConfigurationException("Error activating the stream",e);
		}
		
		hbEngine.updateHbState(HeartbeatStatus.RUNNING);
		logger.info("Converter {} initialized", converterId);
	}

	/**
	 * Shut down the loop and free the resources.
	 */
	public void tearDown() {
		if (closed.getAndSet(true)) {
			logger.info("Converter {} already closed", converterId);
			return;
		}
		logger.info("Converter {} shutting down...", converterId);
		
		hbEngine.updateHbState(HeartbeatStatus.EXITING);
		
		Runtime.getRuntime().removeShutdownHook(shutDownThread);

		try {
			converterStream.stop();
		}  catch (Exception e) {
			logger.error("Converter {}: exception got while terminating the streaming", converterId,e);
		}
		
		hbEngine.shutdown();
		logger.info("Converter {} shutted down.", converterId);
	}

	private static void printUsage() {
		System.out.println("Usage: Converter Convert-ID [-jcdb JSON-CDB-PATH]");
		System.out.println("-jcdb force the usage of the JSON CDB");
		System.out.println("   * Convert-ID: the identifier of the converter");
		System.out.println("   * JSON-CDB-PATH: the path of the JSON CDB\n");
	}

	/**
	 * Application starting point.
	 *
	 * It builds a converter with the help of spring dependency injection
	 *
	 * @param args arguments
	 */
	public static void main(String[] args) {
		if (args.length!=1 && args.length!=3) {
			printUsage();
			System.exit(-1);
		}
		String id=args[0];

		CdbReader cdbReader = null;

		/**
		 * Spring stuff
		 */
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConverterConfig.class);

		if (args.length==3) {
			String cmdLineSwitch = args[1];
			if (cmdLineSwitch.compareTo("-jcdb")!=0) {
				printUsage();
				System.exit(-2);
			}
			String cdbPath = args[2];
			File f = new File(cdbPath);
			if (!f.isDirectory() || !f.canRead()) {
				System.err.println("Invalid file path "+cdbPath);
				System.exit(-3);
			}

			CdbFiles cdbFiles=null;
			try {
				cdbFiles= new CdbJsonFiles(f);
			} catch (Exception e) {
				System.err.println("Error initializing JSON CDB "+e.getMessage());
				System.exit(-4);
			}
			cdbReader = new JsonReader(cdbFiles);
		} else {
			// Get from dependency injection
			cdbReader = context.getBean("cdbReader",CdbReader.class);
		}
		
		IasDao iasDao = null;
		try { 
			Optional<IasDao> iasDaoOpt = cdbReader.getIas();
			iasDao=iasDaoOpt.get();
		} catch (IasCdbException cdbEx) {
			logger.error("Error getting IAS configuration from CDB",cdbEx);
			System.exit(-1);
		}
		
		Optional<String> kafkaBrokersFromCdb = Optional.ofNullable(iasDao.getBsdbUrl());

		ConverterStream converterStream = context.getBean(ConverterStream.class,id, kafkaBrokersFromCdb, System.getProperties());
		
		HbProducer hbProducer = context.getBean(HbProducer.class,id,kafkaBrokersFromCdb,System.getProperties());

		logger.info("Building the {} Converter",id);

		Converter converter = null;
		try { 
			converter=new Converter(id,cdbReader,converterStream,hbProducer);
		} catch (Exception e) {
			logger.error("Exception building the converter {}",id,e);
			System.exit(-1);
		}
		
		try {
			converter.setUp();
			logger.info("Converter {} initialized",id);
		} catch (Exception e) {
			logger.error("Error initializing the converter {}",id,e);

		}
		context.close();
		logger.info("Converter {} terminated",id);
	}



}
