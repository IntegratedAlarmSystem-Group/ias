package org.eso.ias.prototype.input.java;

/**
 *  
 * @author acaproni
 */
public interface IasValueStringSerializer {
	
	/**
	 * Convert the passed {@link IASValue} to a String
	 * 
	 * @param iasValue The value to 
	 * @return A string representation of the passed value
	 * @throws IasValueSerializerException In case of error creating the string 
	 *                                     from the passed value 
	 */
	public String iasValueToString(IASValue<?> iasValue) throws IasValueSerializerException;
	
	/**
	 * Return the IASValue by parsing the passed string
	 * 
	 * @param str The string describing the IASValue
	 * @return the IASValue described in the passed string
	 * @throws IasValueSerializerException In case of error converting the string
	 */
	public IASValue<?> valueOf(String str)throws  IasValueSerializerException;

}
