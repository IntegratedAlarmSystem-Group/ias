package org.eso.ias.dasu.publisher

import org.eso.ias.prototype.input.java.IASValue
import scala.util.Try
import org.eso.ias.prototype.input.java.IasValueJsonSerializer
import java.io.Writer
import org.ias.prototype.logging.IASLogger

/**
 * The publisher of IASValues produced by the DASU
 * in a JSON format.
 * 
 * The JSON output is composed of records of IASValues like
 * [ IASV1, IASV2, IASV3,...]
 * The ideal solution would be to let Jackson2 write the array of IASValues
 * passing one item at a time but, browsing the net, it does not seem as easy
 * as I expected.  
 * 
 * @param filePath the path of the JSON file to write
 */
class JsonWriterPublisher(val writer: Writer) extends OutputPublisher {
  require(Option(writer).isDefined)
  
  /** Flag to know when to write the separator */
  private[this] var firstRecordWritten = false
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /** jsonSerializer translates IASValue into JSON string */
  val jsonSerializer = new IasValueJsonSerializer()
  
  /**
   * Initialize the publisher.
   * 
   * @see OutputPublisher.initialize()
   */
  override def initialize(): Try[Unit] = {
    Try(writer.write('['))
  }
  
  /**
   * Release all the acquired resources.
   * 
   * @see OutputPublisher.cleanUp()
   */
  override def cleanUp(): Try[Unit] = { 
    Try(writer.write(']'))
   }
  
  /** Writes the separator */
  private def writeSeparator(): Try[Unit] = Try(if (firstRecordWritten)(writer.write(",\n")))
  
  /**
   * Publish the output.
   * 
   * @param aisio the not null IASIO to publish
   * @return a try to let the caller aware of errors publishing
   */
  def publish(iasio: IASValue[_]): Try[Unit] = {
    require(Option(iasio).isDefined)
    writeSeparator().map(x => Try{ writer.write(jsonSerializer.iasValueToString(iasio)); firstRecordWritten=true})
  }
}