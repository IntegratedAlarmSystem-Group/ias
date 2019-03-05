package org.eso.ias.monitor.test

import java.io.File

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

    assert(fromJSonString.getClientIds.isEmpty)
    assert(fromJSonString.getExcludedSupervisorIds.isEmpty)
    assert(fromJSonString.getPluginIds.isEmpty)
    assert(fromJSonString.getConverterIds.isEmpty)

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

    assert(fromJSonString.getKafkaSinkConnectors.size()==3)
    assert(fromJSonString.getKafkaSinkConnectors.contains(kConnectorA));
    assert(fromJSonString.getKafkaSinkConnectors.contains(kConnectorB));
    assert(fromJSonString.getKafkaSinkConnectors.contains(kConnectorC));
  }

  it must "read/write the core tools ids" in {
    val config = new IasMonitorConfig
    config.setThreshold(30.toLong)

    val ids = Set("CoreT1-id","CoreT2-id","CoreT3-id","CoreT4-id")
    config.setCoreToolsIds(JavaConverters.setAsJavaSet(ids))

    val jsonStr = config.toJsonString
    val fromJSonString = IasMonitorConfig.valueOf(jsonStr)

    assert(fromJSonString.getCoreToolsIds.size()==4)
    assert(fromJSonString.getCoreToolsIds.contains("CoreT1-id"));
    assert(fromJSonString.getCoreToolsIds.contains("CoreT2-id"));
    assert(fromJSonString.getCoreToolsIds.contains("CoreT3-id"));
    assert(fromJSonString.getCoreToolsIds.contains("CoreT4-id"));
  }

  it must "read the configuration from a file" in {
    val f = new File("./config.json")

    val config = IasMonitorConfig.fromFile(f)

    assert(config.getThreshold==15.toLong)

    assert(config.getExcludedSupervisorIds.size()==2)
    assert(config.getPluginIds.size()==3)
    assert(config.getConverterIds.size()==2)
    assert(config.getClientIds.size()==4)
    assert(config.getSinkIds.size()==1)
    assert(config.getKafkaSinkConnectors.size()==3)

    assert(config.getExcludedSupervisorIds.contains("ExcSup1"));
    assert(config.getExcludedSupervisorIds.contains("ExcSup2"));

    assert(config.getPluginIds.contains("p1"));
    assert(config.getPluginIds.contains("p2"));
    assert(config.getPluginIds.contains("p3"));

    assert(config.getConverterIds.contains("conv1"));
    assert(config.getConverterIds.contains("conv2"));

    assert(config.getClientIds.contains("client1"));
    assert(config.getClientIds.contains("client2"));
    assert(config.getClientIds.contains("client3"));
    assert(config.getClientIds.contains("client4"));

    assert(config.getSinkIds.contains("sink1"));

    val kConnectorA = new KafkaSinkConnectorConfig("host.name.org",8192,"IdA")
    val kConnectorB = new KafkaSinkConnectorConfig("name.host.org",8193,"IdB")
    val kConnectorC = new KafkaSinkConnectorConfig("host.name.com",8194,"IdC")

    assert(config.getKafkaSinkConnectors.contains(kConnectorA))
    assert(config.getKafkaSinkConnectors.contains(kConnectorB))
    assert(config.getKafkaSinkConnectors.contains(kConnectorC))
  }

}

object ConfigTest {
  /** The logger */
  private val logger = IASLogger.getLogger(ConfigTest.getClass)
}
