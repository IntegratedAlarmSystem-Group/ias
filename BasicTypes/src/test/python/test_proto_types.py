#! /usr/bin/env python3

import sys
import os

python_version = f"python{sys.version_info.major}.{sys.version_info.minor}"
build_lib = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))),
    'build', 'lib', python_version, 'site-packages'
)
if build_lib not in sys.path:
    sys.path.insert(0, build_lib)

from IasBasicTypes.IasType import IASType
from IasBasicTypes.Validity import Validity
from IasBasicTypes.OperationalMode import OperationalMode
from IasBasicTypes.AlarmState import AlarmState
from IasBasicTypes.Priority import Priority
from IasProtobufTypes import types_pb2


class TestProtoTypes():

    def test_iastype_count(self):
        assert len(list(IASType)) == 13

    def test_iastype_mapping(self):
        ias_to_proto = {
            IASType.LONG: types_pb2.IASTYPE_LONG,
            IASType.INT: types_pb2.IASTYPE_INT,
            IASType.SHORT: types_pb2.IASTYPE_SHORT,
            IASType.BYTE: types_pb2.IASTYPE_BYTE,
            IASType.DOUBLE: types_pb2.IASTYPE_DOUBLE,
            IASType.FLOAT: types_pb2.IASTYPE_FLOAT,
            IASType.BOOLEAN: types_pb2.IASTYPE_BOOLEAN,
            IASType.CHAR: types_pb2.IASTYPE_CHAR,
            IASType.STRING: types_pb2.IASTYPE_STRING,
            IASType.TIMESTAMP: types_pb2.IASTYPE_TIMESTAMP,
            IASType.ARRAYOFDOUBLES: types_pb2.IASTYPE_ARRAYOFDOUBLES,
            IASType.ARRAYOFLONGS: types_pb2.IASTYPE_ARRAYOFLONGS,
            IASType.ALARM: types_pb2.IASTYPE_ALARM,
        }
        assert len(ias_to_proto) == len(list(IASType))
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}

        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias

    def test_validity_mapping(self):
        ias_to_proto = {
            Validity.RELIABLE: types_pb2.VALIDITY_RELIABLE,
            Validity.UNRELIABLE: types_pb2.VALIDITY_UNRELIABLE,
        }
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}
        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias

    def test_operationalmode_mapping(self):
        assert len(list(OperationalMode)) == 9
        ias_to_proto = {
            OperationalMode.CLOSING: types_pb2.OPERATIONALMODE_CLOSING,
            OperationalMode.DEGRADED: types_pb2.OPERATIONALMODE_DEGRADED,
            OperationalMode.INITIALIZATION: types_pb2.OPERATIONALMODE_INITIALIZATION,
            OperationalMode.MAINTENANCE: types_pb2.OPERATIONALMODE_MAINTENANCE,
            OperationalMode.OPERATIONAL: types_pb2.OPERATIONALMODE_OPERATIONAL,
            OperationalMode.SHUTTEDDOWN: types_pb2.OPERATIONALMODE_SHUTTEDDOWN,
            OperationalMode.STARTUP: types_pb2.OPERATIONALMODE_STARTUP,
            OperationalMode.UNKNOWN: types_pb2.OPERATIONALMODE_UNKNOWN,
            OperationalMode.MALFUNCTIONING: types_pb2.OPERATIONALMODE_MALFUNCTIONING,
        }
        assert len(ias_to_proto) == len(list(OperationalMode))
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}
        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias

    def test_alarmstate_mapping(self):
        assert len(list(AlarmState)) == 4
        ias_to_proto = {
            AlarmState.CLEAR_ACK: types_pb2.ALARMSTATE_CLEAR_ACK,
            AlarmState.CLEAR_UNACK: types_pb2.ALARMSTATE_CLEAR_UNACK,
            AlarmState.SET_UNACK: types_pb2.ALARMSTATE_SET_UNACK,
            AlarmState.SET_ACK: types_pb2.ALARMSTATE_SET_ACK,
        }
        assert len(ias_to_proto) == len(list(AlarmState))
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}
        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias

    def test_priority_mapping(self):
        assert len(list(Priority)) == 4
        ias_to_proto = {
            Priority.LOW: types_pb2.PRIORITY_LOW,
            Priority.MEDIUM: types_pb2.PRIORITY_MEDIUM,
            Priority.HIGH: types_pb2.PRIORITY_HIGH,
            Priority.CRITICAL: types_pb2.PRIORITY_CRITICAL,
        }
        assert len(ias_to_proto) == len(list(Priority))
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}
        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias
