package org.eso.ias.converter;

import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValueBase;

public interface ConverterEngine {
	
	/**
	 * Convert the passed value produced by a remote monitored system
	 * in the IAS data type to pass to the core for processing
	 * 
	 * @param remoteSystemData The not <code>null</code> value read from the 
	 *                         monitored system
	 * @param type The type of the value to convert
	 * @return The value cobverted into a IAS data type
	 */
	public IASValueBase translate(MonitorPointData remoteSystemData, IASTypes type);

}
