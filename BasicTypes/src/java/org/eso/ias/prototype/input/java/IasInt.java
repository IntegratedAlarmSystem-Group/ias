package org.eso.ias.prototype.input.java;

public class IasInt extends IASValue<Integer> {
	
	public IasInt(Integer value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String fullRunningId) {
		super(value,tStamp,mode,iasValidity,fullRunningId,IASTypes.INT);
	}

	/**
	 * Build a new IasInt with the passed value
	 * 
	 * @param newValue The value to set in the new IasInt
	 * @return The new IasInt with the updated value
	 * @see IASValue#updateValue(Object)
	 */
	@Override
	public IasInt updateValue(Integer newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasInt(newValue,System.currentTimeMillis(),mode,iasValidity,fullRunningId);
	}
	
	/**
	 * Build a new IasInt with the passed mode
	 * 
	 * @param newMode The mode to set in the newIasInt
	 * @return The new IasInt with the updated mode
	 */
	public IasInt updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasInt(value,System.currentTimeMillis(),newMode,iasValidity,fullRunningId);
	}
}
