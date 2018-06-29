#! /usr/bin/env python
'''
Created on Jun 8, 2018

@author: acaproni
'''
import unittest
from IASLogging.logConf import Log
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasBasicTypes.Validity import Validity
from IasBasicTypes.OperationalMode import OperationalMode
from IasBasicTypes.IasType import IASType

class TestIasValue(unittest.TestCase):

    # A JSON string for testing
    jSonStr = """{"value":"CLEARED","sentToBsdbTStamp":"2018-03-07T13:08:43.525","dasuProductionTStamp":"2018-03-07T13:08:43.524",
            "depsFullRunningIds":["(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature3:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature2:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature4:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature1:IASIO)"],
            "mode":"UNKNOWN", "iasValidity":"RELIABLE",
            "fullRunningId":"(SupervId:SUPERVISOR)@(DasuWith7ASCEs:DASU)@(ASCE-AlarmsThreshold:ASCE)@(TooManyHighTempAlarm:IASIO)","valueType":"ALARM",
            "props":{"key1":"value1","key2":"value2"}}"""
            
            
    jSonStr2 = """{"value":"SET_HIGH","pluginProductionTStamp":"1970-01-01T00:00:00.1",
            "sentToConverterTStamp":"1970-01-01T00:00:00.2", "receivedFromPluginTStamp":"1970-01-01T00:00:00.3",
            "convertedProductionTStamp":"1970-01-01T00:00:00.4","sentToBsdbTStamp":"1970-01-01T00:00:00.5",
            "readFromBsdbTStamp":"1970-01-01T00:00:00.6","dasuProductionTStamp":"1970-01-01T00:00:00.7",
            "depsFullRunningIds":["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)","(SupervId2:SUPERVISOR)@(dasuVID2:DASU)@(asceVID2:ASCE)@(AlarmID2:IASIO)"],
            "mode":"DEGRADED","iasValidity":"RELIABLE",
            "fullRunningId":"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)",
            "valueType":"ALARM"}"""

    def testName(self):
        iasValue = IasValue.fromJSon(self.jSonStr)
        
        self.assertEqual(iasValue.id,"TooManyHighTempAlarm")
        
        expectedProps= {"key1":"value1","key2":"value2"}
        expectedDeps = ["(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature3:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature2:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature4:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature1:IASIO)"]
        
        
        self.assertEqual(iasValue.value,"CLEARED")
        self.assertEqual(iasValue.valueTypeStr,"ALARM")
        self.assertEqual(iasValue.valueType,IASType.ALARM)
        self.assertEqual(iasValue.fullRunningId,"(SupervId:SUPERVISOR)@(DasuWith7ASCEs:DASU)@(ASCE-AlarmsThreshold:ASCE)@(TooManyHighTempAlarm:IASIO)")
        self.assertEqual(iasValue.id,"TooManyHighTempAlarm")
        self.assertEqual(iasValue.dependentsFullRuningIds,expectedDeps)
        self.assertEqual(iasValue.modeStr,"UNKNOWN")
        self.assertEqual(iasValue.mode,OperationalMode.UNKNOWN)
        self.assertEqual(iasValue.iasValidityStr,"RELIABLE")
        self.assertEqual(iasValue.iasValidity,Validity.RELIABLE)
        self.assertEqual(iasValue.props,expectedProps)
    
        self.assertEqual(iasValue.pluginProductionTStampStr,None)
        self.assertEqual(iasValue.sentToConverterTStampStr,None)
        self.assertEqual(iasValue.receivedFromPluginTStampStr,None)
        self.assertEqual(iasValue.convertedProductionTStampStr,None)
        self.assertEqual(iasValue.sentToBsdbTStampStr,"2018-03-07T13:08:43.525")
        self.assertEqual(iasValue.readFromBsdbTStampStr,None)
        self.assertEqual(iasValue.dasuProductionTStampStr,"2018-03-07T13:08:43.524")
        
        expectedDeps2 = ["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)","(SupervId2:SUPERVISOR)@(dasuVID2:DASU)@(asceVID2:ASCE)@(AlarmID2:IASIO)"]
        
        iasValue2 = IasValue.fromJSon(self.jSonStr2)
        self.assertEqual(iasValue2.value,"SET_HIGH")
        self.assertEqual(iasValue2.valueTypeStr,"ALARM")
        self.assertEqual(iasValue2.valueType,IASType.ALARM)
        self.assertEqual(iasValue2.fullRunningId,"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)")
        self.assertEqual(iasValue2.id,"AlarmType-ID")
        self.assertEqual(iasValue2.dependentsFullRuningIds,expectedDeps2)
        self.assertEqual(iasValue2.modeStr,"DEGRADED")
        self.assertEqual(iasValue2.mode,OperationalMode.DEGRADED)
        self.assertEqual(iasValue2.iasValidityStr,"RELIABLE")
        self.assertEqual(iasValue2.iasValidity,Validity.RELIABLE)
        self.assertEqual(iasValue2.props,None)
    
        self.assertEqual(iasValue2.pluginProductionTStampStr,"1970-01-01T00:00:00.1")
        self.assertEqual(iasValue2.pluginProductionTStamp,Iso8601TStamp.Iso8601ToDatetime(iasValue2.pluginProductionTStampStr))
        
        self.assertEqual(iasValue2.sentToConverterTStampStr,"1970-01-01T00:00:00.2")
        self.assertEqual(iasValue2.sentToConverterTStamp,Iso8601TStamp.Iso8601ToDatetime(iasValue2.sentToConverterTStampStr))
        
        self.assertEqual(iasValue2.receivedFromPluginTStampStr,"1970-01-01T00:00:00.3")
        self.assertEqual(iasValue2.receivedFromPluginTStamp,Iso8601TStamp.Iso8601ToDatetime(iasValue2.receivedFromPluginTStampStr))
        
        self.assertEqual(iasValue2.convertedProductionTStampStr,"1970-01-01T00:00:00.4")
        self.assertEqual(iasValue2.convertedProductionTStamp,Iso8601TStamp.Iso8601ToDatetime(iasValue2.convertedProductionTStampStr))
        
        self.assertEqual(iasValue2.sentToBsdbTStampStr,"1970-01-01T00:00:00.5")
        self.assertEqual(iasValue2.sentToBsdbTStamp,Iso8601TStamp.Iso8601ToDatetime(iasValue2.sentToBsdbTStampStr))
        
        self.assertEqual(iasValue2.readFromBsdbTStampStr,"1970-01-01T00:00:00.6")
        self.assertEqual(iasValue2.readFromBsdbTStamp,Iso8601TStamp.Iso8601ToDatetime(iasValue2.readFromBsdbTStampStr))
        
        self.assertEqual(iasValue2.dasuProductionTStampStr,"1970-01-01T00:00:00.7")
        self.assertEqual(iasValue2.dasuProductionTStamp,Iso8601TStamp.Iso8601ToDatetime(iasValue2.dasuProductionTStampStr))
    
    def testToJSON(self):
        iasValue = IasValue.fromJSon(self.jSonStr)
        
        iasValueJson = iasValue.toJSonString()
        iasFomJson = IasValue.fromJSon(iasValueJson)
        
        self.assertEqual(iasValue.value,iasFomJson.value)
        self.assertEqual(iasValue.valueTypeStr,iasFomJson.valueTypeStr)
        self.assertEqual(iasValue.valueType,iasFomJson.valueType)
        self.assertEqual(iasValue.fullRunningId,iasFomJson.fullRunningId)
        self.assertEqual(iasValue.modeStr,iasFomJson.modeStr)
        self.assertEqual(iasValue.mode,iasFomJson.mode)
        self.assertEqual(iasValue.iasValidityStr,iasFomJson.iasValidityStr)
        self.assertEqual(iasValue.iasValidity,iasFomJson.iasValidity)
        
        self.assertEqual(iasValue.dependentsFullRuningIds,iasFomJson.dependentsFullRuningIds)
        self.assertEqual(iasValue.props,iasFomJson.props)
        
        
        self.assertEqual(iasValue.pluginProductionTStampStr,iasFomJson.pluginProductionTStampStr)
        self.assertEqual(iasValue.pluginProductionTStamp,iasFomJson.pluginProductionTStamp)
        self.assertEqual(iasValue.sentToConverterTStampStr,iasFomJson.sentToConverterTStampStr)
        self.assertEqual(iasValue.sentToConverterTStamp,iasFomJson.sentToConverterTStamp)
        self.assertEqual(iasValue.receivedFromPluginTStampStr,iasFomJson.receivedFromPluginTStampStr)
        self.assertEqual(iasValue.receivedFromPluginTStamp,iasFomJson.receivedFromPluginTStamp)
        self.assertEqual(iasValue.convertedProductionTStampStr,iasFomJson.convertedProductionTStampStr)
        self.assertEqual(iasValue.convertedProductionTStamp,iasFomJson.convertedProductionTStamp)
        self.assertEqual(iasValue.sentToBsdbTStampStr,iasFomJson.sentToBsdbTStampStr)
        self.assertEqual(iasValue.sentToBsdbTStamp,iasFomJson.sentToBsdbTStamp)
        self.assertEqual(iasValue.readFromBsdbTStampStr,iasFomJson.readFromBsdbTStampStr)
        self.assertEqual(iasValue.readFromBsdbTStamp,iasFomJson.readFromBsdbTStamp)
        self.assertEqual(iasValue.dasuProductionTStampStr,iasFomJson.dasuProductionTStampStr)
        self.assertEqual(iasValue.dasuProductionTStamp,iasFomJson.dasuProductionTStamp)
        
        #### Same test with the other JSON string
        
        iasValue2 = IasValue.fromJSon(self.jSonStr2)
        iasValueJson2 = iasValue2.toJSonString()
        iasFomJson2 = IasValue.fromJSon(iasValueJson2)
        
        self.assertEqual(iasValue2.value,iasFomJson2.value)
        self.assertEqual(iasValue2.valueTypeStr,iasFomJson2.valueTypeStr)
        self.assertEqual(iasValue2.valueType,iasFomJson2.valueType)
        self.assertEqual(iasValue2.fullRunningId,iasFomJson2.fullRunningId)
        self.assertEqual(iasValue2.modeStr,iasFomJson2.modeStr)
        self.assertEqual(iasValue2.mode,iasFomJson2.mode)
        self.assertEqual(iasValue2.iasValidityStr,iasFomJson2.iasValidityStr)
        self.assertEqual(iasValue2.iasValidity,iasFomJson2.iasValidity)
        
        self.assertEqual(iasValue2.dependentsFullRuningIds,iasFomJson2.dependentsFullRuningIds)
        self.assertEqual(iasValue2.props,iasFomJson2.props)
        
        self.assertEqual(iasValue2.pluginProductionTStampStr,iasFomJson2.pluginProductionTStampStr)
        self.assertEqual(iasValue2.pluginProductionTStamp,iasFomJson2.pluginProductionTStamp)
        self.assertEqual(iasValue2.sentToConverterTStampStr,iasFomJson2.sentToConverterTStampStr)
        self.assertEqual(iasValue2.sentToConverterTStamp,iasFomJson2.sentToConverterTStamp)
        self.assertEqual(iasValue2.receivedFromPluginTStampStr,iasFomJson2.receivedFromPluginTStampStr)
        self.assertEqual(iasValue2.receivedFromPluginTStamp,iasFomJson2.receivedFromPluginTStamp)
        self.assertEqual(iasValue2.convertedProductionTStampStr,iasFomJson2.convertedProductionTStampStr)
        self.assertEqual(iasValue2.convertedProductionTStamp,iasFomJson2.convertedProductionTStamp)
        self.assertEqual(iasValue2.sentToBsdbTStampStr,iasFomJson2.sentToBsdbTStampStr)
        self.assertEqual(iasValue2.sentToBsdbTStamp,iasFomJson2.sentToBsdbTStamp)
        self.assertEqual(iasValue2.readFromBsdbTStampStr,iasFomJson2.readFromBsdbTStampStr)
        self.assertEqual(iasValue2.readFromBsdbTStamp,iasFomJson2.readFromBsdbTStamp)
        self.assertEqual(iasValue2.dasuProductionTStampStr,iasFomJson2.dasuProductionTStampStr)
        self.assertEqual(iasValue2.dasuProductionTStamp,iasFomJson2.dasuProductionTStamp)
        
if __name__ == "__main__":
    logger=Log.initLogging(__file__)
    logger.info("Start main")
    unittest.main()