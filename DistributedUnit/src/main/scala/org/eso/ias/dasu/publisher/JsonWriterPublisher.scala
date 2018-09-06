package org.eso.ias.dasu.publisher

import org.eso.ias.types.IASValue
import scala.util.Try
import org.eso.ias.types.IasValueJsonSerializer
import java.io.Writer
import org.eso.ias.logging.IASLogger
import scala.util.Success

/**
 * The publisher of IASValues produced by the DASU
 * in a JSON-like format.
 * 
 * The JSON output is composed of records of IASV-n JSON string of the IASValues like
 * IASV1
 * IASV2
 * IASV3
 * 
 * The format of a true JSON file is [ IASV1, IASV2, IASV3,...]
 * 
 * The ideal solution would be to let Jackson2 write the array of IASValues
 * passing one item at a time but, browsing the net, it does not seem as easy
 * as I expected.  
 * 
 * @param filePath the path of the JSON file to write
 */
class JsonWriterPublisher(val writer: Writer) extends OutputPublisher {
  require(Option(writer).isDefined)
  
  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)
  
  /** jsonSerializer translates IASValue into JSON string */
  val jsonSerializer = new IasValueJsonSerializer()
  
  /**
   * Initialize the publisher.
   * 
   * @see OutputPublisher.initialize()
   */
  override def initializePublisher(): Try[Unit] = Success(())
  
  /**
   * Release all the acquired resources.
   * 
   * @see OutputPublisher.cleanUp()
   */
  override def cleanUpPublisher(): Try[Unit] = Success(())
  
  /**
   * Publish the output.
   * 
   * @param aisio the not null IASIO to publish
   * @return a try to let the caller aware of errors publishing
   */
  def publish(iasio: IASValue[_]): Try[Unit] = {
    require(Option(iasio).isDefined)
    Try{ 
      writer.write(jsonSerializer.iasValueToString(iasio)) 
      writer.write('\n')
      writer.flush()
    }
  }
}
