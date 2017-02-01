package org.eso.ias.prototype.input.java;

public class IasShort extends IASValue<Short> {
	
	public IasShort(Short value,
			long tStamp,
			OperationalMode mode,
			String id,
			String runningId) {
		super(value,tStamp,mode,id,runningId,IASTypes.SHORT);
	}
	
	/**
	 * Build a new IasShort with the passed value
	 * 
	 * @param newValue The value to set in the new IasShort
	 * @return The new IasFloat with the updated value
	 * @see IASTypes#updateValue()
	 */
	public IasShort updateValue(Short newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasShort(newValue,System.currentTimeMillis(),mode,id,runningId);
	}
	
	/**
	 * Build a new IasShort with the passed mode
	 * 
	 * @param newMode The mode to set in the new IasShort
	 * @return The new IasShort with the updated mode
	 * @see IASTypes#updateMode(OperationalMode newMode)
	 */
	public IasShort updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasShort(value,System.currentTimeMillis(),newMode,id,runningId);
	}

}
