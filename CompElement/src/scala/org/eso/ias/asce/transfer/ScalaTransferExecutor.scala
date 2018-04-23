package org.eso.ias.asce.transfer

import java.util.Properties

import org.eso.ias.types.InOut
import org.eso.ias.types.Identifier

/**
 * The <code>ScalaTransferExecutor<code> provides the interface
 * for scala implementators of the transfer function.
 */
abstract class ScalaTransferExecutor[T](cEleId: String, cEleRunningId: String, props: Properties) 
extends TransferExecutor(cEleId,cEleRunningId,props) {
  
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
  protected final def getValue(inputs: Map[String, InOut[_]], id: String): Option[InOut[_]] = {
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
	def eval(compInputs: Map[String, InOut[_]], actualOutput: InOut[T]): InOut[T]
  
}
