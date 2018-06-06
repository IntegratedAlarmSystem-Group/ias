package org.eso.ias.plugin.test;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;

/**
 * 
 * A Mockup for the sender of Hbs
 * @author acaproni
 *
 */
public class MockHeartBeatProd extends HbProducer {


	/**
	 * The HBs published 
	 */
	public final List<String> publishedHBs = new Vector<>();
	
	/**
	 * Signal if the producer has been initialized
	 */
	public AtomicBoolean initialized = new AtomicBoolean(false);
	
	/**
	 * Signal if the producer has been closed
	 */
	public AtomicBoolean closed = new AtomicBoolean(false);
	
	/**
	 * Constructor
	 * 
	 * @param hbSer Th eserializer of HB messages
	 */
	public MockHeartBeatProd(HbMsgSerializer hbSer) {
		super(hbSer);
	}

	@Override
	public void init() {
		initialized.set(true);
	}

	@Override
	public void push(String hbAsString) {
		publishedHBs.add(hbAsString);
		
	}

	@Override
	public void shutdown() {
		closed.set(true);
	}
}
