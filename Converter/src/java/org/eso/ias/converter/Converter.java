package org.eso.ias.converter;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.converter.config.ConfigurationException;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.IasioConfigurationDaoImpl;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
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
	public final String converterID;

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(Converter.class);

	/**
	 * Signal the thread to terminate
	 */
	private volatile boolean closed=false;

	/**
	 * The DAO to get the configuration from the CDB
	 */
	private final CdbReader cdbDAO;

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
	 */
	public Converter(
			String id,
			CdbReader cdbReader,
			ConverterStream converterStream) {
		Objects.requireNonNull(id);
		if (id.trim().isEmpty()) {
			throw new InvalidParameterException("The ID of the converter can't be empty");
		}
		converterID=id.trim();
		Objects.requireNonNull(cdbReader);
		this.cdbDAO=cdbReader;
		this.configDao= new IasioConfigurationDaoImpl(cdbReader);

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
		logger.info("Converter {} initializing...", converterID);
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
		logger.info("Converter {} initialized", converterID);
	}

	/**
	 * Shut down the loop and free the resources.
	 */
	public void tearDown() {
		if (closed) {
			logger.info("Converter {} already closed", converterID);
			return;
		}
		logger.info("Converter {} shutting down...", converterID);
		Runtime.getRuntime().removeShutdownHook(shutDownThread);

		closed=true;
		try {
			converterStream.stop();
		}  catch (Exception e) {
			logger.error("Converter {}: exception got while terminating the streaming", converterID,e);
		}
		logger.info("Converter {} shutted down.", converterID);
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

		ConverterStream converterStream = context.getBean(ConverterStream.class,id, new System.getProperties());

		logger.info("Converter {} started",id);

		Converter converter = new Converter(id,cdbReader,converterStream);
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
