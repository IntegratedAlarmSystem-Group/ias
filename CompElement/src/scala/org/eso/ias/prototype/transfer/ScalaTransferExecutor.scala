package org.eso.ias.prototype.transfer

import java.util.Properties
import org.eso.ias.prototype.input.InOut

/**
 * The <code>ScalaTransferExecutor<code> provides the interface
 * for scala implementators of the transfer function.
 */
abstract class ScalaTransferExecutor[T](cEleId: String, cEleRunningId: String, props: Properties) 
extends TransferExecutor(cEleId,cEleRunningId,props) {
  
  /**
	 * Produces the output of the component by evaluating the inputs.
	 * 
	 * <EM>IMPLEMENTATION NOTE</EM>
	 * The {@link HeteroInOut} is immutable. The easiest way to produce
	 * the output to return is to execute the methods of the actualOutput
	 * that return a new HeteroInOut.
	 * 
	 * @param compInputs: the inputs to the ASCE
	 * @param actualOutput: the actual output of the ASCE
	 * @return the computed value to set as output of the ASCE
	 */
	def eval(compInputs: Map[String, InOut[_]], actualOutput: InOut[T]): InOut[T]
  
}
