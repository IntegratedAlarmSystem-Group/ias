/**
 * 
 */
package org.eso.ias.cdb.rdb;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

/**
 * Read CDB configuration from RDB.
 * 
 * @see CdbReader
 * @author acaproni
 *
 */
public class RdbReader implements CdbReader {

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbReader#getIas()
	 */
	@Override
	public Optional<IasDao> getIas() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbReader#getIasios()
	 */
	@Override
	public Optional<Set<IasioDao>> getIasios() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbReader#getIasio(java.lang.String)
	 */
	@Override
	public Optional<IasioDao> getIasio(String id) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbReader#getSupervisor(java.lang.String)
	 */
	@Override
	public Optional<SupervisorDao> getSupervisor(String id) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbReader#getAsce(java.lang.String)
	 */
	@Override
	public Optional<AsceDao> getAsce(String id) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.cdb.CdbReader#getDasu(java.lang.String)
	 */
	@Override
	public Optional<DasuDao> getDasu(String id) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
