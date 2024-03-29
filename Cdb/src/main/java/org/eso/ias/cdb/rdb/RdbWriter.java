/**
 * 
 */
package org.eso.ias.cdb.rdb;

import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.pojos.*;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Writes IAS configuration into RDB.
 * 
 * @see CdbWriter
 * @author acaproni
 *
 */
@Deprecated
public class RdbWriter implements CdbWriter {
	
	/**
	 * Helper object to read and write the RDB
	 */
	private final RdbUtils rdbUtils = RdbUtils.getRdbUtils();

	/**
	 * Write the ias in the CDB.
	 * 
	 * @param ias The IAS configuration to write in the file
	 * @see org.eso.ias.cdb.CdbWriter#writeIas(org.eso.ias.cdb.pojos.IasDao)
	 */
	@Override
	public void writeIas(IasDao ias) throws IasCdbException {
		Objects.requireNonNull(ias, "The DAO object to persist can't be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		s.merge(ias);
		t.commit();
		s.flush();
		s.close();
	}
	
	/**
	 * Write the Supervisor in the CDB.
	 * 
	 * @param superv The Supervisor configuration to write in the file
	 * @see org.eso.ias.cdb.CdbWriter#writeSupervisor(org.eso.ias.cdb.pojos.SupervisorDao)
	 */
	@Override
	public void writeSupervisor(SupervisorDao superv) throws IasCdbException {
		Objects.requireNonNull(superv, "The DAO object to persist can't be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		s.merge(superv);
		t.commit();
		s.flush();
		s.close();
	}

	/**
	 *  Write the DASU in the CDB.
	 * 
	 * @param dasu The DASU configuration to write in the file
	 * @see org.eso.ias.cdb.CdbWriter#writeDasu(org.eso.ias.cdb.pojos.DasuDao)
	 */
	@Override
	public void writeDasu(DasuDao dasu) throws IasCdbException {
		Objects.requireNonNull(dasu, "The DAO object to persist can't be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		s.merge(dasu);
		t.commit();
		s.flush();
	}

	/**
	 * Write the ASCE in the CDB.
	 * 
	 * @param asce The ASCE configuration to write in the file
	 * @see org.eso.ias.cdb.CdbWriter#writeAsce(org.eso.ias.cdb.pojos.AsceDao)
	 */
	@Override
	public void writeAsce(AsceDao asce) throws IasCdbException {
		Objects.requireNonNull(asce, "The DAO object to persist can't be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		s.merge(asce);
		t.commit();
		s.flush();
	}

	/**
	 * Write the IASIO in the CDB.
	 * <P>
	 * This method delegates to {@link #writeIasios(Set, boolean)}
	 * 
	 * @param iasio The IASIO configuration to write in the file
	 * @param append: if <code>true</code> the passed iasio is appended to the file
	 *                otherwise a new file is created
	 * @see #writeIasios(Set, boolean)
	 */
	@Override
	public void writeIasio(IasioDao iasio, boolean append) throws IasCdbException {
		Objects.requireNonNull(iasio, "The DAO object to persist can't be null");
		Set<IasioDao> set = new HashSet<IasioDao>(Arrays.asList(iasio));
		writeIasios(set,append);
	}

	/**
	 * Write the IASIOs in the CDB.
	 * 
	 * @param iasios The IASIOs to write in the file
	 * @param append: if <code>true</code> the passed iasios are appended to the file
	 *                otherwise a new file is created
	 * @see CdbWriter#writeIasios(Set, boolean)
	 */
	@Override
	public void writeIasios(Set<IasioDao> iasios, boolean append) throws IasCdbException {
		Objects.requireNonNull(iasios, "The DAO object to persist can't be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		iasios.stream().forEach(io -> s.merge(io));
		t.commit();
		s.flush();
	}

	/**
	 * Write the configuration of the passed plugin
	 *
	 * @param pluginConfigDao the configuraton of the plugin
	 * @throws IasCdbException In case of error writing the configuration
	 */
	@Override
	public void writePluginConfig(PluginConfigDao pluginConfigDao) throws IasCdbException {
		Objects.requireNonNull(pluginConfigDao, "The plugin DAO object to persist can't be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		for (ValueDao v: pluginConfigDao.getValues()) {
			s.merge(v);
		}
		s.merge(pluginConfigDao);
		t.commit();
		s.flush();
	}

	/**
	 * Write the transfer function in the CDB
	 * 
	 * @param tf The transfer function to write in the CDB
	 * @see CdbWriter#writeTransferFunction(TransferFunctionDao)
	 */
	@Override
	public void writeTransferFunction(TransferFunctionDao tf) throws IasCdbException {
		Objects.requireNonNull(tf, "The TF object to persist can't be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		s.merge(tf);
		t.commit();
		s.flush();
	}
	
	/**
	 *  Persist the passed template to the CDB
	 *  
	 *  @param template The template Dao to write in the CDB
	 *  @throws IasCdbException In case of error writing the template
	 */
	@Override
	public void writeTemplate(TemplateDao template) throws IasCdbException {
		Objects.requireNonNull(template, "The template object to persist can't be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		s.merge(template);
		t.commit();
		s.flush();
	}

	/**
	 * Write the configuration of the client with the passed identifier
	 *
	 * @param clientConfig the configuraton of the client
	 * @throws IasCdbException In case of error writing the configuration
	 */
	@Override
	public void writeClientConfig(ClientConfigDao clientConfig) throws IasCdbException {
		Objects.requireNonNull(clientConfig,"The config DAO cannot be null");
		Session s=rdbUtils.getSession();
		Transaction t =s.beginTransaction();
		s.merge(clientConfig);
		t.commit();
		s.flush();
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
