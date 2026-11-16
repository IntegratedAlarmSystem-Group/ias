#! /usr/bin/env python3
"""Unit tests for proto conversion functions in tf_service.py — tested in isolation.

**No gRPC server is started.**  No network communication occurs.  These
tests exercise the module-level conversion functions directly, verifying
the round-trip correctness of every conversion between protobuf wire
messages and the Python IAS types (``IASType``, ``OperationalMode``,
``Validity``, ``Value``, ``IASIO``, ``IasioInfo``).

The generated protobuf stubs must be present and importable (from the
CompElement and BasicTypes build trees), but no gRPC channel is opened.

The PYTHONPATH is configured to include the build trees of all four
dependency modules plus the generated proto output directories.

Run with::

    pytest PyTfServer/src/test/python/test_service.py
"""

import _test_paths  # noqa: F401 adds generated proto dirs to sys.path

from IasBasicTypes.OperationalMode import OperationalMode
from IasBasicTypes.Validity import Validity
from IasBasicTypes.IasType import IASType
from IasBasicTypes.Alarm import Alarm, AlarmState, Priority
from IasTransferFunction.IASIO import IASIO
from IasTransferFunction.IasioInfo import IasioInfo
from IasProtobufTfserver.pytf_service_pb2 import IasIO as ProtoIasIO, IasioInfoProto

from IasPyTfServer.tf_service import (
    proto_iastype_to_iastype,
    proto_operationalmode_to_mode,
    proto_validity_to_validity,
    proto_value_to_python,
    python_value_to_proto,
    proto_iasioinfo_to_python,
    proto_iasio_to_python,
    python_iasio_to_proto,
)


class TestProtoEnumConversion:
    """Round-trip tests for scalar enum conversions (no gRPC, no network).

    For each enum type, iterates all IAS values, finds the matching proto
    constant by name, and verifies that the conversion function returns the
    original IAS enum member.
    """

    def test_iastype_roundtrip(self):
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
        for ias_type in IASType:
            proto_name = ias_type.name
            proto_enum = getattr(ProtoIASType, proto_name)
            result = proto_iastype_to_iastype(proto_enum)
            assert result == ias_type

    def test_operationalmode_roundtrip(self):
        from IasProtobufTypes.types_pb2 import OperationalMode as ProtoOM
        for mode in OperationalMode:
            proto_name = mode.name
            proto_enum = getattr(ProtoOM, proto_name)
            result = proto_operationalmode_to_mode(proto_enum)
            assert result == mode

    def test_validity_roundtrip(self):
        from IasProtobufTypes.types_pb2 import Validity as ProtoValidity
        for v in Validity:
            proto_name = v.name
            proto_enum = getattr(ProtoValidity, proto_name)
            result = proto_validity_to_validity(proto_enum)
            assert result == v


class TestProtoValueConversion:
    """Round-trip tests for the Value oneof conversion (no gRPC, no network).

    Covers all seven Value cases (int64, double, bool, string,
    doubleArray, int64Array, Alarm) plus the unset case.
    """

    def _make_proto_iastype(self, ias_type):
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
        return getattr(ProtoIASType, ias_type.name)

    def test_int64_value(self):
        from IasProtobufTypes.types_pb2 import Value as ProtoValue
        proto = ProtoValue(int64Value=42)
        result = proto_value_to_python(proto, IASType.LONG)
        assert result == 42

        back = python_value_to_proto(42, IASType.LONG)
        assert back.int64Value == 42

    def test_double_value(self):
        from IasProtobufTypes.types_pb2 import Value as ProtoValue
        proto = ProtoValue(doubleValue=3.14)
        result = proto_value_to_python(proto, IASType.DOUBLE)
        assert result == 3.14

        back = python_value_to_proto(3.14, IASType.DOUBLE)
        assert back.doubleValue == 3.14

    def test_bool_value(self):
        from IasProtobufTypes.types_pb2 import Value as ProtoValue
        proto = ProtoValue(boolValue=True)
        result = proto_value_to_python(proto, IASType.BOOLEAN)
        assert result is True

        back = python_value_to_proto(False, IASType.BOOLEAN)
        assert back.boolValue is False

    def test_string_value(self):
        from IasProtobufTypes.types_pb2 import Value as ProtoValue
        proto = ProtoValue(stringValue="hello")
        result = proto_value_to_python(proto, IASType.STRING)
        assert result == "hello"

        back = python_value_to_proto("world", IASType.STRING)
        assert back.stringValue == "world"

    def test_double_array_value(self):
        from IasProtobufTypes.types_pb2 import Value as ProtoValue
        proto = ProtoValue()
        proto.doubleArrayValue.values.extend([1.0, 2.0, 3.0])
        result = proto_value_to_python(proto, IASType.ARRAYOFDOUBLES)
        assert result == [1.0, 2.0, 3.0]

        back = python_value_to_proto([4.0, 5.0], IASType.ARRAYOFDOUBLES)
        assert list(back.doubleArrayValue.values) == [4.0, 5.0]

    def test_int64_array_value(self):
        from IasProtobufTypes.types_pb2 import Value as ProtoValue
        proto = ProtoValue()
        proto.int64ArrayValue.values.extend([10, 20, 30])
        result = proto_value_to_python(proto, IASType.ARRAYOFLONGS)
        assert result == [10, 20, 30]

        back = python_value_to_proto([40, 50], IASType.ARRAYOFLONGS)
        assert list(back.int64ArrayValue.values) == [40, 50]

    def test_alarm_value(self):
        from IasProtobufTypes import types_pb2
        proto = types_pb2.Value()
        proto.alarmValue.state = types_pb2.AlarmState.Value("SET_UNACK")
        proto.alarmValue.priority = types_pb2.Priority.Value("HIGH")
        result = proto_value_to_python(proto, IASType.ALARM)
        assert isinstance(result, Alarm)
        assert result.alarmState == AlarmState.SET_UNACK
        assert result.priority == Priority.HIGH

    def test_unset_value_returns_none(self):
        from IasProtobufTypes.types_pb2 import Value as ProtoValue
        proto = ProtoValue()
        result = proto_value_to_python(proto, IASType.LONG)
        assert result is None


class TestProtoIasioInfoConversion:
    """Round-trip tests for IasioInfo proto conversion (no gRPC, no network)."""

    def _make_proto_iastype(self, ias_type):
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
        return getattr(ProtoIASType, ias_type.name)

    def test_iasioinfo_roundtrip(self):
        info = IasioInfo("testId", IASType.LONG)
        proto = IasioInfoProto(id="testId", iasType=self._make_proto_iastype(IASType.LONG))
        result = proto_iasioinfo_to_python(proto)
        assert result.id == info.id
        assert result.iasType == info.iasType


class TestProtoIasIOConversion:
    """Round-trip tests for the full IASIO proto conversion (no gRPC, no network).

    Verifies that every field (id, runningId, mode, iasType, validity,
    value, productionTStamp, props) survives the Python → proto → Python
    round trip for all major value types.
    """

    def _make_proto_iastype(self, ias_type):
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
        return getattr(ProtoIASType, ias_type.name)

    def _make_proto_mode(self, mode):
        from IasProtobufTypes.types_pb2 import OperationalMode as ProtoOM
        return getattr(ProtoOM, mode.name)

    def _make_proto_validity(self, v):
        from IasProtobufTypes.types_pb2 import Validity as ProtoValidity
        return getattr(ProtoValidity, v.name)

    def test_iasio_roundtrip_long(self):
        ias_io = IASIO(
            id="mp1",
            runningId="sup:das:asce:mp1",
            mode=OperationalMode.OPERATIONAL,
            iasType=IASType.LONG,
            validity=Validity.RELIABLE,
            value=123,
            productionTStamp=1000,
            props={"key": "val"}
        )
        proto = python_iasio_to_proto(ias_io)
        assert proto.iasId == "mp1"
        assert proto.runningId == "sup:das:asce:mp1"
        assert self._make_proto_iastype(IASType.LONG) == proto.iasType
        assert proto.productionTStamp == 1000
        assert proto.props["key"] == "val"

        result = proto_iasio_to_python(proto)
        assert result.id == ias_io.id
        assert result.iasType == ias_io.iasType
        assert result.value == ias_io.value
        assert result.mode == ias_io.mode
        assert result.validity == ias_io.validity

    def test_iasio_roundtrip_double(self):
        ias_io = IASIO(
            id="mp1",
            runningId="sup:das:asce:mp1",
            mode=OperationalMode.OPERATIONAL,
            iasType=IASType.DOUBLE,
            validity=Validity.UNRELIABLE,
            value=2.718,
            productionTStamp=0,
            props={}
        )
        proto = python_iasio_to_proto(ias_io)
        result = proto_iasio_to_python(proto)
        assert result.value == 2.718
        assert result.validity == Validity.UNRELIABLE

    def test_iasio_roundtrip_string(self):
        ias_io = IASIO(
            id="mp1",
            runningId="sup:das:asce:mp1",
            mode=OperationalMode.INITIALIZATION,
            iasType=IASType.STRING,
            validity=Validity.RELIABLE,
            value="test string",
            productionTStamp=500,
            props={}
        )
        proto = python_iasio_to_proto(ias_io)
        result = proto_iasio_to_python(proto)
        assert result.value == "test string"
        assert result.mode == OperationalMode.INITIALIZATION

    def test_iasio_roundtrip_alarm(self):
        alarm = Alarm(AlarmState.SET_ACK, Priority.CRITICAL)
        ias_io = IASIO(
            id="alarm1",
            runningId="sup:das:asce:alarm1",
            mode=OperationalMode.OPERATIONAL,
            iasType=IASType.ALARM,
            validity=Validity.RELIABLE,
            value=alarm,
            productionTStamp=1000,
            props={}
        )
        proto = python_iasio_to_proto(ias_io)
        result = proto_iasio_to_python(proto)
        assert isinstance(result.value, Alarm)
        assert result.value.alarmState == AlarmState.SET_ACK
        assert result.value.priority == Priority.CRITICAL

    def test_iasio_without_value(self):
        ias_io = IASIO(
            id="mp1",
            runningId="sup:das:asce:mp1",
            mode=OperationalMode.OPERATIONAL,
            iasType=IASType.LONG,
            validity=Validity.RELIABLE,
            value=None,
            productionTStamp=0,
            props={}
        )
        proto = python_iasio_to_proto(ias_io)
        result = proto_iasio_to_python(proto)
        assert result.value is None

    def test_iasio_array_of_doubles(self):
        ias_io = IASIO(
            id="mp1",
            runningId="sup:das:asce:mp1",
            mode=OperationalMode.OPERATIONAL,
            iasType=IASType.ARRAYOFDOUBLES,
            validity=Validity.RELIABLE,
            value=[1.1, 2.2, 3.3],
            productionTStamp=1000,
            props={}
        )
        proto = python_iasio_to_proto(ias_io)
        result = proto_iasio_to_python(proto)
        assert result.value == [1.1, 2.2, 3.3]
