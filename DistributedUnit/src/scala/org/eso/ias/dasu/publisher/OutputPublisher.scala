package org.eso.ias.dasu.publisher

import org.eso.ias.prototype.input.InOut
import scala.util.Try

/**
 * The OutputPublisher publishes the output generated by the
 * DASU.
 * 
 * The DASU keeps publishing the output even in case of error.
 * 
 */
trait OutputPublisher {
  
  /**
   * Publish the output.
   * 
   * @param aisio the not null IASIO to publish
   * @return a try to let the caller aware of errors publishing
   */
  def publish(iasio: InOut[_]): Try[Unit]  
}