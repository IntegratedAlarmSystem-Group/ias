/**
 * 
 */
package org.eso.ias.cdb.rdb;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read CDB configuration from RDB.
 * 
 * @see CdbReader
 * @author acaproni
 *
 */
public class RdbReader implements CdbReader {

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(RdbReader.class);
	
	/**
	 * Helper object to read and write the RDB
	 */
	private final RdbUtils rdbUtils = RdbUtils.getRdbUtils();

	/**
	 * Signal if the reader has been initialized
	 */
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * Signal if the reader has been closed
	 */
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/**
	 * Get the Ias configuration from a file.
	 * <P>
	 * In the actual implementation there must be one and only one IAS 
	 * in the RDB so this method fails if gets more then one IAS from the
	 * database.
	 * <BR>TODO: there is a <A href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/2">ticket</A> to change
	 * the configuration in order to have more then one IAS with its own unique ID.  
	 * 
	 * @return The ias configuration read from the file 
	 * @see org.eso.ias.cdb.CdbReader#getIas()
	 */
	@Override
	public Optional<IasDao> getIas() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		
		List<IasDao> iasdaos = (List<IasDao>)s.createQuery("FROM IasDao").list();
		Set<IasDao> ret = new HashSet<>();
		for (Iterator<IasDao> iterator = iasdaos.iterator(); iterator.hasNext();){
			ret.add(iterator.next());
		}
		t.commit();
		// Check if the query returned too many IAS
		if (ret.size()>1) {
			throw new IllegalStateException("Too many IAS in the RDB");
		}
		// Return an option with the only one returned IAS
		// or empty if the query returned none
		return ret.stream().findAny();
	}

	/**
	 * Get the IASIOs.
	 * 
	 * @return The IASIOs red from the file
	 * @see org.eso.ias.cdb.CdbReader#getIasios()
	 */
	@Override
	public Optional<Set<IasioDao>> getIasios() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		List iasios= s.createCriteria(IasioDao.class).list();
		Set<IasioDao> ret = new HashSet<>();
		for (Iterator iterator = iasios.iterator(); iterator.hasNext();){
			ret.add((IasioDao)iterator.next());
		}
		t.commit();
		return Optional.of(ret);
	}

	/**
	 * Get the templates.
	 *
	 * @return The templates red from the file
	 * @see org.eso.ias.cdb.CdbReader#getIasios()
	 */
	@Override
	public Optional<Set<TemplateDao>> getTemplates() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		List templates = s.createCriteria(TemplateDao.class).list();
		Set<TemplateDao> ret = new HashSet<>();
		for (Iterator iterator = templates.iterator(); iterator.hasNext();){
			ret.add((TemplateDao)iterator.next());
		}
		t.commit();
		return Optional.of(ret);
	}

    /**
     * Get the transfer functions.
     *
     * @return The transfer functions read from the file
     * @see org.eso.ias.cdb.CdbReader#getIasios()
     */
    @Override
    public Optional<Set<TransferFunctionDao>> getTransferFunctions() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

        Session s=rdbUtils.getSession();
        Transaction t =s.beginTransaction();
        List tfs = s.createCriteria(TransferFunctionDao.class).list();
        Set<TransferFunctionDao> ret = new HashSet<>();
        for (Iterator iterator = tfs.iterator(); iterator.hasNext();){
            ret.add((TransferFunctionDao)iterator.next());
        }
        t.commit();
        return Optional.of(ret);
    }

	/**
	 * Get the IASIO with the given ID
	 * 
	 * @param id The ID of the IASIO to read the configuration
	 * @return The IASIO red from the file
	 * @see org.eso.ias.cdb.CdbReader#getIasio(java.lang.String)
	 */
	@Override
	public Optional<IasioDao> getIasio(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		IasioDao iasio = s.get(IasioDao.class,id);
		t.commit();
		return Optional.ofNullable(iasio);
	}

	/**
	 * Read the supervisor configuration from the CDB. 
	 * 
	 * @param id The not null nor empty supervisor identifier
	 * @see org.eso.ias.cdb.CdbReader#getSupervisor(java.lang.String)
	 */
	@Override
	public Optional<SupervisorDao> getSupervisor(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		SupervisorDao superv = s.get(SupervisorDao.class,id);
		t.commit();
		return Optional.ofNullable(superv);
	}

	/**
	 * Read the ASCE configuration from the CDB. 
	 * 
	 * @param id The not null nor empty ASCE identifier
	 * @see org.eso.ias.cdb.CdbReader#getAsce(java.lang.String)
	 */
	@Override
	public Optional<AsceDao> getAsce(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		AsceDao asce = s.get(AsceDao.class,id);
		t.commit();
		return Optional.ofNullable(asce);
	}
	
	/**
	 * Read the transfer function configuration from the CDB. 
	 * 
	 * @param tf_id The not <code>null</code> nor empty transfer function identifier
	 * @return The transfer function red from the CDB
	 * @see org.eso.ias.cdb.CdbReader#getTransferFunction(String)
	 */
	@Override
	public Optional<TransferFunctionDao> getTransferFunction(String tf_id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(tf_id, "The ID of the TF cant't be null");
		if (tf_id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty TF ID");
		}
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		TransferFunctionDao tf = s.get(TransferFunctionDao.class,tf_id);
		t.commit();
		return Optional.ofNullable(tf);
	}
	
	/**
	 * Read the template configuration from the CDB. 
	 * 
	 * @param template_id The not <code>null</code> nor empty identifier of the template
	 * @return The template read from the CDB
	 * @throws IasCdbException in case of error reading from the CDB
	 */
	@Override
	public Optional<TemplateDao> getTemplate(String template_id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		if (template_id==null || template_id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty or null template ID");
		}
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		TemplateDao template = s.get(TemplateDao.class,template_id);
		t.commit();
		return Optional.ofNullable(template);
	}

	/**
	 * Read the DASU configuration from the CDB. 
	 * 
	 * @param id The not null nor empty DASU identifier
	 * @see org.eso.ias.cdb.CdbReader#getDasu(java.lang.String)
	 */
	@Override
	public Optional<DasuDao> getDasu(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		DasuDao dasu = s.get(DasuDao.class,id);
		t.commit();
		
		Optional<DasuDao> ret = Optional.ofNullable(dasu);
		
		// If the DASU is templated then the ID of the templates of ASCEs
		// must be equal to that of the DASU
		if (ret.isPresent()) {
			Set<AsceDao> asces = ret.get().getAsces();
			String templateOfDasu = ret.get().getTemplateId(); 
			if (templateOfDasu==null) {
				// No template in the DASU: ASCEs must have no template
				if (!asces.stream().allMatch(asce -> asce.getTemplateId()==null)) {
					throw new IasCdbException("Template mismatch between DASU ["+id+"] and its ASCEs");
				}
			} else {
				if (!asces.stream().allMatch(asce -> asce.getTemplateId().equals(templateOfDasu))) {
					throw new IasCdbException("Template mismatch between DASU ["+id+"] and its ASCEs");
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Return the DASUs belonging to the given Supervisor.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the supervisor
	 * @return A set of DASUs running in the supervisor with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         supervisor with the give identifier does not exist
	 */
	@Override
	public Set<DasuToDeployDao> getDasusToDeployInSupervisor(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Optional<SupervisorDao> superv = getSupervisor(id);
		Set<DasuToDeployDao> ret = superv.orElseThrow(() -> new IasCdbException("Supervisor ["+id+"] not dound")).getDasusToDeploy();
		return (ret==null)? new HashSet<>() : ret;
	}
	
	/**
	 * Return the ASCEs belonging to the given DASU.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the supervisor
	 * @return A set of DASUs running in the DASU with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         DASU with the give identifier does not exist
	 */
	@Override
	public Set<AsceDao> getAscesForDasu(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Optional<DasuDao> dasu = getDasu(id);
		Set<AsceDao> ret = dasu.orElseThrow(() -> new IasCdbException("DASU ["+id+"] not dound")).getAsces();
		return (ret==null)? new HashSet<>() : ret;
	}
	
	/**
	 * Return the IASIOs in input to the given ASCE.
	 * 
	 * @param id The not <code>null</code> nor empty identifier of the ASCE
	 * @return A set of IASIOs running in the ASCE with the passed id
	 * @throws IasCdbException in case of error reading CDB or if the 
	 *                         ASCE with the give identifier does not exist
	 */
	@Override
	public Collection<IasioDao> getIasiosForAsce(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty ID");
		}
		Optional<AsceDao> asce = getAsce(id);
		Collection<IasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not dound")).getInputs();
		return (ret==null)? new ArrayList<>() : ret;
	}

    /**
     * Get the IDs of the Supervisors.
     *
     * This method is useful to deploy the supervisors
     *
     * @return The the IDs of the supervisors read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the supervisors
     */
    @Override
    public Optional<Set<String>> getSupervisorIds() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		List supervisors = s.createCriteria(SupervisorDao.class).list();
		Set<String> ret = new HashSet<>();
		for (Iterator iterator = supervisors.iterator(); iterator.hasNext();) {
			ret.add(((SupervisorDao)iterator.next()).getId());
		}
		t.commit();
		return Optional.of(ret);
	}

    /**
     * Get the IDs of the DASUs.
     *
     * @return The IDs of the DASUs read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the DASUs
     */
    @Override
    public Optional<Set<String>> getDasuIds() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

        Session s=rdbUtils.getSession();
        Transaction t =s.beginTransaction();
        List supervisors = s.createCriteria(DasuDao.class).list();
        Set<String> ret = new HashSet<>();
        for (Iterator iterator = supervisors.iterator(); iterator.hasNext();) {
            ret.add(((DasuDao)iterator.next()).getId());
        }
        t.commit();
        return Optional.of(ret);
    }

    /**
     * Get the IDs of the ASCEs.
     *
     * @return The IDs of the ASCEs read from the CDB
     * @throws IasCdbException In case of error getting the IDs of the ASCEs
     */
    @Override
    public Optional<Set<String>> getAsceIds() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

        Session s=rdbUtils.getSession();
        Transaction t =s.beginTransaction();
        List supervisors = s.createCriteria(AsceDao.class).list();
        Set<String> ret = new HashSet<>();
        for (Iterator iterator = supervisors.iterator(); iterator.hasNext();) {
            ret.add(((AsceDao)iterator.next()).getId());
        }
        t.commit();
        return Optional.of(ret);
    }

	/**
	 * Return the templated IASIOs in input to the given ASCE.
	 *
	 * These inputs are the one generated by a different template than
	 * that of the ASCE
	 * (@see <A href="https://github.com/IntegratedAlarmSystem-Group/ias/issues/124">#!@$</A>)
	 *
	 * @param id The not <code>null</code> nor empty identifier of the ASCE
	 * @return A set of template instance of IASIOs in input to the ASCE
	 * @throws IasCdbException in case of error reading CDB or if the
	 *                         ASCE with the give identifier does not exist
	 *
	 */
	@Override
	public Collection<TemplateInstanceIasioDao> getTemplateInstancesIasiosForAsce(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		Objects.requireNonNull(id,"Invalid null identifier");
		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid null orempty ID");
		}
        Optional<AsceDao> asce = getAsce(id);
        Collection<TemplateInstanceIasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not dound")).getTemplatedInstanceInputs();
        return (ret==null)? new ArrayList<>() : ret;
	}

	/**
	 * Get the configuraton of the client with the passed identifier.
	 *
	 * The configuration is passed as a string whose format depends
	 * on the client implementation.
	 *
	 * @param id The not null nor empty ID of the IAS client
	 * @return The configuration of the client
	 * @throws IasCdbException In case of error getting the configuration of the client
	 */
	@Override
	public Optional<ClientConfigDao> getClientConfig(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty or null ID of config");
		}
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		ClientConfigDao clientConf = s.get(ClientConfigDao.class,id);
		t.commit();

		return Optional.ofNullable(clientConf);
	}

	/**
	 * Get the configuration of the plugin with the passed identifier.
	 * <p>
	 * The configuration of the plugin can be read from a file or from the CDB.
	 * In both cases, the configuration is returned as #PluginConfigDao.
	 * This m,ethod returns the configuration from the CDB; reading from file is
	 * not implemented here.
	 *
	 * @param id The not null nor empty ID of the IAS plugin
	 * @return The configuration of the plugin
	 * @throws IasCdbException In case of error getting the configuration of the plugin
	 */
	@Override
	public Optional<PluginConfigDao> getPlugin(String id) throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("The reader is shut down");
		}
		if (!initialized.get()) {
			throw new IasCdbException("The reader is not initialized");
		}

		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty or null ID of plugin");
		}
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		PluginConfigDao pluginConf = s.get(PluginConfigDao.class,id);
		t.commit();
		return Optional.ofNullable(pluginConf);
	}

	/**
	 * Initialize the CDB
	 */
	@Override
	public void init() throws IasCdbException {
		if (closed.get()) {
			throw new IasCdbException("Cannot initialize: already closed");
		}
		if(!initialized.get()) {
			logger.debug("Initialized");
			initialized.set(true);
		} else {
			logger.warn("Already initialized: skipping initialization");
		}
	}
	
	/**
	 * Close the CDB and release the associated resources
	 * @throws IasCdbException
	 */
	@Override
	public void shutdown() throws IasCdbException {
		if (!initialized.get()) {
			throw new IasCdbException("Cannot shutdown a reader that has not been initialized");
		}
		if (!closed.get()) {
			rdbUtils.close();
			logger.debug("Closed");
			closed.set(true);
		} else {
			logger.warn("Already closed!");
		}

	}
}
