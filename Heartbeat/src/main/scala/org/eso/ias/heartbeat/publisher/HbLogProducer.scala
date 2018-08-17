package org.eso.ias.heartbeat.publisher

import org.eso.ias.heartbeat.HbProducer
import org.eso.ias.heartbeat.HbMsgSerializer
import org.eso.ias.logging.IASLogger

/**
 * A HB producer that writes HBs in the log
 * 
 * @author acaproni
 *
 */
class HbLogProducer(serializer: HbMsgSerializer) extends HbProducer(serializer) {

	/** The logger */
	val logger = IASLogger.getLogger(classOf[HbLogProducer])
	  
	/** Initialize the producer */
  override def init() = {}
	
	/** Shutdown the producer */
  override def shutdown() = {}
  
  /**
   * Push the string
   */
  override def push(hbAsString: String) {
    logger.info("HeartBeat [{}]",hbAsString)
  }
}
