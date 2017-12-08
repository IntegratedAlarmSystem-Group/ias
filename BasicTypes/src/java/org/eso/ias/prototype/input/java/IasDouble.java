package org.eso.ias.prototype.input.java;

public class IasDouble extends IASValue<Double> {
	
	public IasDouble(Double value,
			long tStamp,
			OperationalMode mode,
			IasValidity iasValidity,
			String id,
			String runningId) {
		super(value,tStamp,mode,iasValidity,id,runningId,IASTypes.DOUBLE);
	}
	
	/**
	 * Build a new IasDouble with the passed value
	 * 
	 * @param newValue The value to set in the new IasDouble
	 * @return The new IasDouble with the updated value
	 * @see IASValue#updateValue(Object)
	 */
	@Override
	public IasDouble updateValue(Double newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasDouble(newValue,System.currentTimeMillis(),mode,iasValidity,id,runningId);
	}
	
	/**
	 * Build a new IasDouble with the passed mode
	 * 
	 * @param newMode The mode to set in the new IasDouble
	 * @return The new IasDouble with the updated mode
	 */
	public IasDouble updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasDouble(value,System.currentTimeMillis(),newMode,iasValidity,id,runningId);
	}

}
