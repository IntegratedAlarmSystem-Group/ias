import org.scalatest.flatspec.AnyFlatSpec
import ch.qos.logback.classic.Level

import org.eso.ias.types.IasValidity
import org.eso.ias.types.IasValidity.*
import org.eso.ias.types.IASValue
import org.eso.ias.types.*
import org.eso.ias.logging.IASLogger
import org.eso.ias.dasu.ReceivedIasValuesContainer

class ReceivedIasValuesContTest extends AnyFlatSpec {
    /** The logger */
    private val logger = IASLogger.getLogger(this.getClass);
    IASLogger.setLogLevel(Some(Level.DEBUG), None, None)

    // The identifier of the monitored system
    val monSysId = new Identifier("MonSysID",IdentifierType.MONITORED_SOFTWARE_SYSTEM,None)

    // The identifier of the plugin
    val pluginId = new Identifier("PluginID",IdentifierType.PLUGIN,Some(monSysId))

    // The identifier of the converter
    val converterId = new Identifier("ConverterID",IdentifierType.CONVERTER,Some(pluginId))

    // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
    val inputTemperature1ID = new Identifier("Temperature1", IdentifierType.IASIO,converterId)
    // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
    val inputTemperature2ID = new Identifier("Temperature2", IdentifierType.IASIO,converterId)
    // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
    val inputTemperature3ID = new Identifier("Temperature3", IdentifierType.IASIO,converterId)
    // The ID of the temperature 1 monitor point in unput to ASCE-Temp1
    val inputTemperature4ID = new Identifier("Temperature4", IdentifierType.IASIO,converterId)

    def buildValue(
      identifier: Identifier, 
      d: Double): IASValue[?] = buildValue(identifier, d, RELIABLE)
  
    def buildValue(
      identifier: Identifier, 
      d: Double,
      validity: IasValidity): IASValue[?] = {
    
        val t0 = System.currentTimeMillis()-100
    
        IASValue.build(
        d,
        OperationalMode.OPERATIONAL,
        validity,
        identifier.fullRunningID,
        IASTypes.DOUBLE,
        t0,
        t0+1,
        t0+5,
        t0+10,
        t0+15,
        null,
        null,
        null,
        null)
    }

    behavior of "The container of not yet processed IASValues"
  
    it must "be empty after construction" in {
        val container = new ReceivedIasValuesContainer("DASU1", Set("ID1","ID2"))
        assert(container.isEmpty)
        assert(container.size == 0)
        assert(!container.nonEmpty)
    }

    it must "be non empty after pushing values into it" in {
        val container = new ReceivedIasValuesContainer("DASU1", Set("ID1","ID2"))
        val v1 = buildValue(inputTemperature1ID, 10.0)
        val v2 = buildValue(inputTemperature2ID, 20.0)
        val v3 = buildValue(inputTemperature3ID, 30.0)
        val v4 = buildValue(inputTemperature4ID, 40.0)
        container.storeInputs(Set(v1,v2,v3,v4))
        assert(container.size == 0)
        assert(container.isEmpty)
        assert(!container.nonEmpty)
    }

    it must "accept only values with accepted IDs" in {
        val container = new ReceivedIasValuesContainer("DASU1", Set(inputTemperature1ID.id,inputTemperature2ID.id))
        val v1 = buildValue(inputTemperature1ID, 10.0)
        val v2 = buildValue(inputTemperature2ID, 20.0)
        val v3 = buildValue(inputTemperature3ID, 30.0)
        val v4 = buildValue(inputTemperature4ID, 40.0)
        container.storeInputs(Set(v1,v2,v3,v4))
        assert(container.size == 2)
        assert(container.nonEmpty)
        assert(!container.isEmpty)
    }

    it must "clear the values" in {
        val container = new ReceivedIasValuesContainer("DASU1", Set(inputTemperature1ID.id,inputTemperature2ID.id))
        val v1 = buildValue(inputTemperature1ID, 10.0)
        val v2 = buildValue(inputTemperature2ID, 20.0)
        val v3 = buildValue(inputTemperature3ID, 30.0)
        val v4 = buildValue(inputTemperature4ID, 40.0)
        container.storeInputs(Set(v1,v2,v3,v4))
        assert(container.size == 2)
        assert(container.nonEmpty)
        assert(!container.isEmpty)

        container.clear()
        assert(container.size == 0)
        assert(container.isEmpty)
        assert(!container.nonEmpty)
    }

    it must "return the full running IDs of the values" in {
        val container = new ReceivedIasValuesContainer("DASU1", Set(inputTemperature1ID.id,inputTemperature2ID.id))
        val v1 = buildValue(inputTemperature1ID, 10.0)
        val v2 = buildValue(inputTemperature2ID, 20.0)
        val v3 = buildValue(inputTemperature3ID, 30.0)
        val v4 = buildValue(inputTemperature4ID, 40.0)
        container.storeInputs(Set(v1,v2,v3,v4))

        val fullRunningIds = container.getFullRunningIds
        assert(fullRunningIds.size == 2)
        assert(fullRunningIds.contains(v1.id))
        assert(fullRunningIds.contains(v2.id))
    }

    it must "return the values and clear the container" in {
        val container = new ReceivedIasValuesContainer("DASU1", Set(inputTemperature1ID.id,inputTemperature2ID.id))
        val v1 = buildValue(inputTemperature1ID, 10.0)
        val v2 = buildValue(inputTemperature2ID, 20.0)
        val v3 = buildValue(inputTemperature3ID, 30.0)
        val v4 = buildValue(inputTemperature4ID, 40.0)
        container.storeInputs(Set(v1,v2,v3,v4))

        val values = container.getValuesAndClear()
        assert(values.size == 2)
        assert(values.contains(v1))
        assert(values.contains(v2))

        assert(container.size == 0)
        assert(container.isEmpty)
        assert(!container.nonEmpty)
    }
}
