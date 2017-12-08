package org.eso.ias.prototype.transfer.impls;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.eso.ias.prototype.compele.exceptions.PropsMisconfiguredException;
import org.eso.ias.prototype.compele.exceptions.TypeMismatchException;
import org.eso.ias.prototype.compele.exceptions.UnexpectedNumberOfInputsException;
import org.eso.ias.prototype.input.java.AlarmSample;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValueBase;
import org.eso.ias.prototype.input.java.IasAlarm;
import org.eso.ias.prototype.input.java.IasByte;
import org.eso.ias.prototype.input.java.IasDouble;
import org.eso.ias.prototype.input.java.IasFloat;
import org.eso.ias.prototype.input.java.IasInt;
import org.eso.ias.prototype.input.java.IasLong;
import org.eso.ias.prototype.input.java.IasShort;
import org.eso.ias.prototype.transfer.JavaTransferExecutor;
import org.eso.ias.prototype.transfer.TransferExecutor;

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
public class MinMaxThresholdTFJava extends JavaTransferExecutor {

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

	public MinMaxThresholdTFJava(String cEleId, String cEleRunningId, Properties props) {
		super(cEleId, cEleRunningId, props);
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
	 * @see JavaTransferExecutor#eval(Map, IASValueBase)
	 */
	public IASValueBase eval(Map<String, IASValueBase> compInputs, IASValueBase actualOutput) throws Exception {
		if (compInputs.size() != 1)
			throw new UnexpectedNumberOfInputsException(compInputs.size(), 1);
		if (actualOutput.valueType != IASTypes.ALARM)
			throw new TypeMismatchException(actualOutput.runningId);

		// Get the input
		IASValueBase hio = compInputs.values().iterator().next();
		
		double hioValue;
		switch (hio.valueType) {
		case LONG:
			hioValue = ((IasLong) hio).value;
			break;
		case INT:
			hioValue = ((IasInt) hio).value;
			break;
		case SHORT:
			hioValue = ((IasShort) hio).value;
			break;
		case BYTE:
			hioValue = ((IasByte) hio).value;
			break;
		case DOUBLE:
			hioValue = ((IasDouble) hio).value;
			break;
		case FLOAT:
			hioValue = ((IasFloat) hio).value;
			break;
		default:
			throw new TypeMismatchException(hio.runningId);
		}
		
		boolean wasActivated = ((IasAlarm)actualOutput).value==AlarmSample.SET;
		
		boolean condition = 
				hioValue >= highOn || hioValue <= lowOn ||
				wasActivated && (hioValue>=highOff || hioValue<=lowOff);
				
				
		AlarmSample newOutput = AlarmSample.fromBoolean(condition);
		return ((IasAlarm) actualOutput).updateValue(newOutput);
	}
}
