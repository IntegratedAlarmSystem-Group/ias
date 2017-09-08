package org.eso.ias.prototype.input.java;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serialize/deserialize {@link IASValue} to/from JSON strings
 *
 * @see IasValueStringSerializer
 * @author acaproni
 *
 */
public class IasValueJsonSerializer implements IasValueStringSerializer {
	
	/**
	 * The jakson 2 mapper
	 */
	private final ObjectMapper jsonMapper = new ObjectMapper();

	/**
	 * Convert the value in a JSON string
	 * 
	 * @see IasValueStringSerializer
	 */
	@Override
	public String iasValueToString(IASValue<?> iasValue) throws IasValueSerializerException {
		Objects.requireNonNull(iasValue);
		try {
			return jsonMapper.writeValueAsString(iasValue);
		} catch (JsonProcessingException jpe) {
			throw new IasValueSerializerException("Error converting "+iasValue.id+" to a JSON string",jpe);
		}
	}

	/**
	 * Convert the passed JSON string into a {@link IASValue}
	 * 
	 * @see IasValueStringSerializer
	 */
	@Override
	public IASValue<?> valueOf(String str)  throws IasValueSerializerException {
		try {
			return jsonMapper.readValue(str, IASValue.class);
		} catch (Exception e) {
			throw new IasValueSerializerException("Error converting the JSON string ["+str+"] to a IAS value",e);
		}
	}

}
