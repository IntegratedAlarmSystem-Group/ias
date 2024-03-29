package org.eso.ias.asce.test.transfer;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eso.ias.asce.transfer.IasIOJ;
import org.eso.ias.asce.transfer.IasioInfo;
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
	public TransferExecutorImpl(String asceId,
			String asceRunningId,
			long validityTimeFrame,
			Properties props) {
		super(asceId,asceRunningId,validityTimeFrame,props);
	}


	/**
	 * Initialize the transfer function
	 *
	 * @param inputsInfo The IDs and types of the inputs
	 * @param outputInfo The Id and type of thr output
	 * @throws Exception in case of errors
	 */
	@Override
	public void initialize(Set<IasioInfo> inputsInfo, IasioInfo outputInfo) throws Exception {
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
			System.out.println(input.toString());
		}
		IasIOJ<Alarm> newValue = actualOutput.updateMode(OperationalMode.SHUTTEDDOWN);
		newValue=newValue.updateValue(Alarm.getInitialAlarmState().set());
		System.out.println("Returning: "+newValue);
		return newValue;
	}
}
