package org.eso.ias.basictypes.test

import org.scalatest.FlatSpec
import org.eso.ias.prototype.input.Identifier
import org.eso.ias.prototype.input.InOut
import org.eso.ias.prototype.input.java.OperationalMode
import org.eso.ias.prototype.input.Validity
import org.eso.ias.prototype.input.java.IASTypes
import org.eso.ias.prototype.input.AlarmValue
import org.eso.ias.prototype.input.AlarmState
import org.eso.ias.prototype.input.AckState

class TestHIOEquality extends FlatSpec {
  
  def fixture = {
    new {
      val id = new Identifier(Some[String]("TestID"), None)
      val refreshRate=InOut.MinRefreshRate+10;
    }
  }
  
  behavior of "HeteroIOValue equality"
  
  it must "properly recognize if 2 values are equal" in {
    val f = fixture
    val hioInt = new InOut[Int](Some(3),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    val newHIO = new InOut[Int](Some(3),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    
    val v1 = hioInt.actualValue.value
    assert(v1.isDefined)
    val v2 = newHIO.actualValue.value
    assert(v2.isDefined)
    
    assert(v1==v2)
    
    // Different values differ
    val anoterHIO = new InOut[Int](Some(5),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    assert(v1!=anoterHIO.actualValue.value)
    assert(anoterHIO.actualValue!=v1)
    
    // Same value at different tstamp must be equal
    Thread.sleep(50);
    val lastHIO = new InOut[Int](Some(3),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    assert(v1==lastHIO.actualValue.value)
    
    // Check after updating a HIO
    val hio=anoterHIO.updateValue(Some(3))
    assert(v1==hio.actualValue.value)
    
  }
  
  it must "properly deal with None values" in {
    val f = fixture
    val hioInt = new InOut[Int](None,f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    val newHIO = new InOut[Int](f.id,f.refreshRate,IASTypes.INT)
    
    assert(hioInt.actualValue==newHIO.actualValue)
    
    val hio=hioInt.updateValue(Some(3))
    assert(hioInt.actualValue!=hio.actualValue.value)
    assert(hio.actualValue!=hioInt.actualValue.value)
    
    val hio2=newHIO.updateValue(Some(3))
    assert(hio.actualValue==hio2.actualValue)
    
  }
  
  it must "properly recognize different values" in {
    val f = fixture
    val hioInt = new InOut[Int](f.id,f.refreshRate,IASTypes.INT)
    val doubleHIO = new InOut[Double](f.id,f.refreshRate,IASTypes.DOUBLE)
    
    assert(hioInt.actualValue==doubleHIO.actualValue)
    
    val hio2=doubleHIO.updateValue(Some(3.6D))
    assert(hio2.actualValue!=hioInt.actualValue)
    assert(hioInt.actualValue!=hio2.actualValue)
    
    val hio3=hioInt.updateValue(Some(3))
    val hio4=doubleHIO.updateValue(Some(3D))
    assert(hio3.actualValue==hio4.actualValue)
    
    val hio5=doubleHIO.updateValue(Some(3.1D))
    assert(hio3.actualValue!=hio5.actualValue)
    
    val alarmHIO = InOut(Some(new AlarmValue),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.ALARM)
    assert(hio5.actualValue!=alarmHIO.actualValue)
    
  }
  
  behavior of "HeteroIOValue hashCode"
  
  it must "return the same integer for equal objects" in {
    
    val f = fixture
    val hioInt = InOut(Some(3),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    val newHIO = InOut(Some(3), f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    assert(hioInt.actualValue.hashCode()==newHIO.actualValue.hashCode())
  }
  
  behavior of "HIO equality and hashCode"
  
  it must "properly recognize if 2 HIOs are equal" in {
    val f = fixture
    val hioInt = new InOut[Int](Some(3),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    val newHIO = new InOut[Int](Some(3),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    
    assert(hioInt==newHIO)
    assert(hioInt.hashCode()==newHIO.hashCode())
    
    val doubleHIO = new InOut[Double](Some(3D),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.DOUBLE)
    assert(hioInt!=doubleHIO)
    assert(hioInt.hashCode()!=doubleHIO.hashCode()) // Not required in the contract of hashCode
    
    val doubleOtherRefRateHIO = new InOut[Double](Some(3D),f.id,f.refreshRate+5,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.DOUBLE)
    assert(doubleOtherRefRateHIO!=doubleHIO)
    assert(doubleOtherRefRateHIO.hashCode()!=doubleHIO.hashCode()) // Not required in the contract of hashCode
    
    val hioOtherModeInt = new InOut[Int](Some(3),f.id,f.refreshRate,OperationalMode.OPERATIONAL,Validity.Unreliable,IASTypes.INT)
    assert(hioOtherModeInt!=hioInt)
    assert(hioOtherModeInt.hashCode()!=hioInt.hashCode()) // Not required in the contract of hashCode
    
    val id2 = new Identifier(Some[String]("AnotherID"), None)
    val hioOtherIDInt = new InOut[Int](Some(3),id2,f.refreshRate,OperationalMode.OPERATIONAL,Validity.Unreliable,IASTypes.INT)
    assert(hioOtherIDInt!=hioInt)
    assert(hioOtherIDInt.hashCode()!=hioInt.hashCode()) // Not required in the contract of hashCode
    
    val hioOtherValidityInt = new InOut[Int](Some(3),f.id,f.refreshRate,OperationalMode.OPERATIONAL,Validity.Reliable,IASTypes.INT)
    assert(hioOtherValidityInt!=hioInt)
    assert(hioOtherValidityInt.hashCode()!=hioInt.hashCode()) // Not required in the contract of hashCode
    
    val hioAnotherValueInt = new InOut[Int](Some(5),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.INT)
    assert(hioAnotherValueInt!=hioInt)
    assert(hioAnotherValueInt.hashCode()!=hioInt.hashCode()) // Not required in the contract of hashCode
    
    val alarm1HIO = new InOut[AlarmValue](Some(new AlarmValue),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.ALARM)
    val alarm2HIO = new InOut[AlarmValue](Some(new AlarmValue),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.ALARM)
    assert(alarm1HIO==alarm2HIO)
    assert(alarm1HIO.hashCode()==alarm2HIO.hashCode())
    
    val alarm3HIO = new InOut[AlarmValue](Some(AlarmValue(AlarmState.Active,false,AckState.Acknowledged)),f.id,f.refreshRate,OperationalMode.UNKNOWN,Validity.Unreliable,IASTypes.ALARM)
    assert(alarm1HIO!=alarm3HIO)
  }
}
