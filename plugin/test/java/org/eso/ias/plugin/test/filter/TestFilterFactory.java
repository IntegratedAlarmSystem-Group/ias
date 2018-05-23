package org.eso.ias.plugin.test.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eso.ias.plugin.PluginException;
import org.eso.ias.plugin.filter.AverageBySamples;
import org.eso.ias.plugin.filter.Filter;
import org.eso.ias.plugin.filter.FilterFactory;
import org.junit.jupiter.api.Test;

/**
 * Test the instantiation of filters by {@link FilterFactory}
 * 
 * @author acaproni
 *
 */
public class TestFilterFactory {
	
	/**
	 * Check if the build works
	 * @throws Exception
	 */
	@Test
	public void instantiateAvgBySample() throws Exception {
		String className = "org.eso.ias.plugin.filter.AverageBySamples";
		String filterOpt = "50";
		
		FilterFactory.getFilter(className, filterOpt); 
		
	}
	
	/**
	 * test if the options are passed to the constructor
	 * of the filter
	 * 
	 * @throws Exception
	 */
	@Test
	public void passOptionToConstructor() throws Exception {
		String className = "org.eso.ias.plugin.filter.AverageBySamples";
		String filterOpt = "50";
		
		Filter avgFilter = FilterFactory.getFilter(className, filterOpt); 
		assertEquals(Integer.parseInt(filterOpt),((AverageBySamples)avgFilter).numberOfSamplesToAverage);
	}
	
	/**
	 * Check if an exception is thrwon when the class does not exist
	 * @throws Exception
	 */
	@Test
	public void testThrowsExceptionFilterNotFound() throws Exception {
		String className = "org.eso.ias.plugin.filter.DoesNotExistFilter";
		String filterOpt = "50";
		
		assertThrows(PluginException.class, () -> FilterFactory.getFilter(className, filterOpt));
	}

}
