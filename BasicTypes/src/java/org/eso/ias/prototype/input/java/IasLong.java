package org.eso.ias.prototype.input.java;

public class IasLong extends IASValue<Long> {
	
	public IasLong(Long value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId) {
		super(value,tStamp,mode,iasValidity,fullRunningId,IASTypes.LONG);
	}
	
	/**
	 * Build a new IasLong with the passed value
	 * 
	 * @param newValue The value to set in the new IasLong
	 * @return The new IasLong with the updated value
	 * @see IASValue#updateValue(Object)
	 */
	@Override
	public IasLong updateValue(Long newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasLong(newValue,System.currentTimeMillis(),mode,iasValidity,fullRunningId);
	}
	
	/**
	 * Build a new IasLong with the passed mode
	 * 
	 * @param newMode The mode to set in the new IasLong
	 * @return The new IasLong with the updated mode
	 */
	public IasLong updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasLong(value,System.currentTimeMillis(),newMode,iasValidity,fullRunningId);
	}

}
