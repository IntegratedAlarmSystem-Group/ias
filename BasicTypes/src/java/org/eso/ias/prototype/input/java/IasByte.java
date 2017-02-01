package org.eso.ias.prototype.input.java;

public class IasByte extends IASValue<Byte> {
	
	public IasByte(Byte value,
			long tStamp,
			OperationalMode mode,
			String id,
			String runningId) {
		super(value,tStamp,mode,id,runningId,IASTypes.BYTE);
	}
	
	/**
	 * Build a new IasByte with the passed value
	 * 
	 * @param newValue The value to set in the new IasByte
	 * @return The new IasByte with the updated value
	 * @see IASTypes#updateValue()
	 */
	public IasByte updateValue(Byte newValue) {
		if (newValue==null) {
			throw new NullPointerException("The value can't be null");
		}
		return new IasByte(newValue,System.currentTimeMillis(),mode,id,runningId);
	}
	
	/**
	 * Build a new IasByte with the passed mode
	 * 
	 * @param newMode The mode to set in the new IasByte
	 * @return The new IasByte with the updated mode
	 * @see IASTypes#updateMode(OperationalMode newMode)
	 */
	public IasByte updateMode(OperationalMode newMode) {
		if (newMode==null) {
			throw new NullPointerException("The mode can't be null");
		}
		return new IasByte(value,System.currentTimeMillis(),newMode,id,runningId);
	}

}
