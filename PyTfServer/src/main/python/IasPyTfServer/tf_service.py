"""gRPC service implementation for the PyTfServer.

This module converts between protobuf messages (wire format) and the Python
IAS types that TransferFunctions operate on, and implements the three RPCs
defined by the ``PyTfService`` proto: *Initialize*, *Eval*, and *Shutdown*.

Key design decision: proto enum wire values do not match the semantic integer
values of the IAS enums. All conversions go through **name** lookup
(e.g. ``ProtoIASType.Name(wire_val)`` → ``IASType`` by ``.name``) to
preserve correctness across the boundary.
"""

import logging

from IasBasicTypes.IasType import IASType
from IasBasicTypes.OperationalMode import OperationalMode
from IasBasicTypes.Validity import Validity
from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.AlarmState import AlarmState
from IasBasicTypes.Priority import Priority
from IasTransferFunction.IASIO import IASIO
from IasTransferFunction.IasioInfo import IasioInfo

from IasProtobufTfserver.pytf_service_pb2 import (
    PingResponse, InitializeResponse, EvalResponse, ShutdownResponse
)
from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceServicer
from IasProtobufTypes import types_pb2
from IasProtobufTypes.types_pb2 import (
    Value as ProtoValue,
    Alarm as ProtoAlarm,
    IASType as ProtoIASType,
    OperationalMode as ProtoOperationalMode,
    Validity as ProtoValidity,
)

from IasPyTfServer.tf_manager import TfManager

logger = logging.getLogger(__name__)


# --- Scalar enum conversions (proto wire value → Python IAS enum) ----------


def proto_iastype_to_iastype(proto_iastype):
    """Convert a proto IASType enum value to its IAS counterpart.

    Proto and IAS enum wires differ, so we match by ``.name``.

    Args:
        proto_iastype: Integer proto enum value (protobuf 4.x).

    Returns:
        The matching ``IASType`` member.

    Raises:
        ValueError: If the proto enum name has no corresponding IASType.
    """
    name = ProtoIASType.Name(proto_iastype)
    for t in IASType:
        if t.name == name:
            return t
    raise ValueError(f"Unknown IASType: {name}")


def proto_operationalmode_to_mode(proto_mode):
    """Convert a proto OperationalMode enum value to its IAS counterpart.

    Args:
        proto_mode: Integer proto enum value.

    Returns:
        The matching ``OperationalMode`` member.

    Raises:
        ValueError: If the proto enum name has no match.
    """
    name = ProtoOperationalMode.Name(proto_mode)
    for m in OperationalMode:
        if m.name == name:
            return m
    raise ValueError(f"Unknown OperationalMode: {name}")


def proto_validity_to_validity(proto_validity):
    """Convert a proto Validity enum value to its IAS counterpart.

    Args:
        proto_validity: Integer proto enum value.

    Returns:
        The matching ``Validity`` member.

    Raises:
        ValueError: If the proto enum name has no match.
    """
    name = ProtoValidity.Name(proto_validity)
    for v in Validity:
        if v.name == name:
            return v
    raise ValueError(f"Unknown Validity: {name}")


# --- Value (oneof) conversions -------------------------------------------


def proto_value_to_python(proto_value: ProtoValue, ias_type: IASType):
    """Deserialize a proto ``Value`` message to a native Python object.

    The ``Value`` proto uses a oneof to hold int64, double, bool, string,
    arrays, or an Alarm. The active case determines how to extract the data.

    Args:
        proto_value: Populated proto ``Value`` message.
        ias_type: The IASType hint (used for Alarm deserialization).

    Returns:
        The Python scalar, list, ``Alarm`` instance, or ``None`` if the
        oneof is not set.
    """
    case = proto_value.WhichOneof('value')
    if case is None:
        return None
    if case == 'int64Value':
        return proto_value.int64Value
    elif case == 'doubleValue':
        return proto_value.doubleValue
    elif case == 'boolValue':
        return proto_value.boolValue
    elif case == 'stringValue':
        return proto_value.stringValue
    elif case == 'doubleArrayValue':
        return list(proto_value.doubleArrayValue.values)
    elif case == 'int64ArrayValue':
        return list(proto_value.int64ArrayValue.values)
    elif case == 'alarmValue':
        state_name = types_pb2.AlarmState.Name(proto_value.alarmValue.state)
        priority_name = types_pb2.Priority.Name(proto_value.alarmValue.priority)
        state = AlarmState.value_of(state_name)
        priority = Priority.value_of(priority_name)
        return Alarm(state, priority)
    return None


def python_value_to_proto(value, ias_type: IASType):
    """Serialize a Python value into a proto ``Value`` message.

    The IASType determines which oneof field of ``Value`` gets populated.

    Args:
        value: The Python scalar, list, or ``Alarm`` object.
        ias_type: IASType indicating the target wire representation.

    Returns:
        A populated ``ProtoValue`` message.
    """
    builder = ProtoValue()
    if ias_type in (IASType.LONG, IASType.INT, IASType.SHORT, IASType.BYTE, IASType.TIMESTAMP):
        builder.int64Value = value
    elif ias_type in (IASType.DOUBLE, IASType.FLOAT):
        builder.doubleValue = value
    elif ias_type == IASType.BOOLEAN:
        builder.boolValue = value
    elif ias_type == IASType.CHAR or ias_type == IASType.STRING:
        builder.stringValue = str(value)
    elif ias_type == IASType.ARRAYOFDOUBLES:
        builder.doubleArrayValue.values.extend(value)
    elif ias_type == IASType.ARRAYOFLONGS:
        builder.int64ArrayValue.values.extend(value)
    elif ias_type == IASType.ALARM:
        proto_alarm = ProtoAlarm()
        proto_alarm.state = types_pb2.AlarmState.Value(value.alarmState.name)
        proto_alarm.priority = types_pb2.Priority.Value(value.priority.name)
        builder.alarmValue.CopyFrom(proto_alarm)
    return builder


# --- IASIO and IasioInfo conversions -------------------------------------


def proto_iasioinfo_to_python(proto_info):
    """Convert a proto IasioInfo message to its Python equivalent.

    Args:
        proto_info: Proto message generated from pytf_service.proto.

    Returns:
        An ``IasioInfo`` instance with the same ``id`` and ``iasType``.
    """
    return IasioInfo(proto_info.id, proto_iastype_to_iastype(proto_info.iasType))


def proto_iasio_to_python(proto_io):
    """Convert a proto IasIO message to a Python ``IASIO`` object.

    Recursively converts type, mode, validity, value (via the oneof
    dispatcher), and the props map.

    Args:
        proto_io: Proto IasIO message from an Evaluate request.

    Returns:
        A fully populated ``IASIO`` instance.
    """
    ias_type = proto_iastype_to_iastype(proto_io.iasType)
    value = proto_value_to_python(proto_io.value, ias_type) if proto_io.HasField('value') else None
    mode = proto_operationalmode_to_mode(proto_io.mode)
    validity = proto_validity_to_validity(proto_io.validity)
    props = dict(proto_io.props) if proto_io.props else {}

    return IASIO(
        id=proto_io.iasId,
        runningId=proto_io.runningId,
        mode=mode,
        iasType=ias_type,
        validity=validity,
        value=value,
        productionTStamp=proto_io.productionTStamp if proto_io.productionTStamp else None,
        props=props
    )


def python_iasio_to_proto(ias_io: IASIO):
    """Convert a Python ``IASIO`` object to a proto IasIO message.

    Enum fields are mapped by name to find the matching proto enum
    constant.  The value oneof is populated via ``python_value_to_proto``.

    Args:
        ias_io: A populated ``IASIO`` instance (typically the result of
            a TF ``eval`` call).

    Returns:
        A proto ``IasIO`` message ready to be serialized in a response.
    """
    from IasProtobufTfserver.pytf_service_pb2 import IasIO as ProtoIasIO
    builder = ProtoIasIO()
    builder.iasId = ias_io.id
    builder.runningId = ias_io.runningId
    builder.mode = getattr(ProtoOperationalMode, ias_io.mode.name, 0)
    builder.iasType = getattr(ProtoIASType, ias_io.iasType.name, 0)
    builder.validity = getattr(ProtoValidity, ias_io.validity.name, 0)
    if ias_io.productionTStamp:
        builder.productionTStamp = ias_io.productionTStamp
    if ias_io.value is not None:
        builder.value.CopyFrom(python_value_to_proto(ias_io.value, ias_io.iasType))
    if ias_io.props:
        builder.props.update(ias_io.props)
    return builder


# --- gRPC service implementation -----------------------------------------


class PyTfService(PyTfServiceServicer):
    """Implements the three PyTfService RPCs.

    Delegates TF lifecycle management to ``TfManager`` and handles
    proto-to-Python conversion in each RPC handler.

    Attributes:
        tf_manager: The :class:`TfManager` instance that tracks per-ASCE
            TransferFunction objects.
    """

    def __init__(self):
        """Initialize the service with a fresh TfManager."""
        self.tf_manager = TfManager()

    def Ping(self, request, context):
        """Handle the Ping RPC: health check for the server.

        Returns ``success=True`` with the number of active ASCE instances
        in the manager.  Useful to verify the server is running before
        invoking Initialize/Eval/Shutdown.

        Args:
            request: Proto PingRequest (message field unused).
            context: The gRPC service context.

        Returns:
            A ``PingResponse`` with ``success=True``.
        """
        active = len(self.tf_manager.tf_instances)
        logger.debug(f"[PyTfServer] Ping received, active ASCEs: {active}")
        return PingResponse(success=True)

    def Initialize(self, request, context):
        """Handle the Initialize RPC: load and register a TF for an ASCE.

        Converts incoming proto IasioInfo messages to Python, extracts the
        optional template instance, then delegates to ``TfManager.initialize``.

        Args:
            request: Proto InitializeRequest containing ASCE identifiers,
                TF class name, constructor props, and I/O metadata.
            context: The gRPC service context.

        Returns:
            An ``InitializeResponse`` with ``success`` and ``error`` fields.
        """
        inputs_info = [proto_iasioinfo_to_python(info) for info in request.inputsInfo]
        output_info = proto_iasioinfo_to_python(request.outputInfo)
        template_instance = request.templateInstance if request.hasTemplateInstance else None

        success, error = self.tf_manager.initialize(
            asceRunningId=request.asceRunningId,
            asceId=request.asceId,
            pythonClassName=request.pythonClassName,
            validityTimeFrame=request.validityTimeFrame,
            props=dict(request.props),
            templateInstance=template_instance,
            inputsInfo=inputs_info,
            outputInfo=output_info
        )
        return InitializeResponse(success=success, error=error)

    def Eval(self, request, context):
        """Handle the Eval RPC: execute an already-initialized TF.

        Converts all input IASIO messages and the actual output to Python
        objects, delegates to ``TfManager.eval``, and converts the result
        back to a proto IasIO on success.

        Args:
            request: Proto EvalRequest with ``asceRunningId``, a map of
                component inputs, and the actual output.
            context: The gRPC service context.

        Returns:
            An ``EvalResponse``.  On success the ``result`` field contains
            the computed output IASIO.
        """
        comp_inputs = {}
        for key, proto_io in request.compInputs.items():
            comp_inputs[key] = proto_iasio_to_python(proto_io)
        actual_output = proto_iasio_to_python(request.actualOutput)

        success, error, result = self.tf_manager.eval(
            asceRunningId=request.asceRunningId,
            compInputs=comp_inputs,
            actualOutput=actual_output
        )

        if success and result is not None:
            proto_result = python_iasio_to_proto(result)
            return EvalResponse(success=True, error="", result=proto_result)
        return EvalResponse(success=success, error=error)

    def Shutdown(self, request, context):
        """Handle the Shutdown RPC: tear down and remove a TF instance.

        Args:
            request: Proto ShutdownRequest with the ``asceRunningId``.
            context: The gRPC service context.

        Returns:
            A ``ShutdownResponse`` with ``success`` and ``error`` fields.
        """
        success, error = self.tf_manager.shutdown(request.asceRunningId)
        return ShutdownResponse(success=success, error=error)
