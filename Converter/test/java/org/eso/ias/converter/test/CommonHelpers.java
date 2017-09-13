package org.eso.ias.converter.test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eso.ias.cdb.CdbWriter;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.JsonWriter;
import org.eso.ias.cdb.pojos.IasTypeDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.plugin.AlarmSample;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;

/**
 * A set of commodities for devolping tests for the converter.
 * 
 * @author acaproni
 *
 */
public class CommonHelpers {
	
	/**
	 * The IASIODAO is the static view of
	 * a monitor point value or alarm in the 
	 * configuration database.
	 * <BR>At run time a {@link MonitorPointData} is produced 
	 * by the plugin and sent to the converter.
	 * <P>
	 * This class build a {@link MonitorPointData} associating the 
	 * proper value.
	 * 
	 * @author acaproni
	 *
	 */
	public static class MonitorPointsBuilderHelper {
		public final IasioDao iasioDao;
		public final Object value;
		
		/**
		 * Helper class to convert the IasioDao in a 
		 * MonitorPointData
		 * 
		 * @param iasioDao The iasio
		 * @param value The vlaue to associate
		 */
		public MonitorPointsBuilderHelper(IasioDao iasioDao,Object value) {
			Objects.requireNonNull(iasioDao);
			Objects.requireNonNull(value);
			this.iasioDao=iasioDao;
			this.value=value;
		}
	}
	
	/**
	 * The parent folder of the CDB is the actual folder
	 */
	public static final Path cdbParentPath =  FileSystems.getDefault().getPath(".");
	
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
	 * Get the index of a ID created by {@link #buildIasId(int)}
	 * 
	 * @param id The identifier
	 * @return The index of the identifier
	 */
	public static int getIndexFromId(String id) {
		Objects.requireNonNull(id);
		if (id.indexOf('-')==-1) {
			throw new IllegalArgumentException("Invalid identifier format ["+id+"]");
		}
		String[] parts = id.split("-");
		if (parts.length!=2) {
			throw new IllegalArgumentException("Invalid identifier format ["+id+"]");
		}
		return Integer.valueOf(parts[1]);
	}
	
	/**
	 * Get the type associated to the passed identifier
	 * 
	 * @param if The identifier of the IASIO
	 * @return the type of the identifier
	 */
	public static IasTypeDao typeOfId(String id) {
		Objects.requireNonNull(id);
		return buildIasType(getIndexFromId(id));
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
		Set<IasioDao> iasios = biuldIasios(numOfIasios);
		cdbWriter.writeIasios(iasios, false);
	}
	
	/**
	 * Build the set of IASIOs configuration to write in the CDB
	 * 
	 * @param numOfIasios the number of IASIOs to write in the configurations
	 * @return the set of IASIOs configuration to write in the CDB
	 */
	public static Set<IasioDao> biuldIasios(int numOfIasios) {
		if (numOfIasios<=0) {
			throw new IllegalArgumentException("Invalid number of IASIOs to write in the CDB");
		}
		Set<IasioDao> iasios = new HashSet<>(numOfIasios);
		for (int t=0; t<numOfIasios; t++) {
			IasTypeDao iasType = buildIasType(t);
			IasioDao iasio = new IasioDao(buildIasId(t), "IASIO description", 1500, iasType);
			iasios.add(iasio);
		}
		return iasios;
	}
	
	/**
	 * Build a monitor point data from the passed iasio and value.
	 * 
	 * @param mPointData: the pojo to associate a value to a monitor point from the CDB
	 * @param pluginID: The ID of the plugin
	 * @param monitoredSystemID: The id of the system monitored by the plugin
	 * @return
	 */
	public static MonitorPointData buildMonitorPointData(
			MonitorPointsBuilderHelper mPointData,
			String pluginID, 
			String monitoredSystemID) {
		Objects.requireNonNull(mPointData);
		Objects.requireNonNull(pluginID);
		Objects.requireNonNull(monitoredSystemID);
		
		List<Sample> samples = new ArrayList<>();
		samples.add(new Sample("PlaceholderSmaple"));
		FilteredValue filteredValue = new FilteredValue(mPointData.value, samples, System.currentTimeMillis());
		ValueToSend valueToSend = new ValueToSend(mPointData.iasioDao.getId(), filteredValue);
		
		return new MonitorPointData(pluginID, monitoredSystemID, valueToSend);
	}
	
	/**
	 * Build the value of a Iasio depending on its type.
	 * @return
	 */
	public static Object buildIasioValue(IasTypeDao iasioDaoType) {
		Object value=null;
		switch (iasioDaoType) {
		case LONG: value = Long.valueOf(1234455667); break;
		case INT: value =Integer.valueOf(321456); break;
		case SHORT: value =Short.valueOf("121"); break;
		case BYTE: value =Byte.valueOf("10"); break;
		case DOUBLE: value =Double.valueOf(2234.6589); break;
		case FLOAT: value =Float.valueOf(554466.8702f); break;
		case BOOLEAN: value=Boolean.FALSE; break; 
		case CHAR: value = Character.valueOf('X'); break;
		case STRING: value="The string"; break;
		case ALARM: value = AlarmSample.SET; break;
		default: throw new UnsupportedOperationException("Unrecognized type "+iasioDaoType);
		}
		return value;
	}
	
	/**
	 * Build and return a A set of monitor point values from the passed iasios
	 * and values
	 * 
	 * @param mpData: the pojo to associate a value to a monitor point from the CDB
	 * @param pluginID: The ID of the plugin
	 * @param monitoredSystemID: The id of the system monitored by the plugin
	 * @return A set of monitor point built from the passed iasio
	 */
	public static Set<MonitorPointData> buildMonitorPointDatas(
			Set<MonitorPointsBuilderHelper> mpDatas,
			String pluginID, 
			String monitoredSystemID) {
		Objects.requireNonNull(mpDatas);
		Set<MonitorPointData> ret = new HashSet<>();
		for (MonitorPointsBuilderHelper mpData: mpDatas) {
			ret.add(buildMonitorPointData(mpData, pluginID, monitoredSystemID));
		}
		return ret;
	}

	/**
	 * Constructor
	 */
	private CommonHelpers() {	}

}
