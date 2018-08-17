package org.eso.ias.dasu.publisher

import org.eso.ias.types.InOut
import scala.util.Try
import org.eso.ias.types.IASValue
import org.eso.ias.types.IasValueStringSerializer
import scala.util.Success

/**
 * The ListenerOutputPublisherImpl publisher forwards the output 
 * to the listener as a IASValue or as a String if a string serializer 
 * is passed in the constructor.
 * 
 * The [[InOut]] is converted to a IASValue before being published.
 * 
 * @constructor build a new output publisher that forwards IASIOS 
 *              to the listener
 * @param listener the listener of events
 * @param stringSerializer the serializer to convert IASValues to string
 *                         if None the output is not converted to a string and not
 *                         sent to [[OutputListener.outputStringifiedEvent]]
 */
class ListenerOutputPublisherImpl (
    val listener: OutputListener,
    val stringSerializer: Option[IasValueStringSerializer])
    extends OutputPublisher {
  require(Option(listener).isDefined,"Invalid listener")
  
  def publishValue(iasValue: IASValue[_]): Try[Unit] = Try(listener.outputEvent(iasValue))
  
  def publishStringValue(str: Option[String]): Try[Unit] = Try(str.foreach(x => listener.outputStringifiedEvent(x)))
  
  /**
   * Initialize the publisher.
   * 
   * @see OutputPublisher.initialize()
   */
  override def initializePublisher(): Try[Unit] = { new Success(()) }
  
  /**
   * Release all the acquired resources.
   * 
   * @see OutputPublisher.cleanUp()
   */
  override def cleanUpPublisher(): Try[Unit] = { new Success(()) }
  
  /**
   * Sends the output to the listener the output.
   * 
   * @param iasValue the not null value to publish converted as IASValue
   * @return a try to let the caller aware of errors publishing
   */
  override def publish(iasValue: IASValue[_]): Try[Unit] = {
    assert(Option(iasValue).isDefined,"Invalid IASIO to publish")
    val stringifiedIasValue = stringSerializer.map(_.iasValueToString(iasValue))
    
    publishValue(iasValue).flatMap(x => publishStringValue(stringifiedIasValue))
  }
  
}