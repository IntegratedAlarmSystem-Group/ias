package org.eso.ias.component.test.transfer;

import java.util.Map;
import java.util.Properties;

import org.eso.ias.prototype.input.AlarmValue;
import org.eso.ias.prototype.input.Set;
import org.eso.ias.prototype.input.java.IASValueBase;
import org.eso.ias.prototype.input.java.IasAlarm;
import org.eso.ias.prototype.input.java.OperationalMode;
import org.eso.ias.prototype.transfer.JavaTransferExecutor;


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
	
	public IASValueBase eval(Map<String, IASValueBase> compInputs, IASValueBase actualOutput) throws Exception{
		System.out.println("java TransferExecutorImpl: evaluating "+compInputs.size()+" inputs");
		System.out.println("java TransferExecutorImpl for comp. with ID="+compElementId+" and output "+actualOutput.toString());
		for (IASValueBase input: compInputs.values()) {
			System.out.println(input);
		}
		IASValueBase newValue = actualOutput.updateMode(OperationalMode.SHUTDOWN);
		AlarmValue alarm = ((IasAlarm)newValue).value;
		alarm = AlarmValue.transition(alarm, new Set()).right().get();
		newValue=((IasAlarm)newValue).updateValue(alarm); 
		System.out.println("Returning: "+newValue);
		return newValue;
	}
}
