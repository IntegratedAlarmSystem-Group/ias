/**
 * 
 */
package org.eso.ias.cdb.rdb;

import java.io.IOException;
import java.util.Set;

import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

/**
 * Writes IAS configuration into RDB.
 * 
 * @see CdbWriter
 * @author acaproni
 *
 */
public class RdbWriter implements CdbWriter {

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbWriter#writeIas(org.eso.ias.cdb.pojos.IasDao)
	 */
	@Override
	public void writeIas(IasDao ias) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbWriter#writeSupervisor(org.eso.ias.cdb.pojos.SupervisorDao)
	 */
	@Override
	public void writeSupervisor(SupervisorDao superv) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbWriter#writeDasu(org.eso.ias.cdb.pojos.DasuDao)
	 */
	@Override
	public void writeDasu(DasuDao dasu) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbWriter#writeAsce(org.eso.ias.cdb.pojos.AsceDao)
	 */
	@Override
	public void writeAsce(AsceDao asce) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbWriter#writeIasio(org.eso.ias.cdb.pojos.IasioDao, boolean)
	 */
	@Override
	public void writeIasio(IasioDao iasio, boolean append) throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbWriter#writeIasios(java.util.Set, boolean)
	 */
	@Override
	public void writeIasios(Set<IasioDao> iasios, boolean append) throws IOException {
		// TODO Auto-generated method stub

	}

}
