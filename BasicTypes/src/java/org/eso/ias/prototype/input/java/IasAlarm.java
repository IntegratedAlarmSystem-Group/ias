package org.eso.ias.prototype.input.java;

/**
 * The IASVAlue encapsulating an alarm.
 * <P>
 * The type of the alarm at this stage is a {@link AlarmSample}
 * as produced by a monitored system.
 * 
 * @author acaproni
 *
 */
public class IasAlarm extends IASValue<AlarmSample> {
	
	public IasAlarm(AlarmSample value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String id,
			String runningId) {
		super(value,tStamp,mode,iasValidity,id,runningId,IASTypes.ALARM);
	}
	
	/**
	 * Build a new IasAlarm with the passed value
	 * 
	 * @param newValue The value to set in the new IASValue
	 * @return The new IASValue with the updated value
	 * @see IASValue#updateValue(Object)
	 */
	@Override
	public IasAlarm updateValue(AlarmSample newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasAlarm(newValue,System.currentTimeMillis(),mode,iasValidity,id,runningId);
	}
	
	/**
	 * Build a new IasAlarm with the passed mode
	 * 
	 * @param newMode The mode to set in the new IASValue
	 * @return The new IASValue with the updated mode
	 */
	public IasAlarm updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasAlarm(value,System.currentTimeMillis(),newMode,iasValidity,id,runningId);
	}

}

