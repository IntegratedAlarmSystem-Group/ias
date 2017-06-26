package org.eso.ias.cdb.rdb;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.PropertyDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton with a set of utility methods to use while reading/writing 
 * the RDB CDB.
 * <P>
 * <code>RdbUtils</code> is a singleton
 * <P>
 * <EM>Life cyle</EM>: the process is in charge to release the resource
 * 				  before exiting (or when does not need to access the 
 * 				  RDB anymore) by calling {@link #close()}.
 * 
 * @author acaproni
 *
 */
public class RdbUtils {
	
	/**
	 * The shutdown hook to close the resources at termination.
	 * <P>
	 * The shutdown is activated when the singleton is instantiated and its only
	 * purpose is to close the JDBC connections freeing allocate resources.
	 * 
	 * @author acaproni
	 *
	 */
	private final class RdbShutdownHook extends Thread {
		
		/**
		 * Constructor
		 */
		public RdbShutdownHook() {
			setName("RdbShutdownHook");
		}
		
		@Override
		public void run() {
			if (!closed.get()) {
				closed.set(true);
				if (sessionFactory!=null) {
					sessionFactory.close();
					sessionFactory=null;
				}
			}
		}
	}
	
	/**
	 * <code>true</code> if the {@link RdbUtils} has been closed;
	 * <code>false</code> otherwise.
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);
	
	/**
	 * <code>true</code> if the <code>sessionFactory</code> has been initialized;
	 * <code>false</code> otherwise.
	 */
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	
	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(RdbUtils.class);
	
	/**
	 * The SQL script to create the database schema 
	 */
	private static final String createTableSqlScript = "org/eso/ias/rdb/sql/CreateTables.sql";
	
	/**
	 * The SQL script to delete the database schema 
	 */
	private static final String deleteTableSqlScript = "org/eso/ias/rdb/sql/DropTables.sql";
	
	/**
	 * The hibernate configuration file
	 */
	private static final String hibernateConfig = "org/eso/ias/hibernate/config/hibernate.cfg.xml";
	
	/**
	 * The singleton
	 */
	private static AtomicReference<RdbUtils> singleton = new AtomicReference<>();
	
	/**
	 * Factory method returning the singleton
	 *  
	 * @return The RdbUtils singleton
	 */
	public synchronized static RdbUtils getRdbUtils() {
		singleton.set(new RdbUtils());
		return singleton.get();
	}
	
	
	/**
	 * The factory to get sessions.
	 * <P>
	 * According to hibernate API documentation we need
	 * only one instance of the factory to get sessions out of it.
	 */
	private SessionFactory sessionFactory=null;
	
	/**
	 * Constructor
	 * @see RdbUtils#getRdbUtils()
	 */
	private RdbUtils() {
		// Install the  shutdown hook to free the resources
		Runtime.getRuntime().addShutdownHook(new RdbUtils.RdbShutdownHook());
	}
    
	/**
	 * Create the hibernate session traffic triggering the connection
	 * with the RDB.
	 * 
	 * @return The hibernate session factory
	 */
	private SessionFactory createSessionFactory() throws IasCdbException {
		try {
			logger.debug("Bootstrapping hibernate");
			
			// Load the configuration from the jar
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			URL configFileURL = classloader.getResource(hibernateConfig);
			Configuration configuration = new Configuration();
			configuration.configure(configFileURL);

			ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();

			MetadataSources sources = new MetadataSources(serviceRegistry);
			sources.addAnnotatedClass(IasDao.class);
			sources.addAnnotatedClass(PropertyDao.class);
			sources.addAnnotatedClass(IasioDao.class);
			sources.addAnnotatedClass(AsceDao.class);
			sources.addAnnotatedClass(DasuDao.class);
			sources.addAnnotatedClass(SupervisorDao.class);
			Metadata data = sources.buildMetadata();
			logger.debug("Building the SessionFactory");
			sessionFactory = data.buildSessionFactory();
			initialized.set(true);
			return sessionFactory;
		} catch (Throwable ex) {
			logger.error("Initial SessionFactory creation failed.",ex);
			initialized.set(false);
			throw new IasCdbException(ex);
		}
	}

	/**
	 *  Return the hibernate SessionFactory singleton.
	 * 
	 * @return the SessionFactory
	 */
	private SessionFactory getSessionFactory() throws IasCdbException {
		if (sessionFactory == null) {
			sessionFactory = createSessionFactory();
			initialized.set(true);
			
		}
		return sessionFactory;
	}
	
	/**
	 * Get a Session out of the SessionFactory
	 * 
	 * @return a Session
	 * @throws IasCdbException in case of error getting the Session
	 */
	public synchronized Session getSession() throws IasCdbException {
		logger.debug("Returning a new hibernate Session");
		return getSessionFactory().openSession();
	}
	
	/**
	 * Close the factory releasing the associated resources
	 * 
	 * @see SessionFactory#close()
	 */
	public synchronized void close() {
		closed.set(true);
		if (sessionFactory!=null && initialized.get()) {
			logger.debug("Closing the hibernate session factory");
			sessionFactory.close();
			logger.debug("Hibernate session factory closed");
		}
	}
	
	/**
	 * 
	 * @return <code>true</code> if the SessionFacory is closed
	 */
	public synchronized boolean isClosed() {
		return closed.get();
	}
	
	/**
	 * Return the reader to get the SQL from.
	 *  
	 * @param resourcePath The path of the resource to read the SQL from
	 * @return
	 */
	private Reader getSqlReader(String resourcePath) {
		Objects.requireNonNull(resourcePath, "The resource path can't be null");
		if (resourcePath.isEmpty()) {
			throw new IllegalArgumentException("The resource path can't be empty");
		}
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = null;
		is = classloader.getResourceAsStream(resourcePath);
		return new BufferedReader(new InputStreamReader(is));
	}
	
	/**
	 * Run the SQL script from the passed resource path.
	 * 
	 * @param resourcePath The path of the resource to read the SQL from
	 */
	private void runSqlScript(String resourcePath) throws IasCdbException {
		Objects.requireNonNull(resourcePath, "The resource path can't be null");
		if (resourcePath.isEmpty()) {
			throw new IllegalArgumentException("The resource path can't be empty");
		}
		Reader sqlReader = getSqlReader(resourcePath);
		SqlRunner sqlRunner = new SqlRunner(sqlReader);
		sqlRunner.runSQLScript(getSession());
	}
	
	public synchronized void dropTables() throws IasCdbException {
		logger.debug("Dropping tables by running the SQL script in "+RdbUtils.deleteTableSqlScript);
		runSqlScript(RdbUtils.deleteTableSqlScript);
		
		logger.info("Database schema cleared");
	}
	
	public synchronized void createTables() throws IasCdbException {
		logger.debug("Creating database schema by running the provided SQL script "+RdbUtils.createTableSqlScript);
		runSqlScript(RdbUtils.createTableSqlScript);
		
		logger.info("Database schema created");
	}
	
}
