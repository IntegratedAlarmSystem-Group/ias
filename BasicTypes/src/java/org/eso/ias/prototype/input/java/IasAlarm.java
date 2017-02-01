package org.eso.ias.prototype.input.java;

import org.eso.ias.prototype.input.AlarmValue;

public class IasAlarm extends IASValue<AlarmValue> {
	
	public IasAlarm(AlarmValue value,
			long tStamp,
			OperationalMode mode,
			String id,
			String runningId) {
		super(value,tStamp,mode,id,runningId,IASTypes.ALARM);
	}
	
	/**
	 * Build a new IasAlarm with the passed value
	 * 
	 * @param newValue The value to set in the new IASValue
	 * @return The new IASValue with the updated value
	 * @see IASTypes#updateValue(AlarmValue newValue)
	 */
	public IasAlarm updateValue(AlarmValue newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasAlarm(newValue,System.currentTimeMillis(),mode,id,runningId);
	}
	
	/**
	 * Build a new IasAlarm with the passed mode
	 * 
	 * @param newMode The mode to set in the new IASValue
	 * @return The new IASValue with the updated mode
	 * @see IASTypes#updateMode(OperationalMode newMode)
	 */
	public IasAlarm updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasAlarm(value,System.currentTimeMillis(),newMode,id,runningId);
	}

}

