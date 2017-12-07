package org.eso.ias.prototype.input.java;

public class IasFloat extends IASValue<Float> {
	
	public IasFloat(Float value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String id,
			String runningId) {
		super(value,tStamp,mode,iasValidity,id,runningId,IASTypes.FLOAT);
	}
	
	/**
	 * Build a new IasFloat with the passed value
	 * 
	 * @param newValue The value to set in the new IasFloat
	 * @return The new IasFloat with the updated value
	 * @see IASValue#updateValue(Object)
	 */
	@Override
	public IasFloat updateValue(Float newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasFloat(newValue,System.currentTimeMillis(),mode,iasValidity,id,runningId);
	}
	
	/**
	 * Build a new IasFloatwith the passed mode
	 * 
	 * @param newMode The mode to set in the new IasFloat
	 * @return The new IasFloat with the updated mode
	 */
	public IasFloat updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasFloat(value,System.currentTimeMillis(),newMode,iasValidity,id,runningId);
	}

}
