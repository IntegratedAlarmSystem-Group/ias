package org.eso.ias.kafkautils.test;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.SimpleStringProducer;

/**
 * 
 * Objects of this class uses the {@link SimpleStringProducer}  
 * to push strings in the kafka topic for the passed time interval.
 * <P>
 * The purpose of this class is to have a producer for testing 
 * everything related to kafka. 
 * 
 * 
 * @author acaproni
 *
 */
public class KafkaProducer implements Runnable {
	
	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	/**
	 * The suffix appended to the message 
	 */
	public final String suffix;
	
	/**
	 * The time to sleep (ms) before sending consecutive messages
	 */
	public final long sleepTime;
	
	/**
	 * The time to run the tool (ms)
	 */
	public final long timeToRun;
	
	/**
	 * The number of messages sent to kafka
	 */
	public long messagesProduced=0;
	
	public final SimpleStringProducer producer;
	
	/**
	 * Constructor
	 * 
	 * @param suffix The suffix to append to each string produced
	 * @param timeinterval The time to run
	 * @param timeUnit The unit of the time to run
	 * @param slepeTime the interval between consecutive messages
	 * @param sleepTimeUnit the unit of the interval
	 * @param topicName the topic to push string to
	 */
	public KafkaProducer(
			String suffix, 
			long timeinterval, 
			TimeUnit timeUnit,
			long slepeTime,
			TimeUnit sleepTimeUnit,
			String topicName) {
		this.suffix=suffix;
		sleepTime = TimeUnit.MILLISECONDS.convert(slepeTime,sleepTimeUnit);
		timeToRun = TimeUnit.MILLISECONDS.convert(timeinterval,timeUnit);
		
		producer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, topicName, "IAS-KafkaProd");
	}
	
	/**
	 * Initialize the producer with the passed topic name
	 * 
	 * 
	 */
	public void init() {
		producer.setUp();
	}
	
	public Thread start() {
		Thread ret = new Thread(this,"KafkaProducer-thread");
		ret.start();
		return ret;
	}
	
	
	/**
	 * Cose the producer
	 */
	public void done() {
		producer.tearDown();
	}
	
	@Override
	public void run() {
		long now = System.currentTimeMillis();
		
		while (System.currentTimeMillis()-now<timeToRun) {
			
			
			
			Date d = new Date(System.currentTimeMillis());
			String s = dateFormat.format(d);
			messagesProduced+=1;
			String strToPush = s+" #"+messagesProduced+suffix;
			try {
				producer.push(strToPush, null, null);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			System.out.println("Pushed: "+strToPush);
			try {
				Thread.sleep(sleepTime);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		
	}
	
	public static void main(String[] args) {
		KafkaProducer kp = new KafkaProducer(
				" - a string just to write something", 
				1, TimeUnit.DAYS, 
				1, TimeUnit.SECONDS, 
				KafkaHelper.IASIOs_TOPIC_NAME);
		
		kp.init();
		Thread t = kp.start();
		try {
			t.join();
		} catch (InterruptedException ie) {
			System.err.println("Interrupted!");
		}
		kp.done();
	}

}
