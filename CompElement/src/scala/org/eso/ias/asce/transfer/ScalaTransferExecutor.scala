package org.eso.ias.asce.transfer

import java.util.Properties

import org.eso.ias.types.Identifier

/**
 * The <code>ScalaTransferExecutor<code> provides the interface
 * for scala implementations of the transfer function.
 * 
 * @param cEleId: The id of the ASCE
 * @param cEleRunningId: the running ID of the ASCE
 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
 * @param props: The properties for the executor
 */
abstract class ScalaTransferExecutor[T](cEleId: String, cEleRunningId: String, validityTimeFrame: Long, props: Properties) 
extends TransferExecutor(cEleId,cEleRunningId,validityTimeFrame,props) {
  
  /**
	 * This method transparently return a value from the passed ID,
	 * being the ASCE templated or not.
	 * 
	 * If the ASCE is not templated, this method delegates to 
	 * {@link Map#get(Object)}.
	 * 
	 * If the ASCE is generated out of a template,
	 * its inputs can or cannot be generated out of the same template. In the latter, 
	 * their identifiers must be enriched with the number of the instance.
	 * 
	 * @param inputs the map of the inputs
	 * @param id The (non templated) identifier of the value
	 * @return the IASValue of the given ID, or None if not found in the Map
	 */
  protected final def getValue(inputs: Map[String, IasIO[_]], id: String): Option[IasIO[_]] = {
    if (Identifier.isTemplatedIdentifier(id)) {
			throw new IllegalArgumentException("Templated IDs are forbidden here");
		}
    
    val fromMap = inputs.get(id)
    (isTemplated(), fromMap) match {
      case (true, None) => {
        val templateId = Identifier.buildIdFromTemplate(id, getTemplateInstance.get)
        inputs.get(templateId)
      }
      case (_, _) => fromMap
    }
  }

	/**
		*
		* @param id the id to translate, templated or not
		* @return teh identifer translated if the ASCE is templated
		*/
	protected final def getIdentifier(id: String): String = {
		require(Option(id).isDefined && !id.isEmpty)
		if (!isTemplated) id
		else if (Identifier.isTemplatedIdentifier(id)) id
		else Identifier.buildIdFromTemplate(id, getTemplateInstance.get)
	}
  
  /**
	 * Produces the output of the component by evaluating the inputs.
	 * 
	 * <EM>IMPLEMENTATION NOTE</EM>
	 * The {@link InOut} is immutable. The easiest way to produce
	 * the output to return is to execute the methods of the actualOutput
	 * that returns a new InOut.
	 * 
	 * @param compInputs: the inputs to the ASCE
	 * @param actualOutput: the actual output of the ASCE
	 * @return the computed value to set as output of the ASCE
	 */
	def eval(compInputs: Map[String, IasIO[_]], actualOutput: IasIO[T]): IasIO[T]
  
}
