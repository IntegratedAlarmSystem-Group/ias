package org.eso.ias.cdb.rdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;

import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.PropertyDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.hibernate.Query;
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
 * A set of utilities to use while reading/writing the RDB CDB.
 * <BR>
 * The session factory is a singleton, initialized and built the first
 * time that a process accesses it.
 * <P>
 * <EM>Note</EM>: the process is in charge to release the resource
 * 				  before exiting (or when does not need to access the 
 * 				  RDB anymore) by calling {@link #close()}.
 * 
 * 
 * @author acaproni
 *
 */
public class RdbUtils {
	
	/**
	 * The logger
	 */
	static final Logger logger = LoggerFactory.getLogger(RdbUtils.class);
	
	/**
	 * The SQL script to create the database schema 
	 */
	private static final String createTableSqlScript = "org/eso/ias/rdb/sql/CreateTables.sql";
	
	/**
	 * The SQL script to delete the database schema 
	 */
	private static final String deleteTableSqlScript = "org/eso/ias/rdb/sql/DropTables.sql";
	
	
	/**
	 * The factory to get sessions.
	 * 
	 * According to hibernate API documentation we need
	 * only one instance of the factory to get sessions out of it.
	 */
	private static SessionFactory sessionFactory=null;
    
	private static SessionFactory createSessionFactory() {

		try {
			logger.debug("Bootstrapping hibernate");
			File configFile = new File("hibernate.cfg.xml");
			Configuration configuration = new Configuration();
			configuration.configure(configFile);
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
			return sessionFactory;
		} catch (Throwable ex) {
			logger.error("Initial SessionFactory creation failed.",ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	/**
	 *  Return the hibernate SessionFactory singleton.
	 * 
	 * @return the SessionFactory
	 */
	private static SessionFactory getSessionFactory() {
		if (sessionFactory == null) {
			sessionFactory = RdbUtils.createSessionFactory();
		}
		return sessionFactory;
	}
	
	/**
	 * Get a Session out of the SessionFactory
	 * 
	 * @return a Session
	 */
	public static Session getSession() {
		logger.debug("Returning a new hibernate Session");
		return RdbUtils.getSessionFactory().openSession();
	}
	
	/**
	 * Close the factory releasing the associated resources
	 * 
	 * @see SessionFactory#close()
	 */
	public void close() {
		logger.debug("Closing the hibernate session factory");
		RdbUtils.getSessionFactory().close();
	}
	
	/**
	 * 
	 * @return <code>true</code> if the SessionFacory is closed
	 */
	public static boolean isClosed() {
		return getSessionFactory().isClosed();
	}
	
	/**
	 * Return the reader to get the SQL from.
	 *  
	 * @param resourcePath The path of the resource to read the SQL from
	 * @return
	 */
	private static Reader getSqlReader(String resourcePath) {
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
	private static void runSqlScript(String resourcePath) {
		Objects.requireNonNull(resourcePath, "The resource path can't be null");
		if (resourcePath.isEmpty()) {
			throw new IllegalArgumentException("The resource path can't be empty");
		}
		Reader sqlReader = RdbUtils.getSqlReader(resourcePath);
		SqlRunner sqlRunner = new SqlRunner(sqlReader);
		sqlRunner.runSQLScript(getSession());
	}
	
	public static void dropTables() throws IasCdbException {
		logger.debug("Dropping tables by running the SQL script in "+RdbUtils.deleteTableSqlScript);
		RdbUtils.runSqlScript(RdbUtils.deleteTableSqlScript);
		
		logger.info("Database schema cleared");
	}
	
	public static void createTables() throws IasCdbException {
		logger.debug("Creating database schema by running the provided SQL script "+RdbUtils.createTableSqlScript);
		RdbUtils.runSqlScript(RdbUtils.createTableSqlScript);
		
		logger.info("Database schema created");
	}
	
}
