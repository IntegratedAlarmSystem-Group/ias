#! /usr/bin/env python3
'''
Created on Jun 8, 2018

@author: acaproni
'''
from IasBasicTypes.Identifier import Identifier
from IASLogging.logConf import Log
from IasBasicTypes.IasType import IASType
from IasBasicTypes.IasValue import IasValue
from IasBasicTypes.Iso8601TStamp import Iso8601TStamp
from IasBasicTypes.OperationalMode import OperationalMode
from IasBasicTypes.Validity import Validity
from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.AlarmState import AlarmState
from IasBasicTypes.Priority import Priority

class TestIasValue():

    # A JSON string for testing
    jSonStr = """{"value":"CLEAR_ACK:MEDIUM","sentToBsdbTStamp":"2018-03-07T13:08:43.525","productionTStamp":"2018-03-07T13:08:43.524",
            "depsFullRunningIds":["(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature3:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature2:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature4:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature1:IASIO)"],
            "mode":"UNKNOWN", "iasValidity":"RELIABLE",
            "fullRunningId":"(SupervId:SUPERVISOR)@(DasuWith7ASCEs:DASU)@(ASCE-AlarmsThreshold:ASCE)@(TooManyHighTempAlarm:IASIO)","valueType":"ALARM",
            "props":{"key1":"value1","key2":"value2"}}"""
            
            
    jSonStr2 = """{"value":"SET_UNACK:HIGH","productionTStamp":"1970-01-01T00:00:00.1",
            "sentToConverterTStamp":"1970-01-01T00:00:00.2", "receivedFromPluginTStamp":"1970-01-01T00:00:00.3",
            "convertedProductionTStamp":"1970-01-01T00:00:00.4","sentToBsdbTStamp":"1970-01-01T00:00:00.5",
            "readFromBsdbTStamp":"1970-01-01T00:00:00.6",
            "depsFullRunningIds":["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)","(SupervId2:SUPERVISOR)@(dasuVID2:DASU)@(asceVID2:ASCE)@(AlarmID2:IASIO)"],
            "mode":"DEGRADED","iasValidity":"RELIABLE",
            "fullRunningId":"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)",
            "valueType":"ALARM"}"""

    jSonStrTimestamp = """{"value":"2018-05-09T16:15:05","productionTStamp":"1970-01-01T00:00:00.1",
            "sentToConverterTStamp":"1970-01-01T00:00:00.2", "receivedFromPluginTStamp":"1970-01-01T00:00:00.3",
            "convertedProductionTStamp":"1970-01-01T00:00:00.4","sentToBsdbTStamp":"1970-01-01T00:00:00.5",
            "readFromBsdbTStamp":"1970-01-01T00:00:00.6",
            "depsFullRunningIds":["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)"],
            "mode":"OPERATIONAL","iasValidity":"RELIABLE",
            "fullRunningId":"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(Timestamp-ID:IASIO)",
            "valueType":"TIMESTAMP"}"""

    jSonStrArrayOfLongs = """{"value":"[1, 2, 3, 4]","productionTStamp":"1970-01-01T00:00:00.1",
            "sentToConverterTStamp":"1970-01-01T00:00:00.2", "receivedFromPluginTStamp":"1970-01-01T00:00:00.3",
            "convertedProductionTStamp":"1970-01-01T00:00:00.4","sentToBsdbTStamp":"1970-01-01T00:00:00.5",
            "readFromBsdbTStamp":"1970-01-01T00:00:00.6",
            "depsFullRunningIds":["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)"],
            "mode":"OPERATIONAL","iasValidity":"RELIABLE",
            "fullRunningId":"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(Timestamp-ID:IASIO)",
            "valueType":"ARRAYOFLONGS"}"""

    jSonStrArrayOfDoubles = """{"value":"[0.123,-99.05,2,3,5,7]","productionTStamp":"1970-01-01T00:00:00.1",
            "sentToConverterTStamp":"1970-01-01T00:00:00.2", "receivedFromPluginTStamp":"1970-01-01T00:00:00.3",
            "convertedProductionTStamp":"1970-01-01T00:00:00.4","sentToBsdbTStamp":"1970-01-01T00:00:00.5",
            "readFromBsdbTStamp":"1970-01-01T00:00:00.6",
            "depsFullRunningIds":["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)"],
            "mode":"OPERATIONAL","iasValidity":"RELIABLE",
            "fullRunningId":"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(Timestamp-ID:IASIO)",
            "valueType":"ARRAYOFDOUBLES"}"""
    
    def test_build(self):
        """
        Test the build inf IasValues with the IasValue.build() factory method.
        """
        val = "15.6"
        value_type = IASType.DOUBLE
        id = Identifier.from_string("(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(Temperature-ID:IASIO)")
        mode = OperationalMode.MAINTENANCE
        validity = Validity.UNRELIABLE

        ias_value = IasValue.build(
            value=val,
            value_type=value_type,
            fr_id=id,
            validity=validity,
            mode=mode)
        assert ias_value.value == val
        assert ias_value.valueType == value_type
        assert ias_value.fullRunningId == str(id)
        assert ias_value.mode == mode
        assert ias_value.iasValidity == validity

        assert ias_value.readFromMonSysTStamp is not None
        assert ias_value.productionTStamp is not None
        assert ias_value.sentToConverterTStamp is not None
        assert ias_value.receivedFromPluginTStamp is not None
        assert ias_value.convertedProductionTStamp is not None
        assert ias_value.sentToBsdbTStamp is not None

        val = "16"
        value_type = IASType.LONG
        id = Identifier.from_string("(SupervId:SUPERVISOR)@(DasuWith7ASCEs:DASU)@(ASCE-AlarmsThreshold:ASCE)@(TooManyHighTempAlarm:IASIO)")
        mode = OperationalMode.DEGRADED
        validity = Validity.RELIABLE
        props = {"key1":"value1","key2":"value2"}

        ias_value = IasValue.build(
            value=val,
            value_type=value_type,
            fr_id=id,
            validity=validity,
            mode=mode,
            props=props)
        assert ias_value.value == val
        assert ias_value.valueType == value_type
        assert ias_value.fullRunningId == str(id)
        assert ias_value.mode == mode
        assert ias_value.iasValidity == validity
        assert ias_value.props == props

        assert ias_value.readFromMonSysTStamp is None
        assert ias_value.productionTStamp is not None
        assert ias_value.sentToConverterTStamp is None
        assert ias_value.receivedFromPluginTStamp is None
        assert ias_value.convertedProductionTStamp is None
        assert ias_value.sentToBsdbTStamp is not None

    def test_build_alarm(self):
        """
        Test the build_alarm factory method of IasValue.
        """
        alarm_state = AlarmState.SET_UNACK
        alarm_priority = Priority.HIGH
        id = Identifier.from_string("(SupervId:SUPERVISOR)@(DasuWith7ASCEs:DASU)@(ASCE-AlarmsThreshold:ASCE)@(TooManyHighTempAlarm:IASIO)")
        mode = OperationalMode.OPERATIONAL
        validity = Validity.RELIABLE

        ias_value = IasValue.build_alarm(
            alarm_state=alarm_state,
            alarm_priority=alarm_priority,
            fr_id=id,
            validity=validity,
            mode=mode)
        
        assert ias_value.value == f"{alarm_state.name}:{alarm_priority.name}"
        assert ias_value.valueType == IASType.ALARM
        assert ias_value.fullRunningId == str(id)
        assert ias_value.mode == mode
        assert ias_value.iasValidity == validity

    def test_name(self):
        iasValue = IasValue.fromJSon(self.jSonStr)
        
        assert iasValue.id == "TooManyHighTempAlarm"
        
        expectedProps= {"key1":"value1","key2":"value2"}
        expectedDeps = ["(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature3:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature2:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature4:IASIO)",
            "(MonitoredSystemID:MONITORED_SOFTWARE_SYSTEM)@(PluginID:PLUGIN)@(ConverterID:CONVERTER)@(Temperature1:IASIO)"]
        
        
        assert iasValue.value=="CLEAR_ACK:MEDIUM"
        assert iasValue.valueTypeStr=="ALARM"
        assert iasValue.valueType == IASType.ALARM
        assert iasValue.fullRunningId == "(SupervId:SUPERVISOR)@(DasuWith7ASCEs:DASU)@(ASCE-AlarmsThreshold:ASCE)@(TooManyHighTempAlarm:IASIO)"
        assert iasValue.id == "TooManyHighTempAlarm"
        assert iasValue.dependentsFullRuningIds == expectedDeps
        assert iasValue.modeStr =="UNKNOWN"
        assert iasValue.mode ==OperationalMode.UNKNOWN
        assert iasValue.iasValidityStr == "RELIABLE"
        assert iasValue.iasValidity == Validity.RELIABLE
        assert iasValue.props == expectedProps
    
        assert iasValue.sentToConverterTStampStr == None
        assert iasValue.receivedFromPluginTStampStr == None
        assert iasValue.convertedProductionTStampStr == None
        assert iasValue.sentToBsdbTStampStr == "2018-03-07T13:08:43.525"
        assert iasValue.readFromBsdbTStampStr == None
        assert iasValue.productionTStampStr == "2018-03-07T13:08:43.524"
        
        expectedDeps2 = ["(SupervId1:SUPERVISOR)@(dasuVID1:DASU)@(asceVID1:ASCE)@(AlarmID1:IASIO)","(SupervId2:SUPERVISOR)@(dasuVID2:DASU)@(asceVID2:ASCE)@(AlarmID2:IASIO)"]
        
        iasValue2 = IasValue.fromJSon(self.jSonStr2)
        assert iasValue2.value == "SET_UNACK:HIGH"
        alarm = Alarm.fromString(iasValue2.value)
        assert alarm.alarmState == AlarmState.SET_UNACK
        assert alarm.priority == Priority.HIGH
        assert iasValue2.valueTypeStr == "ALARM"
        assert iasValue2.valueType == IASType.ALARM
        assert iasValue2.fullRunningId == "(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)"
        assert iasValue2.id == "AlarmType-ID"
        assert iasValue2.dependentsFullRuningIds == expectedDeps2
        assert iasValue2.modeStr == "DEGRADED"
        assert iasValue2.mode == OperationalMode.DEGRADED
        assert iasValue2.iasValidityStr == "RELIABLE"
        assert iasValue2.iasValidity == Validity.RELIABLE
        assert iasValue2.props == None
    
        assert iasValue2.sentToConverterTStampStr == "1970-01-01T00:00:00.2"
        assert iasValue2.sentToConverterTStamp == Iso8601TStamp.Iso8601ToDatetime(iasValue2.sentToConverterTStampStr)
        
        assert iasValue2.receivedFromPluginTStampStr == "1970-01-01T00:00:00.3"
        assert iasValue2.receivedFromPluginTStamp == Iso8601TStamp.Iso8601ToDatetime(iasValue2.receivedFromPluginTStampStr)
        
        assert iasValue2.convertedProductionTStampStr == "1970-01-01T00:00:00.4"
        assert iasValue2.convertedProductionTStamp == Iso8601TStamp.Iso8601ToDatetime(iasValue2.convertedProductionTStampStr)
        
        assert iasValue2.sentToBsdbTStampStr == "1970-01-01T00:00:00.5"
        assert iasValue2.sentToBsdbTStamp == Iso8601TStamp.Iso8601ToDatetime(iasValue2.sentToBsdbTStampStr)
        
        assert iasValue2.readFromBsdbTStampStr == "1970-01-01T00:00:00.6"
        assert iasValue2.readFromBsdbTStamp == Iso8601TStamp.Iso8601ToDatetime(iasValue2.readFromBsdbTStampStr)
        
        assert iasValue2.productionTStampStr == "1970-01-01T00:00:00.1"
        assert iasValue2.productionTStamp == Iso8601TStamp.Iso8601ToDatetime(iasValue2.productionTStampStr)
    
    def test_to_json(self):
        iasValue = IasValue.fromJSon(self.jSonStr)
        
        iasValueJson = iasValue.toJSonString()
        iasFomJson = IasValue.fromJSon(iasValueJson)
        
        assert iasValue.value == iasFomJson.value
        assert iasValue.valueTypeStr == iasFomJson.valueTypeStr
        assert iasValue.valueType == iasFomJson.valueType
        assert iasValue.fullRunningId == iasFomJson.fullRunningId
        assert iasValue.modeStr == iasFomJson.modeStr
        assert iasValue.mode == iasFomJson.mode
        assert iasValue.iasValidityStr == iasFomJson.iasValidityStr
        assert iasValue.iasValidity == iasFomJson.iasValidity
        
        assert iasValue.dependentsFullRuningIds == iasFomJson.dependentsFullRuningIds
        assert iasValue.props == iasFomJson.props
        
        assert iasValue.sentToConverterTStampStr == iasFomJson.sentToConverterTStampStr
        assert iasValue.sentToConverterTStamp == iasFomJson.sentToConverterTStamp
        assert iasValue.receivedFromPluginTStampStr == iasFomJson.receivedFromPluginTStampStr
        assert iasValue.receivedFromPluginTStamp == iasFomJson.receivedFromPluginTStamp
        assert iasValue.convertedProductionTStampStr == iasFomJson.convertedProductionTStampStr
        assert iasValue.convertedProductionTStamp == iasFomJson.convertedProductionTStamp
        assert iasValue.sentToBsdbTStampStr == iasFomJson.sentToBsdbTStampStr
        assert iasValue.sentToBsdbTStamp == iasFomJson.sentToBsdbTStamp
        assert iasValue.readFromBsdbTStampStr == iasFomJson.readFromBsdbTStampStr
        assert iasValue.readFromBsdbTStamp == iasFomJson.readFromBsdbTStamp
        assert iasValue.productionTStampStr == iasFomJson.productionTStampStr
        assert iasValue.productionTStamp == iasFomJson.productionTStamp
        
        #### Same test with the other JSON string
        
        iasValue2 = IasValue.fromJSon(self.jSonStr2)
        iasValueJson2 = iasValue2.toJSonString()
        iasFomJson2 = IasValue.fromJSon(iasValueJson2)
        
        assert iasValue2.value == iasFomJson2.value
        assert iasValue2.valueTypeStr == iasFomJson2.valueTypeStr
        assert iasValue2.valueType == iasFomJson2.valueType
        assert iasValue2.fullRunningId == iasFomJson2.fullRunningId
        assert iasValue2.modeStr == iasFomJson2.modeStr
        assert iasValue2.mode == iasFomJson2.mode
        assert iasValue2.iasValidityStr == iasFomJson2.iasValidityStr
        assert iasValue2.iasValidity == iasFomJson2.iasValidity
        
        assert iasValue2.dependentsFullRuningIds == iasFomJson2.dependentsFullRuningIds
        assert iasValue2.props == iasFomJson2.props
        
        assert iasValue2.sentToConverterTStampStr == iasFomJson2.sentToConverterTStampStr
        assert iasValue2.sentToConverterTStamp == iasFomJson2.sentToConverterTStamp
        assert iasValue2.receivedFromPluginTStampStr == iasFomJson2.receivedFromPluginTStampStr
        assert iasValue2.receivedFromPluginTStamp == iasFomJson2.receivedFromPluginTStamp
        assert iasValue2.convertedProductionTStampStr == iasFomJson2.convertedProductionTStampStr
        assert iasValue2.convertedProductionTStamp == iasFomJson2.convertedProductionTStamp
        assert iasValue2.sentToBsdbTStampStr == iasFomJson2.sentToBsdbTStampStr
        assert iasValue2.sentToBsdbTStamp == iasFomJson2.sentToBsdbTStamp
        assert iasValue2.readFromBsdbTStampStr == iasFomJson2.readFromBsdbTStampStr
        assert iasValue2.readFromBsdbTStamp == iasFomJson2.readFromBsdbTStamp
        assert iasValue2.productionTStampStr == iasFomJson2.productionTStampStr
        assert iasValue2.productionTStamp == iasFomJson2.productionTStamp

    def test_timestamp(self):
        '''
        Test the TIMESTAMP type
        '''
        iasValue = IasValue.fromJSon(self.jSonStrTimestamp)

        iasValueJson = iasValue.toJSonString()
        iasFomJson = IasValue.fromJSon(iasValueJson)
        assert iasValue.value == iasFomJson.value

    def test_array_of_longs(self):
        '''
        Test the ARRAYOFLONGS type
        '''
        iasValue = IasValue.fromJSon(self.jSonStrArrayOfLongs)

        iasValueJson = iasValue.toJSonString()
        iasFomJson = IasValue.fromJSon(iasValueJson)
        assert iasValue.value == iasFomJson.value

    def test_array_of_doubles(self):
        '''
        Test the ARRAYOFDOUBLES type
        '''
        iasValue = IasValue.fromJSon(self.jSonStrArrayOfDoubles)

        iasValueJson = iasValue.toJSonString()
        iasFomJson = IasValue.fromJSon(iasValueJson)
        assert iasValue.value == iasFomJson.value
