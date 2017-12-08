package org.eso.ias.prototype.input.java;

public class IasString extends IASValue<String> {
	
	public IasString(String value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String id,
			String runningId) {
		super(value,tStamp,mode,iasValidity,id,runningId,IASTypes.STRING);
	}
	
	/**
	 * Build a new IasString with the passed value
	 * 
	 * @param newValue The value to set in the new IasString
	 * @return The new IasString with the updated value
	 * @see IASValue#updateValue(Object)
	 */
	@Override
	public IasString updateValue(String newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasString(newValue,System.currentTimeMillis(),mode,iasValidity,id,runningId);
	}
	
	/**
	 * Build a new IasFloatwith the passed mode
	 * 
	 * @param newMode The mode to set in the new IasString
	 * @return The new IasString with the updated mode
	 */
	public IasString updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasString(value,System.currentTimeMillis(),newMode,iasValidity,id,runningId);
	}

}
