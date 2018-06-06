package org.eso.ias.component.test.transfer;

import java.util.Map;
import java.util.Properties;

import org.eso.ias.asce.transfer.IasIOJ;
import org.eso.ias.asce.transfer.JavaTransferExecutor;
import org.eso.ias.types.Alarm;
import org.eso.ias.types.OperationalMode;


/**
 * A java transfer function for testing
 * 
 * @author acaproni
 *
 */
public class TransferExecutorImpl  extends JavaTransferExecutor<Alarm> {
	
	/**
	 * Constructor 
	 * 
	 * @param asceId: the ID of the ASCE
	 * @param asceRunningId: the runningID of the ASCE
	 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
	 * @param props: the user defined properties
	 */
	public TransferExecutorImpl(String cEleId, 
			String cEleRunningId,
			long validityTimeFrame,
			Properties props) {
		super(cEleId,cEleRunningId,validityTimeFrame,props);
	}

	@Override
	public void initialize() throws Exception {
		System.out.println("java TransferExecutorImpl: Initializing");
	}

	@Override
	public void shutdown() throws Exception{
		System.out.println("java TransferExecutorImpl: shutting down");
	}
	
	@Override
	public IasIOJ<Alarm> eval(Map<String, IasIOJ<?>> compInputs, IasIOJ<Alarm>actualOutput) throws Exception{
		System.out.println("java TransferExecutorImpl: evaluating "+compInputs.size()+" inputs");
		System.out.println("java TransferExecutorImpl for comp. with ID="+compElementId+" and output "+actualOutput.toString());
		for (IasIOJ<?> input: compInputs.values()) {
			System.out.println(input);
		}
		IasIOJ<Alarm> newValue = actualOutput.updateMode(OperationalMode.SHUTTEDDOWN);
		newValue=newValue.updateValue(Alarm.getSetDefault()); 
		System.out.println("Returning: "+newValue);
		return newValue;
	}
}
