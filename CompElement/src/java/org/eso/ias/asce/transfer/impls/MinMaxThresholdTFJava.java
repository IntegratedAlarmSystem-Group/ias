package org.eso.ias.asce.transfer.impls;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eso.ias.types.Alarm;
import org.eso.ias.types.IASTypes;
import org.eso.ias.asce.exceptions.PropsMisconfiguredException;
import org.eso.ias.asce.exceptions.TypeMismatchException;
import org.eso.ias.asce.exceptions.UnexpectedNumberOfInputsException;
import org.eso.ias.asce.transfer.IasIOJ;
import org.eso.ias.asce.transfer.JavaTransferExecutor;
import org.eso.ias.asce.transfer.TransferExecutor;

/**
 * The TF implementing a Min/Max threshold TF  (there is also
 * a scala implementation for comparison).
 * 
 * The alarm is activated when the alarm is higher then
 * the max threshold or when it is lower then the low threshold.
 * 
 * We could call this alarm a "Non-nominal temperature" because it is 
 * equally set if the temperature is too low or is too high but
 * cannot distinguish between the 2 cases.
 * 
 * If we want to distinguish between the 2 cases,  we need 2 ASCE having 
 * the same input, one checking for the high value and the other checking 
 * for the low value.
 * 
 * To be generic, the value of the properties and that of the IASIO 
 * are converted in double.
 * 
 * The value of the Min and Max thresholds are passed as properties:
 * <UL>
 * 	<LI>HighON: the (high) alarm is activated when the value of the IASIO 
 *              is greater then HighON
 *  <LI>HighOFF: if the (high) alarm is active and the value of the IASIO
 *               goes below HighOFF, then the alarm is deactivated
 *  <LI>LowOFF: if the (low) alarm is active and the value of the IASIO
 *               becomes greater then LowOFF, then the alarm is deactivated
 *  <LI>LowON: the (low) alarm is activated when the value of the IASIO is
 *             lower then LowON
 * </UL>
 *   
 * @author acaproni
 */
public class MinMaxThresholdTFJava extends JavaTransferExecutor<Alarm> {

	/**
	 * The name of the HighOn property
	 */
	public static final String highOnPropName = "org.eso.ias.tf.minmaxthreshold.java.highOn";

	/**
	 * The name of the HighOff property
	 */
	public static final String highOffPropName = "org.eso.ias.tf.minmaxthreshold.java.highOff";

	/**
	 * The name of the lowOn property
	 */
	public static final String lowOnPropName = "org.eso.ias.tf.minmaxthreshold.java.lowOn";

	/**
	 * The name of the lowOff property
	 */
	public static final String lowOffPropName = "org.eso.ias.tf.minmaxthreshold.java.lowOff";
	
	/** 
	 * The name of the property to set the priority of the alarm 
	 */
	public static final String alarmPriorityPropName = "org.eso.ias.tf.alarm.priority";

	/**
	 * The (high) alarm is activated when the value of the HIO is greater then
	 * HighON
	 */
	public final double highOn = getValue(props, MinMaxThresholdTFJava.highOnPropName, Double.MAX_VALUE);

	/**
	 * if the (high) alarm is active and the value of the HIO goes below
	 * HighOFF, then the alarm is deactivated
	 */
	public final double highOff = getValue(props, MinMaxThresholdTFJava.highOffPropName, Double.MAX_VALUE);

	/**
	 * the (low) alarm is activated when the value of the HIO is lower then
	 * LowON
	 */
	public final double lowOn = getValue(props, MinMaxThresholdTFJava.lowOnPropName, Double.MIN_VALUE);

	/**
	 * if the (low) alarm is active and the value of the HIO becomes greater
	 * then LowOFF, then the alarm is deactivated
	 */
	public final double lowOff = getValue(props, MinMaxThresholdTFJava.lowOffPropName, Double.MIN_VALUE);
	
	public final Alarm alarmSet = Alarm.valueOf(
			props.getProperty(MinMaxThresholdTFJava.alarmPriorityPropName, Alarm.getSetDefault().toString()));
	
	/** 
	 * Additional properties
	 */
	public final Map<String , String> additionalProperties = new HashMap<>();

	/**
	 * Get the value of a property from the passed properties.
	 * 
	 * @param props:
	 *            The properties to look for the property with the given name
	 * @param propName:
	 *            the name of the property
	 * @param default:
	 *            the value to return if the property is not defined in the
	 *            passed properties
	 */
	private double getValue(Properties props, String propName, double default_value) {
		String propStr = props.getProperty(propName);
		if (propStr != null) {
			return Double.valueOf(propStr);
		} else {
			return default_value;
		}
	}

	/**
	 * Constructor 
	 * 
	 * @param asceId: the ID of the ASCE
	 * @param asceRunningId: the runningID of the ASCE
	 * @param validityTimeFrame: The time frame (msec) to invalidate monitor points
	 * @param props: the user defined properties
	 */
	public MinMaxThresholdTFJava(String cEleId, String cEleRunningId, long validityTimeFrame, Properties props) {
		super(cEleId, cEleRunningId, validityTimeFrame, props);
	}

	/**
	 * Initialize the TF by getting the four properties (being the properties
	 * lazy, they will be initialized here.
	 * 
	 * This method merely checks if the values of the properties are coherent
	 * with the definitions given above.
	 * 
	 * @see TransferExecutor#initialize()
	 */
	@Override
	public void initialize() throws Exception {
		if (highOn < highOff) {
			Properties p = new Properties();
			p.put(MinMaxThresholdTFJava.highOnPropName, "" + highOn);
			p.put(MinMaxThresholdTFJava.highOffPropName, "" + highOff);
			throw new PropsMisconfiguredException(p);
		}
		if (lowOff < lowOn) {
			Properties p = new Properties();
			p.put(MinMaxThresholdTFJava.lowOnPropName, "" + lowOn);
			p.put(MinMaxThresholdTFJava.lowOffPropName, "" + lowOff);
			throw new PropsMisconfiguredException(p);

		}
		if (lowOff > highOff) {
			Properties p = new Properties();
			p.put(MinMaxThresholdTFJava.lowOffPropName, "" + lowOff);
			p.put(MinMaxThresholdTFJava.highOffPropName, "" + highOff);
			throw new PropsMisconfiguredException(p);
		}
	}

	/**
	 * @see TransferExecutor#shutdown()
	 */
	@Override
	public void shutdown() {}

	/**
	 * @see JavaTransferExecutor#eval(Map, IASValue)
	 */
	public IasIOJ<Alarm> eval(Map<String, IasIOJ<?>> compInputs, IasIOJ<Alarm> actualOutput) throws Exception {
		if (compInputs.size() != 1)
			throw new UnexpectedNumberOfInputsException(compInputs.size(), 1);
		if (actualOutput.getType() != IASTypes.ALARM)
			throw new TypeMismatchException(actualOutput.getFullrunningId());

		// Get the input
		IasIOJ<?> iasio = compInputs.values().iterator().next();
		
		double hioValue;
		
		switch (iasio.getType()) {
		case LONG:
		case INT:
		case SHORT:
		case BYTE:
		case DOUBLE:
		case FLOAT:
			Number num = (Number)iasio.getValue().get();
			hioValue = num.doubleValue();
			break;
		default:
			throw new TypeMismatchException(iasio.getFullrunningId());
		}
		
		System.out.println("===>"+actualOutput.getId()+" "+actualOutput.getValue());
		boolean wasActivated = actualOutput.getValue().isPresent() && ((Alarm)actualOutput.getValue().get()).isSet();
		
		boolean condition = 
				hioValue >= highOn || hioValue <= lowOn ||
				wasActivated && (hioValue>=highOff || hioValue<=lowOff);
				
				
		Alarm newOutput;
		if (condition) {
			newOutput=alarmSet;
		} else {
			newOutput=Alarm.cleared();
		}
		additionalProperties.put("actualValue", Double.valueOf(hioValue).toString());
		return actualOutput.updateValue(newOutput).updateMode(iasio.getMode()).updateProps(additionalProperties);
	}
}
