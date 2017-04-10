/**
 * 
 */
package org.eso.ias.cdb.rdb;

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
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
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
		for (Iterator iterator = iasdaos.iterator(); iterator.hasNext();){
			ret.add((IasDao)iterator.next());
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
	 * @param id The ID of the IASIO to read the congiuration
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
	 * Read the supervisor configuration from the file. 
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
	 * Read the ASCE configuration from the file. 
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
	 * Read the DASU configuration from the file. 
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
		return Optional.ofNullable(dasu);
	}

}
