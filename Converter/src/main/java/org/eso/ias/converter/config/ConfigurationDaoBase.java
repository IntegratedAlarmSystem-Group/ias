package org.eso.ias.converter.config;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the implementers of the {@link IasioConfigurationDAO}.
 * 
 * @author acaproni
 *
 */
public abstract class ConfigurationDaoBase implements IasioConfigurationDAO {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(ConfigurationDaoBase.class);
	
	/**
	 * <code>true</code> if the DAO has been initialized
	 */
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	
	/**
	 * <code>true</code> if the DAO has been closed
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);
	
	/**
	 * Setup the DAO: the implementer user provider initialization
	 * of the DAO.
	 * 
	 * @throws ConfigurationException The exception returned in case of error initializing
	 */
	protected abstract void setUp() throws ConfigurationException;

	/**
	 * @see IasioConfigurationDAO#initialize()
	 */
	@Override
	public void initialize() throws ConfigurationException {
		logger.info("Initializing..");
		try {
			setUp();
		} catch (Exception e) {
			throw new ConfigurationException("Exception caught initializing the DAO",e);
		}
		initialized.set(true);
		logger.info("Initialized.");
	}
	
	/**
	 * @see IasioConfigurationDAO#isInitialized()
	 */
	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

	/**
	 * Shuts down the DAO: the user provided closing.
	 * 
	 * @throws ConfigurationException in case of error shutting down
	 */
	protected abstract void tearDown() throws ConfigurationException;

	/**
	 * @see IasioConfigurationDAO#close()
	 */
	@Override
	public void close() throws ConfigurationException {
		logger.info("Shutting down...");
		try {
			tearDown();
		} catch (Exception e) {
			throw new ConfigurationException("Exception caught closing the DAO",e);
		}
		closed.set(true);
		logger.info("Shutted down.");
	}

	/**
	 * @see IasioConfigurationDAO#isClosed()
	 */
	@Override
	public boolean isClosed() {
		return closed.get();
	}

}
