package org.eso.ias.plugin.filter;

import java.lang.reflect.Constructor;

import org.eso.ias.plugin.PluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The factory of filters.
 * <P>
 * FilterFactory instantiates a filter by its class name and options.
 * 
 * @author acaproni
 *
 */
public class FilterFactory {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(FilterFactory.class);
	
	/**
	 * Constructor
	 * 
	 * @param className The non empty nor <code>null</code> name of the class of the filter
	 * @param options The options (can be <code>null</code>)
	 * @return An instance of the filter of the passed class
	 * @throws PluginException In case of error instantiating the filter
	 */
	public static Filter getFilter(String className, String options) throws PluginException {
		if (className==null || className.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty filter class name");
		}
		
		logger.debug("Loading filter {}",className);
		
		Class<?> clazz = null;
		try { 
			clazz = Class.forName(className.trim());
		} catch (ClassNotFoundException cnfe) {
			throw new PluginException("Class not found "+className,cnfe);
		}
		
		logger.debug("Loading costructor {}(String)",className);
		Constructor<?> ctor = null;
		try {
			ctor = clazz.getConstructor(String.class);
		} catch (Exception e) {
			throw new PluginException("Constructor "+className.trim()+"(String.class) not found",e);
		}
		
		String filterOpt = (options==null)?"":options;
		logger.debug("Instantiating the object passing param=[{}]",filterOpt);
		
		Object object = null;
		
		try {
			object = ctor.newInstance(new Object[] { filterOpt });
		} catch (Exception ie) {
			throw new PluginException("Error instantatiating object through  "+className.trim()+"(String.class) c'tor",ie);
		}
		
		// This is only to return a more meaningful error message
		logger.debug("Casting the object to Filter");
		Filter ret = null;
		try {
			ret = (Filter)object;
		} catch (Exception e) {
			throw new PluginException(className.trim()+" does not implement Filter interface",e);
		}
		logger.debug("Filter {} instantiated with param {}",className.trim(),filterOpt);
		return ret;
	}
}
