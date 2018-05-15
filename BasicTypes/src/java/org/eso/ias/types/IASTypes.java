package org.eso.ias.types;

import java.util.Objects;

import org.eso.ias.cdb.pojos.IasTypeDao;

/**
 * Java representation of the IAS types.
 * 
 * In this case it is better to have java enumerations instead 
 * of scala because the twos differ too much up to the point 
 * that scala Eumeration are not usable within java sources.
 *
 * TODO: avoid duplication with org.eso.ias.cdb.pojos.IasType
 * @see org.eso.ias.cdb.pojos.IasTypeDao
 * @author acaproni
 *
 */
public enum IASTypes {
	LONG(java.lang.Long.class,"LongType"), 
    INT(java.lang.Integer.class,"IntType"), 
    SHORT(java.lang.Short.class,"ShortType"), 
    BYTE(java.lang.Byte.class,"ByteType"), 
    DOUBLE(java.lang.Double.class,"DoubleType"), 
    FLOAT(java.lang.Float.class,"FloatType"), 
    BOOLEAN(java.lang.Boolean.class,"BooleanType"), 
    CHAR(java.lang.Character.class,"CharType"), 
    STRING(java.lang.String.class,"StringType"), 
    ALARM(Alarm.class,"AlarmType");
	
	public final Class<?> typeClass; 
	
	public final String typeName;
    
    private IASTypes(Class<?> c, String typeName) {
    	this.typeClass=c;
    	this.typeName=typeName;
	}
    
    public IasTypeDao toIasTypeDao() {
    	if (this==LONG) return IasTypeDao.LONG;
    	else if (this==INT) return IasTypeDao.INT;
    	else if (this==SHORT) return IasTypeDao.SHORT;
    	else if (this==BYTE) return IasTypeDao.BYTE;
    	else if (this==DOUBLE) return IasTypeDao.DOUBLE;
    	else if (this==FLOAT) return IasTypeDao.FLOAT;
    	else if (this==BOOLEAN) return IasTypeDao.BOOLEAN;
    	else if (this==CHAR) return IasTypeDao.CHAR;
    	else if (this==STRING) return IasTypeDao.STRING;
    	else if (this==ALARM) return IasTypeDao.ALARM;
    	else throw new UnsupportedOperationException("Unsupported IAS type "+this.typeName);
    }
    
    /**
     * Build a IASTypes from the DAO definition
     * 
     * @param typeDao the IasTypeDao
     * @return the related IASTypes for the passed type DAO
     */
    public static IASTypes fromIasioDaoType(IasTypeDao typeDao) {
    	Objects.requireNonNull(typeDao);
    	switch (typeDao) {
    	case LONG: return LONG;
    	case INT: return INT;
    	case SHORT: return SHORT;
    	case BYTE: return BYTE;
    	case DOUBLE: return DOUBLE;
    	case FLOAT: return FLOAT;
    	case BOOLEAN: return BOOLEAN;
    	case CHAR: return CHAR;
    	case STRING: return STRING;
    	case ALARM: return ALARM;
    	default: throw new UnsupportedOperationException("Unsupported DAO type "+typeDao);
    	}
    }
    
    /**
	 * Parse the passed string java object
	 * 
	 * @param value the string representation of the value
	 * @return the java object for the give value and type
	 */
    public Object convertStringToObject(String value) {
    	if (value==null || value.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty value string to parse");
		}
    	switch (this) {
    	case LONG: return Long.parseLong(value);
    	case INT: return Integer.parseInt(value);
    	case SHORT: return Short.parseShort(value);
    	case BYTE: return Byte.parseByte(value);
    	case DOUBLE: return Double.parseDouble(value);
    	case FLOAT: return Float.parseFloat(value);
    	case BOOLEAN: return Boolean.parseBoolean(value);
    	case CHAR: return value.charAt(0);
    	case STRING: return value;
    	case ALARM: return Alarm.valueOf(value);
    	default: throw new UnsupportedOperationException("Unsupported type "+this);
	}
    }
    
    /**
	 * Parse the passed string of the given type into a java object
	 * 
	 * @param value the string representation of the value
	 * @param valueType the type of the value
	 * @return the java object for the give value and type
	 */
	public static Object convertStringToObject(String value, IASTypes valueType) {
		Objects.requireNonNull(valueType);
		return valueType.convertStringToObject(value);
	}
};
