package org.eso.ias.prototype.input.java;

import org.eso.ias.prototype.input.AlarmValue;

/**
 * Java representation of the IAS types.
 * 
 * In this case it is better to have java enumerations instead 
 * of scala because the twos differ too much up to the point 
 * that scala Eumeration are not usable within java sources.
 * 
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
    ALARM(AlarmValue.class,"AlarmType");
	
	public final Class typeClass; 
	
	public final String typeName;
    
    private IASTypes(Class c, String typeName) {
    	this.typeClass=c;
    	this.typeName=typeName;
	}
};
