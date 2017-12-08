package org.eso.ias.prototype.input.java;

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
    ALARM(AlarmSample.class,"AlarmType");
	
	public final Class typeClass; 
	
	public final String typeName;
    
    private IASTypes(Class c, String typeName) {
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
};
