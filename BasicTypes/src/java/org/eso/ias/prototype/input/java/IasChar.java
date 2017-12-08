package org.eso.ias.prototype.input.java;

public class IasChar extends IASValue<Character> {
	
	public IasChar(Character value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String id,
			String runningId) {
		super(value,tStamp,mode,iasValidity,id,runningId,IASTypes.CHAR);
	}
	
	/**
	 * Build a new IasChar with the passed value
	 * 
	 * @param newValue The value to set in the new IASValue
	 * @return The new IasChar with the updated value
	 * @see IASValue#updateValue(Object)
	 */
	@Override
	public IasChar updateValue(Character newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasChar(newValue,System.currentTimeMillis(),mode,iasValidity,id,runningId);
	}
	
	/**
	 * Build a new IasAlarm with the passed mode
	 * 
	 * @param newMode The mode to set in the new IASValue
	 * @return The new IASValue with the updated mode
	 */
	public IasChar updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasChar(value,System.currentTimeMillis(),newMode,iasValidity,id,runningId);
	}

}
