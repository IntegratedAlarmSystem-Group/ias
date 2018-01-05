package org.eso.ias.basictypes.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.eso.ias.prototype.input.Identifier;
import org.eso.ias.prototype.input.Validity;
import org.eso.ias.prototype.input.java.IASTypes;
import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValidity;
import org.eso.ias.prototype.input.java.IdentifierType;
import org.eso.ias.prototype.input.java.OperationalMode;

import scala.Some;
import scala.Option;

public class IASValueTest {

	/**
	 * Test the correctness of the ID generated from the fullRunningId
	 * 
	 * @throws Exception
	 */
	@Test
	public void testId() throws Exception {
		String valId="testIdentifier";
		Option<Identifier> none = Option.apply(null);
		Identifier dasuId =  new Identifier("DasuId",IdentifierType.DASU,none);
		Identifier asceId =  new Identifier("AsceId",IdentifierType.ASCE,new Some(dasuId));
		Identifier id = new Identifier(valId, IdentifierType.IASIO,new Some(asceId));
		
		IASValue<?> val = IASValue.buildIasValue(
				10L, 
				System.currentTimeMillis(),
				OperationalMode.DEGRADED, IasValidity.RELIABLE, 
				id.fullRunningID(), 
				IASTypes.LONG);
		assertEquals(valId, val.id);
		
	}
}
