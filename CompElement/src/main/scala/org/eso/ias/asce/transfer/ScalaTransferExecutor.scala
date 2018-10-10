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
	  * This method transparently returns a value from the passed ID and instance.
    *
    * When an input is not templated, neither a template instance, then its value
    * cabn be directly retrieved from the map of the inputs passed to [#eval].
    * But if the imput is templated or a templated instance then this method must be used.
    *
    * If the ASCE is generated out of a template,
    * its inputs can or cannot be generated out of the same template. In the latter,
    * their identifiers must be enriched with the number of the instance of the ASCE.
    * For templated inputs the number of the instance is not needed because it is
    * the same instance number of the ASCE and it is known to the transfer function.
    * The case of templated input instances is different because their instance number
    * is defined in the configuration of the ASCE and not known in the transfer function
    * so it must be esplicity passed by the caller.
    *
    * For normal and templated inputs, the instance is empty.
    * For templated inputs the instance can be passed if it is teh same of the ASCE.
    * For templated input instances the instance number is required.
    *
    * This method must be used also to get templated input instances from non templated ASCEs.
    *
    * @param inputs the map of the inputs
	  * @param id The (non templated) identifier of the value
    * @param instance the optional instance nummber for templated input instances,
    *                 not required for other cases
	  * @return the IASValue of the given ID, or None if not found in the Map
	  */
  protected final def getValue(
                                inputs: Map[String, IasIO[_]],
                                id: String,
                                instance: Option[Int]=None): Option[IasIO[_]] = {
    if (Option(id).isEmpty || id.isEmpty) {
      throw new IllegalArgumentException("Invalid null or empty identifier");
    }
    if (Identifier.isTemplatedIdentifier(id)) {
			throw new IllegalArgumentException("Templated IDs are forbidden here");
		}
    
    val fromMap = inputs.get(id)

    (isTemplated(), fromMap.isDefined, instance) match {
      case ( _, true, _) => fromMap // It is in the map, return immediately
      case (true, _, None) => {
        // Templated and NOT in the map: it is  a templated input
        // whose instance number is teh same of the ASCE
        val templateId = Identifier.buildIdFromTemplate(id, getTemplateInstance.get)
        inputs.get(templateId)
      }
      case (_, _ , Some(inst)) => {
        // An instance is given, then it is a templated input instance
        val templateId = Identifier.buildIdFromTemplate(id, inst)
        inputs.get(templateId)
      }
      case ( false, false, None ) => None // No templated, not in map, not a templated instance
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

	/**
		* Initialize the transfer function
		*
		* @param inputsInfo The IDs and types of the inputs
    * @param outputInfo The Id and type of thr output
		*/
	def initialize(inputsInfo: Set[IasioInfo],outputInfo: IasioInfo): Unit
  
}
