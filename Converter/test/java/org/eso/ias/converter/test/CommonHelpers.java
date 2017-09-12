package org.eso.ias.converter.test;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;

/**
 * A set of commodities for devolping tests for the converter.
 * 
 * @author acaproni
 *
 */
public class CommonHelpers {
	
	/**
	 * The prefix of the IDs of the IASIOs written in the config file
	 */
	public static final String IasioIdPrefix="IoID-";
	
	/**
	 * Create a Iasio ID from the given index
	 * 
	 * @param n The index
	 * @return The ID
	 */
	public static final String buildIasId(int n) {
		return IasioIdPrefix+n;
	}
	
	/**
	 * Create a IasTypeDao from the given index
	 * 
	 * @param n The index
	 * @return The IasTypeDao
	 */
	public static IasTypeDao buildIasType(int n) {
		return IasTypeDao.values()[n%IasTypeDao.values().length];
	}
	
	/**
	 * Populate the CDB with the passed number of IASIO
	 * 
	 * @param numOfIasio the number of IASIOs to write in the configurations
	 * @param cdbFiles The folder struct of the CDB
	 * @throws Exception
	 */
	public static void populateCDB(int numOfIasios,CdbFiles cdbFiles) throws Exception {
		Objects.requireNonNull(cdbFiles);
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		populateCDB(numOfIasios, new JsonWriter(cdbFiles));
	}
	
	/**
	 * Populate the CDB with the passed number of IASIO
	 * 
	 * @param numOfIasio the number of IASIOs to write in the configurations
	 * @param cdbWriter The writer of the CDB
	 * @throws Exception
	 */
	public static void populateCDB(int numOfIasios,CdbWriter cdbWriter) throws Exception {
		Objects.requireNonNull(cdbWriter);
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		Set<IasioDao> iasios = new HashSet<>(numOfIasios);
		for (int t=0; t<numOfIasios; t++) {
			IasTypeDao iasType = buildIasType(t);
			IasioDao iasio = new IasioDao(IasioIdPrefix+t, "IASIO description", 1500, iasType);
			iasios.add(iasio);
		}
		cdbWriter.writeIasios(iasios, false);
	}

	/**
	 * Constructor
	 */
	private CommonHelpers() {	}

}
