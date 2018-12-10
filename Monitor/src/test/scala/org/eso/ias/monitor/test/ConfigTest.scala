package org.eso.ias.monitor.test

import org.eso.ias.logging.IASLogger
import org.eso.ias.monitor.{IasMonitorConfig, KafkaSinkConnectorConfig}
import org.scalatest.FlatSpec

import scala.collection.JavaConverters

/** Test the [[org.eso.ias.monitor.IasMonitorConfig]]
  *
  */
class ConfigTest extends FlatSpec {

  behavior of "The IasMonitorConfig"

  it must "read/write the threshold" in {
    val config = new IasMonitorConfig
    config.setThreshold(30.toLong)

    val jsonStr = config.toJsonString
    val fromJSonString = IasMonitorConfig.valueOf(jsonStr)

    assert(fromJSonString.getThreshold==config.getThreshold)
  }

  it must "read/write the plugin ids" in {
    val config = new IasMonitorConfig
    config.setThreshold(30.toLong)

    val ids = Set("PluginA-id","PluginB-id","PluginC-id","PluginD-id")
    config.setPluginIds(JavaConverters.setAsJavaSet(ids))

    val jsonStr = config.toJsonString
    val fromJSonString = IasMonitorConfig.valueOf(jsonStr)

    assert(fromJSonString.getPluginIds.size()==4)
    assert(fromJSonString.getPluginIds.contains("PluginA-id"));
    assert(fromJSonString.getPluginIds.contains("PluginB-id"));
    assert(fromJSonString.getPluginIds.contains("PluginC-id"));
    assert(fromJSonString.getPluginIds.contains("PluginD-id"));
  }

  it must "read/write the converter ids" in {
    val config = new IasMonitorConfig
    config.setThreshold(30.toLong)

    val ids = Set("ConvA-id","ConvB-id","ConvC-id","ConvD-id")
    config.setConverterIds(JavaConverters.setAsJavaSet(ids))

    val jsonStr = config.toJsonString
    val fromJSonString = IasMonitorConfig.valueOf(jsonStr)

    assert(fromJSonString.getConverterIds.size()==4)
    assert(fromJSonString.getConverterIds.contains("ConvA-id"));
    assert(fromJSonString.getConverterIds.contains("ConvB-id"));
    assert(fromJSonString.getConverterIds.contains("ConvC-id"));
    assert(fromJSonString.getConverterIds.contains("ConvD-id"));
  }

  it must "read/write the client ids" in {
    val config = new IasMonitorConfig
    config.setThreshold(30.toLong)

    val ids = Set("ClientA-id","ClientB-id","ClientC-id","ClientD-id")
    config.setClientIds(JavaConverters.setAsJavaSet(ids))

    val jsonStr = config.toJsonString
    val fromJSonString = IasMonitorConfig.valueOf(jsonStr)

    assert(fromJSonString.getClientIds.size()==4)
    assert(fromJSonString.getClientIds.contains("ClientA-id"));
    assert(fromJSonString.getClientIds.contains("ClientB-id"));
    assert(fromJSonString.getClientIds.contains("ClientC-id"));
    assert(fromJSonString.getClientIds.contains("ClientD-id"));
  }

  it must "read/write the sink ids" in {
    val config = new IasMonitorConfig
    config.setThreshold(30.toLong)

    val ids = Set("SinkA-id","SinkB-id","SinkC-id","SinkD-id")
    config.setSinkIds(JavaConverters.setAsJavaSet(ids))

    val jsonStr = config.toJsonString
    val fromJSonString = IasMonitorConfig.valueOf(jsonStr)

    assert(fromJSonString.getSinkIds.size()==4)
    assert(fromJSonString.getSinkIds.contains("SinkA-id"));
    assert(fromJSonString.getSinkIds.contains("SinkB-id"));
    assert(fromJSonString.getSinkIds.contains("SinkC-id"));
    assert(fromJSonString.getSinkIds.contains("SinkD-id"));
  }

  it must "read/write the set of kafka connectors" in {
    val config = new IasMonitorConfig
    config.setThreshold(30.toLong)

    val kConnectorA = new KafkaSinkConnectorConfig("host.name.org",8192,"IdA")
    val kConnectorB = new KafkaSinkConnectorConfig("name.host.org",8193,"IdB")
    val kConnectorC = new KafkaSinkConnectorConfig("host.name.com",8194,"IdC")


    config.setKafkaSinkConnectors(JavaConverters.setAsJavaSet(Set(kConnectorA,kConnectorB,kConnectorC)))

    val jsonStr = config.toJsonString
    val fromJSonString = IasMonitorConfig.valueOf(jsonStr)

    println(jsonStr)

    assert(fromJSonString.getKafkaSinkConnectors.size()==3)
    assert(fromJSonString.getKafkaSinkConnectors.contains(kConnectorA));
    assert(fromJSonString.getKafkaSinkConnectors.contains(kConnectorB));
    assert(fromJSonString.getKafkaSinkConnectors.contains(kConnectorC));
  }

}

object ConfigTest {
  /** The logger */
  private val logger = IASLogger.getLogger(ConfigTest.getClass)
}
