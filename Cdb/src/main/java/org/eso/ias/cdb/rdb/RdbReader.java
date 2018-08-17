/**
 * 
 */
package org.eso.ias.cdb.rdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.DasuToDeployDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.eso.ias.cdb.pojos.TemplateDao;
import org.eso.ias.cdb.pojos.TransferFunctionDao;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Read CDB configuration from RDB.
 * 
 * @see CdbReader
 * @author acaproni
 *
 */
public class RdbReader implements CdbReader {
	
	/**
	 * Helper object to read and write the RDB
	 */
	private final RdbUtils rdbUtils = RdbUtils.getRdbUtils();

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
	 * Get the IASIO with the given ID
	 * 
	 * @param id The ID of the IASIO to read the configuration
	 * @return The IASIO red from the file
	 * @see org.eso.ias.cdb.CdbReader#getIasio(java.lang.String)
	 */
	@Override
	public Optional<IasioDao> getIasio(String id) throws IasCdbException {
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
	public Collection<IasioDao> getIasiosForAsce(String id) throws IasCdbException {
		Objects.requireNonNull(id, "The ID cant't be null");
		if (id.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty ID");
		}
		Optional<AsceDao> asce = getAsce(id);
		Collection<IasioDao> ret = asce.orElseThrow(() -> new IasCdbException("ASCE ["+id+"] not dound")).getInputs();
		return (ret==null)? new ArrayList<>() : ret;
	}

	/**
	 * Initialize the CDB
	 */
	@Override
	public void init() throws IasCdbException {}
	
	/**
	 * Close the CDB and release the associated resources
	 * @throws IasCdbException
	 */
	@Override
	public void shutdown() throws IasCdbException {
		rdbUtils.close();
	}
}
