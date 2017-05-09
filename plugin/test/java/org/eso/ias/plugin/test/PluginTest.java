package org.eso.ias.plugin.test;

import java.util.Collection;
import java.util.Vector;

import org.eso.ias.plugin.Plugin;
import org.eso.ias.plugin.PluginException;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.config.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginTest  {
	
	private static final Logger logger = LoggerFactory.getLogger(PluginTest.class);
	
	public static void main(String[] args) {
		logger.info("PluginTest started...");
		
		Collection<Value> values = new Vector<>();
		Value v = new Value();
		v.setId("MonitorPointID");
		v.setRefreshTime(1500);
		values.add(v);
		
		Plugin plugin = new Plugin("PluginID", "iasserver.hq.eso.org", 8192, values);
		plugin.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ie) {}
		
		Sample s = new Sample(Double.valueOf(15.5));
		try {
			plugin.updateMonitorPointValue(v.getId(), s);
			logger.info("Sample submitted");
		}catch (PluginException pe) {
			logger.error("Error submitting a sample",pe);
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException ie) {}
		plugin.shutdown();
		
		logger.info("PluginTest done.");
	}

	public PluginTest() {
		// TODO Auto-generated constructor stub
	}

}
