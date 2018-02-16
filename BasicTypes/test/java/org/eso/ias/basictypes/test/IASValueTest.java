package org.eso.ias.basictypes.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.IdentifierType;
import org.eso.ias.types.OperationalMode;
import org.eso.ias.types.Validity;

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
		Identifier supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,none);
		Identifier dasuId =  new Identifier("DasuId",IdentifierType.DASU,new Some(supervId));
		Identifier asceId =  new Identifier("AsceId",IdentifierType.ASCE,new Some(dasuId));
		Identifier id = new Identifier(valId, IdentifierType.IASIO,new Some(asceId));
		
		IASValue<?> val = IASValue.build(
				10L, 
				OperationalMode.DEGRADED, 
				IasValidity.RELIABLE, 
				id.fullRunningID(), 
				IASTypes.LONG);
		assertEquals(valId, val.id);
		
	}
}
