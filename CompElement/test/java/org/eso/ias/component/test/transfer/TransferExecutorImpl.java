package org.eso.ias.component.test.transfer;

import java.util.Map;
import java.util.Properties;

import org.eso.ias.types.IASValue;
import org.eso.ias.asce.transfer.JavaTransferExecutor;
import org.eso.ias.types.Alarm;
import org.eso.ias.types.OperationalMode;


/**
 * A java transfer function for testing
 * 
 * @author acaproni
 *
 */
public class TransferExecutorImpl  extends JavaTransferExecutor {
	
	public TransferExecutorImpl(String cEleId, 
			String cEleRunningId,
			Properties props) {
		super(cEleId,cEleRunningId,props);
	}

	@Override
	public void initialize() throws Exception {
		System.out.println("java TransferExecutorImpl: Initializing");
	}

	@Override
	public void shutdown() throws Exception{
		System.out.println("java TransferExecutorImpl: shutting down");
	}
	
	public IASValue<?> eval(Map<String, IASValue<?>> compInputs, IASValue<?>actualOutput) throws Exception{
		System.out.println("java TransferExecutorImpl: evaluating "+compInputs.size()+" inputs");
		System.out.println("java TransferExecutorImpl for comp. with ID="+compElementId+" and output "+actualOutput.toString());
		for (IASValue<?> input: compInputs.values()) {
			System.out.println(input);
		}
		IASValue<Alarm> newValue = ((IASValue<Alarm>)actualOutput).updateMode(OperationalMode.SHUTTEDDOWN);
		newValue=newValue.updateValue(Alarm.getSetDefault()); 
		System.out.println("Returning: "+newValue);
		return newValue;
	}
}
