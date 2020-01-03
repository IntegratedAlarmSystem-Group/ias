package org.eso.ias.converter;

import org.apache.commons.cli.*;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.command.CommandManager;
import org.eso.ias.command.DefaultCommandExecutor;
import org.eso.ias.converter.config.ConfigurationException;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.IasioConfigurationDaoImpl;
import org.eso.ias.heartbeat.HbEngine;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.HeartbeatProducerType;
import org.eso.ias.heartbeat.HeartbeatStatus;
import org.eso.ias.logging.IASLogger;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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
public class Converter implements AutoCloseable {

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
	 * The receiver and executor of commands
	 */
	private final CommandManager commandManager;

	/**
	 * The shutdown thread for a clean exit
	 */
	private Thread shutDownThread=new Thread("Converter shutdown thread") {
		@Override
		public void run() {
			Converter.this.close();
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
	 * @param cmdManager The command manager
	 */
	public Converter(
			String id,
			CdbReader cdbReader,
			ConverterStream converterStream,
			HbProducer hbProducer,
			CommandManager cmdManager) throws ConfigurationException {
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
		hbEngine = HbEngine.getInstance(
				converterId,
				HeartbeatProducerType.CONVERTER,
				hbFrequency, 
				hbProducer);

		Objects.requireNonNull(cmdManager,"The command executor can't be null");
		this.commandManager=cmdManager;
		
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

		try {
			commandManager.start(new DefaultCommandExecutor(),this);
		} catch (Exception e) {
			throw new ConfigurationException("Error activating the command executor",e);
		}
		
		hbEngine.updateHbState(HeartbeatStatus.RUNNING);
		logger.info("Converter {} initialized", converterId);
	}

	/**
	 * Shut down the loop and free the resources.
	 */
	public void close() {
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

		commandManager.close();
		
		hbEngine.shutdown();
		logger.info("Converter {} shutted down.", converterId);
	}

    /**
     * The string shown by the help
     */
	private static String cmdLineSyntax = "Usage: Converter Converter-ID [-j|-jcdb JSON-CDB-PATH] [-h|--help] [-x|--logLevel] level";


	/**
	 * Parse the command line.
	 *
	 * If help is requested, prints the message and exits.
	 *
	 * @param args The params read from the command line
	 * @param params the map of values read from the command line
	 */
	public static void parseCommandLine(String[] args, Map<String, Optional<?>> params) {
        Options options = new Options();
        options.addOption("h", "help", false, "Print help and exit");
        options.addOption("j", "jcdb", true, "Use the JSON Cdb at the passed path");
        options.addOption("x", "logLevel", true, "Set the log level (TRACE, DEBUG, INFO, WARN, ERROR)");

        CommandLineParser parser = new DefaultParser();

        CommandLine cmdLine = null;
        try {
            cmdLine = parser.parse(options, args);
        } catch (Exception e) {
            HelpFormatter helpFormatter = new HelpFormatter();
            System.err.println("Exception parsing the comamnd line: " + e.getMessage());
            e.printStackTrace(System.err);
            helpFormatter.printHelp(cmdLineSyntax, options);
            System.exit(-1);
        }
        boolean help = cmdLine.hasOption('h');

        Optional<String> jcdb = Optional.ofNullable(cmdLine.getOptionValue('j'));
        Optional<String> logLevelName = Optional.ofNullable(cmdLine.getOptionValue('x'));
        Optional<LogLevelDao> logLvl=Optional.empty();
        try {
            logLvl = logLevelName.map(name -> LogLevelDao.valueOf(name));
        } catch (Exception e) {
            System.err.println("Unrecognized log level");
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp(cmdLineSyntax, options);
            System.exit(-1);
        }


        List<String> remaingArgs = cmdLine.getArgList();

        Optional<String> supervId;
        if (remaingArgs.isEmpty()) {
            supervId = Optional.empty();
        } else {
            supervId = Optional.of(remaingArgs.get(0));
        }

        if (help) {
            new HelpFormatter().printHelp(cmdLineSyntax, options);
            System.exit(0);
        }
		if (!supervId.isPresent()) {
			System.err.println("Missing Converter ID");
			new HelpFormatter().printHelp(cmdLineSyntax, options);
			System.exit(-1);
		}

        params.put("ID",supervId);
		params.put("jcdb",jcdb);
		params.put("log",logLvl);

		Converter.logger.info("Params from command line: jcdb={}, logLevel={} converter ID={}",
				jcdb.orElse("Undefined"),
				logLvl.map( l -> l.name()).orElse("Undefined"),
				supervId.orElse("Undefined"));
	}

	/**
	 * Application starting point.
	 *
	 * It builds a converter with the help of spring dependency injection
	 *
	 * @param args arguments
	 */
	public static void main(String[] args) {
	    Map<String,Optional<?>> params = new HashMap<>();
	    parseCommandLine(args,params);

	    Optional<?> supervIdOpt = params.get("ID");
	    if (!supervIdOpt.isPresent()) {
	        throw new IllegalArgumentException("Missing converter ID");
        }
        String id= (String)supervIdOpt.get();

		CdbReader cdbReader = null;

		/**
		 * Spring stuff
		 */
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConverterConfig.class);

		 Optional<?> jcdbOpt = params.get("jcdb");
		 if (jcdbOpt.isPresent()) {
		     String cdbPath = (String)jcdbOpt.get();
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
			if (!iasDaoOpt.isPresent()) {
			   logger.error("IAS config not found in the CDB");
			   System.exit(-1);
            }
			iasDao=iasDaoOpt.get();
		} catch (IasCdbException cdbEx) {
			logger.error("Error getting IAS configuration from CDB",cdbEx);
			System.exit(-1);
		}

		// Set the log level
		Optional<LogLevelDao> logLevelFromIasOpt = Optional.ofNullable(iasDao.getLogLevel());
		Optional<LogLevelDao> logLevelFromCmdLineOpt = (Optional<LogLevelDao>)params.get("log");
        IASLogger.setLogLevel(
             logLevelFromCmdLineOpt.map(l -> l.toLoggerLogLevel()).orElse(null),
             logLevelFromIasOpt.map(l -> l.toLoggerLogLevel()).orElse(null),
             null);

		Optional<String> kafkaBrokersFromCdb = Optional.ofNullable(iasDao.getBsdbUrl());

		ConverterStream converterStream = context.getBean(ConverterStream.class,id, kafkaBrokersFromCdb, System.getProperties());
		
		HbProducer hbProducer = context.getBean(HbProducer.class,id,kafkaBrokersFromCdb,System.getProperties());

		CommandManager commandManager = context.getBean(CommandManager.class,id,kafkaBrokersFromCdb, System.getProperties());

		logger.info("Building the {} Converter",id);

		Converter converter = null;
		try { 
			converter=new Converter(id,cdbReader,converterStream,hbProducer,commandManager);
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
	}
}
