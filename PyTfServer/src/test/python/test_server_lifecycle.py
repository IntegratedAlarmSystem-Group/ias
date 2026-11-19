#! /usr/bin/env python3
"""Multi-TF routing tests for the PyTfServer.

Verifies that a single in-process server instance correctly maintains
separate TF instances per ``asceRunningId`` and routes ``Eval`` requests
to the right one when multiple TFs are initialized concurrently.

======================== ================== ========
Category                 Requires server? Network?
======================== ================== ========
TestPyTfServerLifecycle   In-process gRPC    No
======================== ================== ========

This file was extracted from ``test_server.py`` to keep the lifecycle
tests in a dedicated module.  It reuses the mock TFs
``PassThroughTF``, ``DoublingTF``, and ``TriplingTF`` from
``test_tf_module``.
"""

import _test_paths  # noqa: F401 adds generated proto dirs to sys.path

import grpc
from concurrent import futures

from IasProtobufTfserver.pytf_service_pb2 import (
    EvalRequest,
    IasIO as ProtoIasIO,
    InitializeRequest,
    ShutdownRequest,
    IasioInfoProto,
)
from IasProtobufTfserver.pytf_service_pb2_grpc import (
    PyTfServiceStub,
    add_PyTfServiceServicer_to_server,
)
from IasProtobufTypes.types_pb2 import (
    IASType as ProtoIASType,
    OperationalMode as ProtoOM,
    Validity as ProtoValidity,
)


def _make_input_io(iasId, runningId, value, iasType=ProtoIASType.LONG):
    """Build a ProtoIasIO input value with the given int64 payload."""
    io = ProtoIasIO(
        iasId=iasId,
        runningId=runningId,
        mode=ProtoOM.OPERATIONAL,
        iasType=iasType,
        validity=ProtoValidity.RELIABLE,
        productionTStamp=1000,
    )
    io.value.int64Value = value
    return io


def _make_actual_out(iasId, runningId, iasType=ProtoIASType.LONG):
    """Build a ProtoIasIO actual output template."""
    return ProtoIasIO(
        iasId=iasId,
        runningId=runningId,
        mode=ProtoOM.OPERATIONAL,
        iasType=iasType,
        validity=ProtoValidity.RELIABLE,
        productionTStamp=1000,
    )


class TestPyTfServerLifecycle:
    """Initialize/Eval/Shutdown lifecycle tests — in-process gRPC.

    Same pattern as ``TestPyTfServer``: servicer registered on an
    in-process ``grpc.server`` on a random localhost port.  Exercises
    full lifecycle, parameter passing, templateInstance handling,
    double-initialize rejection, shutdown cleanup, and multi-TF routing.
    """

    def setup_method(self):
        from IasPyTfServer.tf_service import PyTfService
        self.servicer = PyTfService()
        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        add_PyTfServiceServicer_to_server(self.servicer, self.server)
        self.port = self.server.add_insecure_port("localhost:0")
        self.server.start()
        self.channel = grpc.insecure_channel(f"localhost:{self.port}")

    def teardown_method(self):
        self.channel.close()
        self.server.stop(0)

    def test_initialize_and_shutdown_lifecycle(self):
        """Verify Initialize and Shutdown RPCs via gRPC stub end-to-end."""
        stub = PyTfServiceStub(self.channel)

        init_req = InitializeRequest(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={"key1": "val1"},
            inputsInfo=[IasioInfoProto(id="in1", iasType=ProtoIASType.LONG)],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        )
        resp = stub.Initialize(init_req)
        assert resp.success is True
        assert resp.error == ""

        shutdown_resp = stub.Shutdown(ShutdownRequest(asceRunningId="test:run1"))
        assert shutdown_resp.success is True
        assert shutdown_resp.error == ""

    def test_initialize_verifies_args(self):
        """Verify Initialize passes pythonClassName, asceRunningId, props to the TF."""
        stub = PyTfServiceStub(self.channel)

        init_req = InitializeRequest(
            asceRunningId="verify:run1",
            asceId="verify",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=2000,
            props={"threshold": "42", "name": "mytf"},
            inputsInfo=[],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        )
        resp = stub.Initialize(init_req)
        assert resp.success is True

        tf = self.servicer.tf_manager.tf_instances["verify:run1"]
        assert tf.asceID == "verify"
        assert tf.asceRunningId == "verify:run1"
        assert tf.validityTimeFrame == 2000
        assert tf.props.get("threshold") == "42"
        assert tf.props.get("name") == "mytf"

        shutdown_resp = stub.Shutdown(ShutdownRequest(asceRunningId="verify:run1"))
        assert shutdown_resp.success is True

    def test_initialize_with_template_instance(self):
        """Verify templateInstance is passed correctly to the TF."""
        stub = PyTfServiceStub(self.channel)

        init_req = InitializeRequest(
            asceRunningId="template:run1",
            asceId="template",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            templateInstance="3",
            inputsInfo=[],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        )
        resp = stub.Initialize(init_req)
        assert resp.success is True

        tf = self.servicer.tf_manager.tf_instances["template:run1"]
        assert tf.instance == 3

        shutdown_resp = stub.Shutdown(ShutdownRequest(asceRunningId="template:run1"))
        assert shutdown_resp.success is True

    def test_double_initialize_shuts_down_orphan(self):
        """Verify a second Initialize for the same asceRunningId cleans up the orphan and succeeds."""
        stub = PyTfServiceStub(self.channel)

        init_req = InitializeRequest(
            asceRunningId="dup:run1",
            asceId="dup",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            inputsInfo=[],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        )
        resp = stub.Initialize(init_req)
        assert resp.success is True

        resp2 = stub.Initialize(init_req)
        assert resp2.success is True

        stub.Shutdown(ShutdownRequest(asceRunningId="dup:run1"))

    def test_shutdown_nonexistent_fails(self):
        """Verify Shutdown for a non-existent asceRunningId fails."""
        stub = PyTfServiceStub(self.channel)

        resp = stub.Shutdown(ShutdownRequest(asceRunningId="nonexistent:run"))
        assert resp.success is False
        assert "not found" in resp.error

    def test_shutdown_removes_tf_instance(self):
        """Verify Shutdown removes the TF instance from the manager."""
        stub = PyTfServiceStub(self.channel)

        init_req = InitializeRequest(
            asceRunningId="remove:run1",
            asceId="remove",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            inputsInfo=[],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        )
        resp = stub.Initialize(init_req)
        assert resp.success is True
        assert "remove:run1" in self.servicer.tf_manager.tf_instances

        stub.Shutdown(ShutdownRequest(asceRunningId="remove:run1"))
        assert "remove:run1" not in self.servicer.tf_manager.tf_instances

    def test_eval_passthrough_returns_input_value(self):
        """Verify Eval RPC passes input value through PassThroughTF unchanged."""
        stub = PyTfServiceStub(self.channel)

        stub.Initialize(InitializeRequest(
            asceRunningId="eval:run1",
            asceId="eval",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            inputsInfo=[IasioInfoProto(id="in1", iasType=ProtoIASType.LONG)],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        ))

        input_io = _make_input_io("in1", "eval:run1:in1", 42)
        actual_out = _make_actual_out("out", "eval:run1:out")

        eval_req = EvalRequest(
            asceRunningId="eval:run1",
            compInputs={"in1": input_io},
            actualOutput=actual_out,
        )
        resp = stub.Eval(eval_req)
        assert resp.success is True
        assert resp.result.value.int64Value == 42

        stub.Shutdown(ShutdownRequest(asceRunningId="eval:run1"))

    def test_eval_doubling_doubles_input_value(self):
        """Verify Eval RPC doubles the input value via DoublingTF."""
        stub = PyTfServiceStub(self.channel)

        stub.Initialize(InitializeRequest(
            asceRunningId="eval:run2",
            asceId="eval",
            pythonClassName="test_tf_module.DoublingTF",
            validityTimeFrame=1000,
            props={},
            inputsInfo=[IasioInfoProto(id="in1", iasType=ProtoIASType.LONG)],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        ))

        input_io = _make_input_io("in1", "eval:run2:in1", 21)
        actual_out = _make_actual_out("out", "eval:run2:out")

        eval_req = EvalRequest(
            asceRunningId="eval:run2",
            compInputs={"in1": input_io},
            actualOutput=actual_out,
        )
        resp = stub.Eval(eval_req)
        assert resp.success is True
        assert resp.result.value.int64Value == 42

        stub.Shutdown(ShutdownRequest(asceRunningId="eval:run2"))

    def test_eval_uninitialized_fails(self):
        """Verify Eval for a non-existent asceRunningId returns an error."""
        stub = PyTfServiceStub(self.channel)

        actual_out = _make_actual_out("out", "noasce:run:out")

        eval_req = EvalRequest(
            asceRunningId="noasce:run",
            compInputs={},
            actualOutput=actual_out,
        )
        resp = stub.Eval(eval_req)
        assert resp.success is False
        assert "not initialized" in resp.error

    def test_eval_double_roundtrip(self):
        """Verify multiple consecutive Eval calls work correctly on the same TF."""
        stub = PyTfServiceStub(self.channel)

        stub.Initialize(InitializeRequest(
            asceRunningId="eval:run3",
            asceId="eval",
            pythonClassName="test_tf_module.DoublingTF",
            validityTimeFrame=1000,
            props={},
            inputsInfo=[IasioInfoProto(id="in1", iasType=ProtoIASType.LONG)],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        ))

        actual_out = _make_actual_out("out", "eval:run3:out")

        for input_val in [5, 10, 15]:
            input_io = _make_input_io("in1", "eval:run3:in1", input_val)
            actual_out.value.int64Value = 0

            eval_req = EvalRequest(
                asceRunningId="eval:run3",
                compInputs={"in1": input_io},
                actualOutput=actual_out,
            )
            resp = stub.Eval(eval_req)
            assert resp.success is True
            assert resp.result.value.int64Value == input_val * 2

        stub.Shutdown(ShutdownRequest(asceRunningId="eval:run3"))

    def test_multi_tf_correct_routing(self):
        """Verify Eval routes to the correct TF when two different TFs are active.

        Initializes DoublingTF (output = 2 * input) and TriplingTF
        (output = 3 * input) on the same server with different
        asceRunningIds.  Sends intermixed Eval requests and verifies
        each returns the expected transformed value, proving the server
        dispatches to the right TF instance.
        """
        stub = PyTfServiceStub(self.channel)

        # Initialize DoublingTF as asce1
        stub.Initialize(InitializeRequest(
            asceRunningId="multi:double",
            asceId="multi",
            pythonClassName="test_tf_module.DoublingTF",
            validityTimeFrame=1000,
            props={},
            inputsInfo=[IasioInfoProto(id="in1", iasType=ProtoIASType.LONG)],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        ))

        # Initialize TriplingTF as asce2
        stub.Initialize(InitializeRequest(
            asceRunningId="multi:triple",
            asceId="multi",
            pythonClassName="test_tf_module.TriplingTF",
            validityTimeFrame=1000,
            props={},
            inputsInfo=[IasioInfoProto(id="in1", iasType=ProtoIASType.LONG)],
            outputInfo=IasioInfoProto(id="out", iasType=ProtoIASType.LONG),
        ))

        assert len(self.servicer.tf_manager.tf_instances) == 2

        # Intermix evals — double first, then triple, then double again
        test_values = [7, 11, 13]
        for val in test_values:
            # DoublingTF
            input_io = _make_input_io("in1", "multi:double:in1", val)
            actual_out = _make_actual_out("out", "multi:double:out")
            resp = stub.Eval(EvalRequest(
                asceRunningId="multi:double",
                compInputs={"in1": input_io},
                actualOutput=actual_out,
            ))
            assert resp.success is True
            assert resp.result.value.int64Value == val * 2, (
                f"DoublingTF expected {val * 2}, got {resp.result.value.int64Value}"
            )

            # TriplingTF
            input_io = _make_input_io("in1", "multi:triple:in1", val)
            actual_out = _make_actual_out("out", "multi:triple:out")
            resp = stub.Eval(EvalRequest(
                asceRunningId="multi:triple",
                compInputs={"in1": input_io},
                actualOutput=actual_out,
            ))
            assert resp.success is True
            assert resp.result.value.int64Value == val * 3, (
                f"TriplingTF expected {val * 3}, got {resp.result.value.int64Value}"
            )

        stub.Shutdown(ShutdownRequest(asceRunningId="multi:double"))
        stub.Shutdown(ShutdownRequest(asceRunningId="multi:triple"))
