package org.eso.ias.converter;

import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.converter.config.IasioConfigurationDAO;
import org.eso.ias.converter.config.MonitorPointConfiguration;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValueBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * The tool to convert raw monitor point values and alarms
 * coming from plugins into IAS data type to be processed by the core.
 * <P>
 * The converter consists of a never-ending tool that
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
public class Converter implements Runnable {
	
	/**
	 * The identifier of the converter
	 */
	private final String converterID;
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(Converter.class);
	
	/**
	 * The thread getting and converting data
	 * @see #run()
	 */
	private Thread converterThread = null;
	
	/**
	 * Signal the thread to terminate
	 */
	private volatile boolean closed=false;
	
	/**
	 * The object to get data from the remote monitored systems
	 */
	private RawDataReader mpGetter;
	
	/**
	 * The object to convert a monitor pint in a valid IAS data type
	 */
	private ConverterEngine converter;
	
	/**
	 * The DAO to get the configuration from the CDB
	 */
	private final CdbReader cdbDAO;
	
	/**
	 * The DAO to get the configuration of monitor points
	 */
	private IasioConfigurationDAO configDao;
	
	/**
	 * The object to send converted monitor point values and alarms
	 * to the core of the IAS
	 */
	private CoreFeeder mpSender;
	
	/**
	 * Constructor.
	 * <P>
	 * Dependency injection with spring take place here.
	 * 
	 * @param id The not <code>null</code> nor empty ID of the converter
	 * @param mpReader The reader to get monitor point and alarms 
	 *        provided by remote monitored control systems
	 * @param cdbReader The DAO of the configuration database
	 * @param feeder The publisher of IASIO data to the core of the IAS  
	 */
	public Converter(
			String id,
			RawDataReader mpReader,
			CdbReader cdbReader,
			CoreFeeder feeder) {
		Objects.requireNonNull(id);
		if (id.isEmpty()) {
			throw new InvalidParameterException("The ID of the converter can't be empty");
		}
		this.converterID=id;
		Objects.requireNonNull(mpReader);
		this.mpGetter=mpReader;
		Objects.requireNonNull(cdbReader);
		this.cdbDAO=cdbReader;
		Objects.requireNonNull(feeder);
		this.mpSender=feeder;
	}
	
	/**
	 * Initialize the converter and start the loop
	 */
	public void setUp() {
		Runtime.getRuntime().addShutdownHook(new Thread("Converter "+converterID+" shutdown thread") {
			public void run() {
				Converter.this.tearDown();
			}
		});
		converterThread = new Thread(this, "Converter "+converterID+" thread");
		converterThread.setDaemon(true);
		converterThread.start();
	}
	
	/**
	 * Shut down the loop and free the resources.
	 */
	public void tearDown() {
		logger.info("Converter {} shutting down...", converterID);
		closed=true;
		if (converterThread.isAlive()) {
			converterThread.interrupt();
			try {
				converterThread.join(60000);
				logger.info("Converter {} thread terminated", converterID);
			} catch (InterruptedException e) {
				logger.error("Converter {}: exception got while waiting for thread termination", converterID,e);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * The loop to get and convert values received from plugins.
	 * 
	 */
	@Override
	public void run() {
		logger.info("Converter {} tread running",converterID);
		while (!closed) {
			// Get a value produced by a monitored system
			MonitorPointData mPoint=mpGetter.get(1, TimeUnit.SECONDS);
			if (mPoint==null) {
				continue;
			}
			// Get the configuration from the CDB
			String mpId = mPoint.getId();
			MonitorPointConfiguration mpConfiguration=configDao.getConfiguration(mpId);
			if (mpConfiguration==null) {
				logger.error("Nno configuration found for {}: raw value lost",mpId);;
			}
			IASTypes iasType = mpConfiguration.mpType;
			// Convert the monitor point in the IAS type
			IASValueBase iasValue=converter.translate(mPoint, iasType);
			// Send data to the core of the IAS
			mpSender.push(iasValue);
		}
		logger.info("Converter {} tread terminated",converterID);
	}

	/**
	 * Application starting point.
	 * 
	 * It builds a converter with the help of spring dependency injection
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String id="IasConverter";
		
		/**
		 * Spring stuff
		 */
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConverterConfig.class);
		CdbReader cdbReader = context.getBean("cdbReader",CdbReader.class);
		RawDataReader rawDataReader = context.getBean("rawDataReader",RawDataReader.class);
		CoreFeeder coreFeeder = context.getBean("coreFeeder",CoreFeeder.class);
		
		logger.info("Converter {} started",id);
		Converter converter = new Converter(id,rawDataReader,cdbReader,coreFeeder);
		try {
			converter.setUp();
			logger.info("Converter {} initialized",id);
		} catch (Exception e) {
			logger.error("Error initializing the converter {}",id,e);
			
		}
		
		logger.info("Converter {} terminated",id);
	}

	

}
