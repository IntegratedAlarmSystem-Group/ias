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
            IASType.LONG: types_pb2.IASType.LONG,
            IASType.INT: types_pb2.IASType.INT,
            IASType.SHORT: types_pb2.IASType.SHORT,
            IASType.BYTE: types_pb2.IASType.BYTE,
            IASType.DOUBLE: types_pb2.IASType.DOUBLE,
            IASType.FLOAT: types_pb2.IASType.FLOAT,
            IASType.BOOLEAN: types_pb2.IASType.BOOLEAN,
            IASType.CHAR: types_pb2.IASType.CHAR,
            IASType.STRING: types_pb2.IASType.STRING,
            IASType.TIMESTAMP: types_pb2.IASType.TIMESTAMP,
            IASType.ARRAYOFDOUBLES: types_pb2.IASType.ARRAYOFDOUBLES,
            IASType.ARRAYOFLONGS: types_pb2.IASType.ARRAYOFLONGS,
            IASType.ALARM: types_pb2.IASType.ALARM,
        }
        assert len(ias_to_proto) == len(list(IASType))
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}

        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias

    def test_validity_mapping(self):
        ias_to_proto = {
            Validity.RELIABLE: types_pb2.Validity.RELIABLE,
            Validity.UNRELIABLE: types_pb2.Validity.UNRELIABLE,
        }
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}
        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias

    def test_operationalmode_mapping(self):
        assert len(list(OperationalMode)) == 9
        ias_to_proto = {
            OperationalMode.CLOSING: types_pb2.OperationalMode.CLOSING,
            OperationalMode.DEGRADED: types_pb2.OperationalMode.DEGRADED,
            OperationalMode.INITIALIZATION: types_pb2.OperationalMode.INITIALIZATION,
            OperationalMode.MAINTENANCE: types_pb2.OperationalMode.MAINTENANCE,
            OperationalMode.OPERATIONAL: types_pb2.OperationalMode.OPERATIONAL,
            OperationalMode.SHUTTEDDOWN: types_pb2.OperationalMode.SHUTTEDDOWN,
            OperationalMode.STARTUP: types_pb2.OperationalMode.STARTUP,
            OperationalMode.UNKNOWN: types_pb2.OperationalMode.UNKNOWN,
            OperationalMode.MALFUNCTIONING: types_pb2.OperationalMode.MALFUNCTIONING,
        }
        assert len(ias_to_proto) == len(list(OperationalMode))
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}
        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias

    def test_alarmstate_mapping(self):
        assert len(list(AlarmState)) == 4
        ias_to_proto = {
            AlarmState.CLEAR_ACK: types_pb2.AlarmState.CLEAR_ACK,
            AlarmState.CLEAR_UNACK: types_pb2.AlarmState.CLEAR_UNACK,
            AlarmState.SET_UNACK: types_pb2.AlarmState.SET_UNACK,
            AlarmState.SET_ACK: types_pb2.AlarmState.SET_ACK,
        }
        assert len(ias_to_proto) == len(list(AlarmState))
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}
        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias

    def test_priority_mapping(self):
        assert len(list(Priority)) == 4
        ias_to_proto = {
            Priority.LOW: types_pb2.Priority.LOW,
            Priority.MEDIUM: types_pb2.Priority.MEDIUM,
            Priority.HIGH: types_pb2.Priority.HIGH,
            Priority.CRITICAL: types_pb2.Priority.CRITICAL,
        }
        assert len(ias_to_proto) == len(list(Priority))
        proto_to_ias = {v: k for k, v in ias_to_proto.items()}
        for ias, proto in ias_to_proto.items():
            assert proto_to_ias[proto] == ias
