package org.eso.ias.cdb.json;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasioDao;

/**
 * Interface to build CDB pojos from JSON files.
 * 
 * @author acaproni
 */
public interface CdbFileReader {
	
	/**
	 * Get the Ias configuration from a file file.
	 * 
	 * @return The ias configuration read from the file 
	 */
	Optional<IasDao> getIas() throws IOException;
	
	/**
	 * Get the IASIOs from the passed file.
	 * 
	 * @return The IASIOs read from the file
	 */
	public Optional<Set<IasioDao>> getIasios() throws IOException;

}
