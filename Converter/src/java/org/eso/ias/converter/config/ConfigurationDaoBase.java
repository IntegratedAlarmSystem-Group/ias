package org.eso.ias.converter.config;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for the implementers of the {@link IasioConfigurationDAO}.
 * 
 * @author acaproni
 *
 */
public abstract class ConfigurationDaoBase implements IasioConfigurationDAO {
	
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
	 */
	protected abstract void setUp();

	@Override
	public void initialize() {
		setUp();
		initialized.set(true);

	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}

	@Override
	public MonitorPointConfiguration getConfiguration(String mpId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Shuts down the DAO: the user provided closing.
	 */
	protected abstract void tearDown();

	@Override
	public void close() {
		tearDown();
		closed.set(true);

	}

	@Override
	public boolean ClosedInitialized() {
		return closed.get();
	}

}
