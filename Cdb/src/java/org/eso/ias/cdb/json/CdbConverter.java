package org.eso.ias.cdb.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.pojos.PropertyDao;
import org.eso.ias.cdb.pojos.SupervisorDao;
import org.hibernate.Transaction;

/**
 * {@link CdbConverter} import/exports JSON file to from
 * the java pojos used by hibernate (i.e. those in 
 * org.eso.ias.cdb.rdb package.
 *  
 * @author acaproni
 */
public class CdbConverter {
	
	/**
	 * The parent folder of the CDB 
	 * i.e. the folder where we expect to find CDB, CDB/DASU etc.
	 * 
	 * @see CdbFolders
	 */
	private final Path jsonCdbParentFolder;
	
	/**
	 * The writer to save pojos in JSON files
	 */
	private final JsonWriter writer = new JsonWriter();
	
	/**
	 * The reader to parse JSON files
	 */
	private final JsonReader reader = new JsonReader();
	
	/**
	 * Constructor
	 * 
	 * @param jsonCdbFolder The CDB folder in the file system
	 * @throws IOException If the passed CDB is invalid
	 */
	public  CdbConverter(String jsonCdbFolder) throws IOException {
		if (jsonCdbFolder==null || jsonCdbFolder.isEmpty()) {
			throw new NullPointerException("CDB parent folder can't be null nor empty");
		}
		Path p =   FileSystems.getDefault().getPath(jsonCdbFolder);
		File f = p.toFile();
		if (!f.exists() || !f.isDirectory() || !f.canWrite()) {
			throw new IOException("Invaflid CDB parent folder "+f.getAbsolutePath());
		}
		this.jsonCdbParentFolder=p;
	}
	
	/**
	 * Serialize the ias in the JSON file.
	 * 
	 * @param ias The IAS configuration to write in the file
	 * @throws IOException in case of errors creating CDB folders or wiring JSON files.
	 */
	public void serializeIas(IasDao ias) throws IOException {
		if (ias==null) {
			throw new NullPointerException("IAS pojo cant't be null");
		}
		Path folder = CdbFolders.getSubfolder(jsonCdbParentFolder, CdbFolders.ROOT, true);
		File outF = folder.resolve("ias.json").toFile();
		try {
			writer.writeIas(ias, outF);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Get the IAS from the file CDB
	 * 
	 * @return The IAS form the file CDB
	 */
	public Optional<IasDao> getIas() {
		File inF;
		try {
			Path folder = CdbFolders.getSubfolder(jsonCdbParentFolder, CdbFolders.ROOT, true);
			inF = folder.resolve("ias.json").toFile();
		} catch (Throwable t) {
			System.out.println("Error getting the IAS JSON file: ");
			return Optional.empty();
		}
		return reader.getIas(inF);
	}
	
	/**
	 * Serialize the Supervisor in the JSON file.
	 * 
	 * @param superv The Supervisor configuration to write in the file
	 * @throws IOException in case of errors creating CDB folders or wiring JSON files.
	 */
	public void serializeSupervisor(SupervisorDao superv) throws IOException {
		if (superv==null) {
			throw new NullPointerException("Supervisor pojo cant't be null");
		}
		String supervisorID=superv.getId();
		if (supervisorID==null || supervisorID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty or null supervisor ID");
		}
		Path folder = CdbFolders.getSubfolder(jsonCdbParentFolder, CdbFolders.SUPERVISOR, true);
		File outF = folder.resolve(supervisorID+".json").toFile();
		try {
			writer.writeSupervisor(superv, outF);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Serialize the DASU in the JSON file.
	 * 
	 * @param dasu The DASU configuration to write in the file
	 * @throws IOException in case of errors creating CDB folders or wiring JSON files.
	 */
	public void serializeDasu(DasuDao dasu) throws IOException {
		if (dasu==null) {
			throw new NullPointerException("DASU pojo cant't be null");
		}
		String dasuID=dasu.getId();
		if (dasuID==null || dasuID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty or null DASU ID");
		}
		Path folder = CdbFolders.getSubfolder(jsonCdbParentFolder, CdbFolders.DASU, true);
		File outF = folder.resolve(dasuID+".json").toFile();
		try {
			writer.writeDasu(dasu, outF);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Serialize the ASCE in the JSON file.
	 * 
	 * @param asce The ASCE configuration to write in the file
	 * @throws IOException in case of errors creating CDB folders or wiring JSON files.
	 */
	public void serializeAsce(AsceDao asce) throws IOException {
		if (asce==null) {
			throw new NullPointerException("ASCE pojo cant't be null");
		}
		String asceID=asce.getId();
		if (asceID==null || asceID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty or null ASCE ID");
		}
		Path folder = CdbFolders.getSubfolder(jsonCdbParentFolder, CdbFolders.ASCE, true);
		File outF = folder.resolve(asceID+".json").toFile();
		try {
			writer.writeAsce(asce, outF);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Serialize the IASIO in the JSON file.
	 * 
	 * @param iasio The IASIO configuration to write in the file
	 *  @throws IOException in case of errors creating CDB folders or wiring JSON files.
	 */
	public void serializeIasio(IasioDao iasio) throws IOException {
		Objects.requireNonNull(iasio,"The IASIO pojo cant't be null");
		String iasioID=iasio.getId();
		if (iasioID==null || iasioID.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty or null IASIO ID");
		}
		Path folder = CdbFolders.getSubfolder(jsonCdbParentFolder, CdbFolders.IASIO, true);
		File outF = folder.resolve("iasio.json").toFile();
		try {
			writer.writeIasio(iasio, outF,true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Serialize the IASIOs in the JSON file.
	 * 
	 * @param iasios The IASIOs to write in the file
	 * @throws IOException in case of errors creating CDB folders or wiring JSON files.
	 */
	public void serializeIasios(Set<IasioDao> iasios) throws IOException {
		Objects.requireNonNull(iasios,"The set of IASIO pojos cant't be null");
		Path folder = CdbFolders.getSubfolder(jsonCdbParentFolder, CdbFolders.IASIO, true);
		File outF = folder.resolve("iasio.json").toFile();
		try {
			writer.writeIasios(iasios, outF,true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Get the IAS from the file CDB
	 * 
	 * @return The IAS form the file CDB
	 */
	public Optional<Set<IasioDao>> getIasios() {
		File inF;
		try {
			Path folder = CdbFolders.getSubfolder(jsonCdbParentFolder, CdbFolders.IASIO, true);
			inF = folder.resolve("iasio.json").toFile();
		} catch (Throwable t) {
			System.out.println("Error getting the IASIOs JSON file: ");
			return Optional.empty();
		}
		return reader.getIasios(inF);
	}
	
	
	
	public static void main(String[] args) throws Exception {
		System.out.println("Writing IAS");
		CdbConverter conv = new CdbConverter(args[0]);
		IasDao ias = new IasDao();
		ias.setLogLevel(LogLevelDao.INFO);
		Set<PropertyDao> props = ias.getProps();
		PropertyDao p = new PropertyDao();
		p.setName("P1");
		p.setValue("Value1");
		PropertyDao p2 = new PropertyDao();
		p2.setName("P2");
		p2.setValue("Value2");
		props.add(p);
		props.add(p2);
		conv.serializeIas(ias);
		
		IasioDao i1 = new IasioDao();
		i1.setId("CONTROL/DA01/WVR/AMB_TEMP");
		i1.setRefreshRate(1000);
		i1.setShortDesc("The short description of this MP");
		i1.setIasType(IasTypeDao.ALARM);
		
		IasioDao i2 = new IasioDao();
		i2.setId("WTANK3/LEVEL");
		i2.setRefreshRate(1500);
		i2.setShortDesc("Remaining liters in water tank 3");
		i2.setIasType(IasTypeDao.INT);
		
		IasioDao i3 = new IasioDao();
		i3.setId("WTANK2/LEVEL");
		i3.setRefreshRate(1500);
		i3.setIasType(IasTypeDao.INT);
		
		System.out.println("Writing a set of IASIOs");
		Set<IasioDao> iasios = new HashSet<>();
		iasios.add(i1);
		iasios.add(i2);
		iasios.add(i3);
    	conv.serializeIasios(iasios);
    	
    	PropertyDao asce_p1 = new PropertyDao();
    	asce_p1.setName("ASCE prop1 Name");
    	asce_p1.setValue("ASCE prop1 Value");
    	PropertyDao asce_p2 = new PropertyDao();
    	asce_p2.setName("ASCE prop2 Name");
    	asce_p2.setValue("ASCE prop2 Value");
    	
    	AsceDao asce = new AsceDao();
    	asce.setId("ASCE-ID");
    	asce.setOutput(i2);
    	asce.setTfClass("alma.acs.eso.org.tf.Multiplicity");
    	asce.getProps().add(asce_p1);
    	asce.getProps().add(asce_p2);
    	asce.getInputs().add(i1);
    	asce.getInputs().add(i3);
    	
    	DasuDao dasu = new DasuDao();
    	dasu.setId("TheIdOfTheDASU");
    	dasu.setLogLevel(LogLevelDao.FATAL);
    	dasu.addAsce(asce);
    	
    	SupervisorDao superv = new SupervisorDao();
    	superv.setId("SupervID");
    	superv.setHostName("iasdevel.hq.eso.org");
    	superv.addDasu(dasu);
    	superv.setLogLevel(LogLevelDao.ALL);
    	
    	System.out.println("Writing Supervisor with its DASU and ASCE");
    	conv.serializeSupervisor(superv);
    	conv.serializeDasu(dasu);
    	conv.serializeAsce(asce);
    	System.out.println("Replacing "+i1);
    	i1.setRefreshRate(5000);
    	conv.serializeIasio(i1);
    	
    	System.out.println("Reading IAS configuration from JSON file");
    	Optional<IasDao> iasFromFile = conv.getIas();
    	if (iasFromFile.isPresent()) {
    		System.out.println("IAS written: "+ias);
    		System.out.println("IAS read:    "+iasFromFile.get());
    		System.out.println("Are the IAS equal? "+ias.equals(iasFromFile.get()));
    	} else System.out.println("IAS NOT read");
    	
    	System.out.println("Reading IASIO configuration from JSON file");
    	Optional<Set<IasioDao>> iasiosFromFile = conv.getIasios();
    	if (iasiosFromFile.isPresent()) {
    		System.out.println("Size of set read: "+iasiosFromFile.get().size());
    		for (IasioDao iasio: iasiosFromFile.get()) {
    			System.out.println(iasio);
    			System.out.println("Are the IASIOs equal? "+iasio.equals(i1));
    		}
    	} else System.out.println("IASIOs NOT read");
	}
}
