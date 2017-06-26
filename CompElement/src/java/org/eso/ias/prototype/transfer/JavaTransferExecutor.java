package org.eso.ias.prototype.transfer;

import java.util.Map;
import java.util.Properties;

import org.eso.ias.prototype.input.java.IASValueBase;

/**
 * The JavaTransferExecutor provides the interface
 * for java implementators of the transfer function
 * as java data structs differ from scala ones.
 * 
 * @author acaproni
 *
 */
public abstract class JavaTransferExecutor extends TransferExecutor {
	
	
	public JavaTransferExecutor(
			String cEleId, 
			String cEleRunningId,
			Properties props
			) {
		super(cEleId,cEleRunningId,props);
	}

	/**
	 * Produces the output of the component by evaluating the inputs.
	 * 
	 * <EM>IMPLEMENTATION NOTE</EM>
	 * The {@link IASValueBase} is immutable. The easiest way to produce
	 * the output to return is to execute the methods of the actualOutput
	 * that return a new IASValue.
	 * 
	 * @param compInputs: the inputs to the ASCE
	 * @param actualOutput: the actual output of the ASCE
	 * @return the computed value to set as output of the ASCE
	 * @throws Exception in case of error
	 */
	public abstract IASValueBase eval(Map<String, IASValueBase> compInputs, IASValueBase actualOutput) throws Exception;

}
