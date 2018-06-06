package org.eso.ias.asce.transfer;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.eso.ias.types.IASValue;
import org.eso.ias.types.Identifier;

/**
 * The JavaTransferExecutor provides the interface
 * for java implementators of the transfer function
 * as java data structs differ from scala ones.
 * 
 * @author acaproni
 *
 */
public abstract class JavaTransferExecutor<T> extends TransferExecutor {
	
	/**
	 * Constructor
	 * 
	 * @param cEleId: The id of the ASCE
	 * @param cEleRunningId: the running ID of the ASCE
	 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
	 * @param props: The properties for the executor
	 */
	public JavaTransferExecutor(
			String cEleId, 
			String cEleRunningId,
			long validityTimeFrame,
			Properties props
			) {
		super(cEleId,cEleRunningId,validityTimeFrame,props);
	}
	
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
	 * @return the IASValue of the given ID or <code>null</code>
	 *         if a IASValue with the passed id is not in the map
	 */
	protected final IasIOJ<?> getValue(Map<String, IasIOJ<?>> inputs, String id) {
		Objects.requireNonNull(inputs,"Invalid map of inputs");
		Objects.requireNonNull(id,"Invalid IASIO ID");
		if (Identifier.isTemplatedIdentifier(id)) {
			throw new IllegalArgumentException("Templated IDs are forbidden here");
		}
		if (!isTemplated()) {
			return inputs.get(id);
		}
		
		// If the ASCE is templated, it can have non-templated or
		// template inputs.
		//
		// Let's try first with a non template
		IasIOJ<?> ret = inputs.get(id);
		if (ret==null) {
			// Bad luck: try with templated ID
			Integer instance = getTemplateInstance().get();
			String templatedID = Identifier.buildIdFromTemplate(id,instance);
			ret = inputs.get(templatedID);
		}
		return ret;
	}

	/**
	 * Produces the output of the component by evaluating the inputs.
	 * 
	 * <EM>IMPLEMENTATION NOTE</EM>
	 * The {@link IASValue} is immutable. The easiest way to produce
	 * the output to return is to execute the methods of the actualOutput
	 * that return a new IASValue.
	 * 
	 * @param compInputs: the inputs to the ASCE
	 * @param actualOutput: the actual output of the ASCE
	 * @return the computed value to set as output of the ASCE
	 * @throws Exception in case of error
	 */
	public abstract IasIOJ<T> eval(Map<String, IasIOJ<?>> compInputs, IasIOJ<T> actualOutput) throws Exception;

}
