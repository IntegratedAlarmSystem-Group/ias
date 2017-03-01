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

/**
 * {@link CdbConverter} import/exports JSON file to from
 * the java pojos used by hibernate (i.e. those in 
 * org.eso.ias.cdb.rdb package.
 *  
 * @author acaproni
 */
public class CdbConverter {
	
	/**
	 * The writer to save pojos in JSON files
	 */
	private final JsonWriter writer;
	
	/**
	 * The reader to parse JSON files
	 */
	private final JsonReader reader;
	
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
		CdbFiles cdbFiles = new CdbJsonFiles(p);
		writer = new JsonWriter(cdbFiles);
		reader = new JsonReader(cdbFiles);
		
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
		try {
			writer.writeIas(ias);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Get the IAS from the file CDB
	 * 
	 * @return The IAS form the file CDB
	 */
	public Optional<IasDao> getIas() throws IOException {
		return reader.getIas();
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
		try {
			writer.writeSupervisor(superv);
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
		try {
			writer.writeDasu(dasu);
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
		try {
			writer.writeAsce(asce);
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
		try {
			writer.writeIasio(iasio, true);
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
		try {
			writer.writeIasios(iasios, true);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Get the IAS from the file CDB
	 * 
	 * @return The IAS form the file CDB
	 */
	public Optional<Set<IasioDao>> getIasios() throws IOException{
		return reader.getIasios();
	}
	
	/**
	 * Get the Supervisor from the file CDB
	 * 
	 * @param The id of the Supervisor
	 * @return The Supervisor form the file CDB
	 */
	public Optional<SupervisorDao> getSupervisor(String id) throws IOException{
		return reader.getSupervisor(id);
	}
	
	/**
	 * Get the ASCE from the file CDB
	 * 
	 * @param The id of the ASCE
	 * @return The ASCE form the file CDB
	 */
	public Optional<AsceDao> getAsce(String id) throws IOException{
		return reader.getAsce(id);
	}
	
	/**
	 * Get the DASU from the file CDB
	 * 
	 * @param The id of the DASU
	 * @return The DASU form the file CDB
	 */
	public Optional<DasuDao> getDasu(String id) throws IOException{
		return reader.getDasu(id);
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
    	asce.addInput(i1,true);
    	asce.addInput(i3,true);
    	
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
    	
    	System.out.println("================================== Now reading ==================================");
    	
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
    	
    	System.out.println("Reading ASCE configuration from JSON file");
    	Optional<AsceDao> a = conv.getAsce("ASCE-ID");
    	if (a.isPresent()) {
    		System.out.println("ASCE found!");
    		System.out.println(a.get());
    	} else {
    		System.out.println("Got an empy ASCE");
    	}
    	
    	System.out.println("Reading Supervisor configuration from JSON file");
    	Optional<SupervisorDao> sup = conv.getSupervisor("SupervID");
    	if (sup.isPresent()) {
    		System.out.println("SUPERVISOR found!");
    		System.out.println(sup.get());
    	} else {
    		System.out.println("Got an empy SUPERVISOR");
    	}
    	
    	System.out.println("Reading DASU configuration from JSON file");
    	Optional<DasuDao> optDasu = conv.getDasu("TheIdOfTheDASU");
    	if (optDasu.isPresent()) {
    		System.out.println("DASU found!");
    		System.out.println(optDasu.get());
    	} else {
    		System.out.println("Got an empy DASU");
    	}
	}
}
