#! /usr/bin/env python3
"""Test module for the PyTfServer gRPC server.

This module contains three categories of tests:

========================= =================== ========
Category                  Requires server?   Network?
========================= =================== ========
TestPyTfServer (Ping)     In-process gRPC    No
TestPyTfServerLifecycle   In-process gRPC    No
TestPyTfServerNetwork     Separate subprocess  Yes (TCP)
========================= =================== ========

**TestPyTfServer** — Starts the ``PyTfService`` servicer in-process on a
random localhost port using ``grpc.server(ThreadPoolExecutor)``.  Verifies
only the ``Ping`` health-check RPC.  No ASCE lifecycle is involved.
Termination: ``server.stop(0)`` in ``teardown_method``.

**TestPyTfServerLifecycle** — Also starts the servicer in-process (same
pattern as TestPyTfServer) but exercises the full
Initialize/Eval/Shutdown RPC lifecycle.  Verifies parameter passing,
templateInstance handling, double-initialize rejection, and TF instance
cleanup.  **No network stack is involved** — the gRPC channel is
localhost-to-localhost within the same process.
Termination: ``server.stop(0)`` in ``teardown_method``.

**TestPyTfServerNetwork** — Spawns ``iasPyTfServer.py`` as an independent
child process on a random TCP port.  The test connects via a Python gRPC
client over the real network stack.  Termination: ``SIGINT`` is sent to
the child's process group, with a ``SIGKILL`` fallback on timeout.

The PYTHONPATH in all tests includes the build trees of CompElement,
BasicTypes, Tools, and the PyTfServer source and generated proto paths.

Run with::

    pytest PyTfServer/src/test/python/test_server.py
"""

import os
import signal
import socket
import subprocess
import sys
import time

python_version = f"python{sys.version_info.major}.{sys.version_info.minor}"
ias_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))))

comp_element_build = os.path.join(ias_root, 'CompElement', 'build', 'lib', python_version, 'site-packages')
basictypes_build = os.path.join(ias_root, 'BasicTypes', 'build', 'lib', python_version, 'site-packages')
tools_build = os.path.join(ias_root, 'Tools', 'build', 'lib', python_version, 'site-packages')
pytfserver_build = os.path.join(ias_root, 'PyTfServer', 'build', 'lib', python_version, 'site-packages')
pytfserver_src = os.path.join(ias_root, 'PyTfServer', 'src', 'main', 'python')
basictypes_gen_proto = os.path.join(ias_root, 'BasicTypes', 'build', 'generated', 'python', 'IasProtobufTypes')
compelement_gen_proto = os.path.join(ias_root, 'CompElement', 'build', 'generated', 'python', 'IasProtobufTfserver')

for path in [comp_element_build, basictypes_build, tools_build, pytfserver_build, pytfserver_src,
             basictypes_gen_proto, compelement_gen_proto]:
    if path not in sys.path:
        sys.path.insert(0, path)

import grpc
from concurrent import futures

from IasProtobufTfserver.pytf_service_pb2 import (
    PingRequest,
    InitializeRequest,
    ShutdownRequest,
    IasioInfoProto,
)
from IasProtobufTfserver.pytf_service_pb2_grpc import add_PyTfServiceServicer_to_server


class TestPyTfServer:
    """Ping health-check tests — in-process gRPC server, no separate process, no network.

    The ``PyTfService`` servicer is registered on an in-process ``grpc.server``
    bound to a random localhost port.  Each test uses a Python gRPC blocking
    stub to send ``Ping`` RPCs.

    **Start**: ``grpc.server(ThreadPoolExecutor)`` +
    ``add_PyTfServiceServicer_to_server(PyTfService(), server)`` in
    ``setup_method``.

    **Terminate**: ``server.stop(0)`` + ``channel.close()`` in
    ``teardown_method``.
    """

    def setup_method(self):
        from IasPyTfServer.tf_service import PyTfService
        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        add_PyTfServiceServicer_to_server(PyTfService(), self.server)
        self.port = self.server.add_insecure_port("localhost:0")
        self.server.start()
        self.channel = grpc.insecure_channel(f"localhost:{self.port}")

    def teardown_method(self):
        self.channel.close()
        self.server.stop(0)

    def test_ping_returns_success(self):
        """Verify a single Ping RPC returns success = True."""
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        blocking_stub = PyTfServiceStub(self.channel)
        response = blocking_stub.Ping(PingRequest(message="hello"))
        assert response.success is True

    def test_ping_multiple_times(self):
        """Verify multiple consecutive Ping RPCs all succeed."""
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        stub = PyTfServiceStub(self.channel)
        for i in range(5):
            response = stub.Ping(PingRequest(message=f"ping-{i}"))
            assert response.success is True

class TestPyTfServerLifecycle:
    """Initialize/Eval/Shutdown lifecycle tests — in-process gRPC, no separate process, no network.

    Same start/terminate pattern as ``TestPyTfServer``: the servicer is
    registered on an in-process ``grpc.server`` on a random localhost port.
    Each test creates its own fresh ``PyTfService`` instance (via the
    servicer reference) so that ``tf_manager.tf_instances`` is clean.

    What is tested in isolation (no network):
    - Full Initialize → Shutdown roundtrip via gRPC stubs
    - Parameter passing (asceId, asceRunningId, validityTimeFrame, props)
    - Optional templateInstance (string→int conversion via proto HasField)
    - Double-initialize rejection (same asceRunningId)
    - Shutdown of non-existent ASCE (error response)
    - Shutdown removes TF instance from the TfManager dict

    **Start**: ``grpc.server(ThreadPoolExecutor)`` + servicer in
    ``setup_method``.

    **Terminate**: ``server.stop(0)`` + ``channel.close()`` in
    ``teardown_method``.
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
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
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
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
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
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
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

    def test_double_initialize_fails(self):
        """Verify a second Initialize for the same asceRunningId fails."""
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
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
        assert resp2.success is False
        assert "already initialized" in resp2.error

        stub.Shutdown(ShutdownRequest(asceRunningId="dup:run1"))

    def test_shutdown_nonexistent_fails(self):
        """Verify Shutdown for a non-existent asceRunningId fails."""
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        stub = PyTfServiceStub(self.channel)

        resp = stub.Shutdown(ShutdownRequest(asceRunningId="nonexistent:run"))
        assert resp.success is False
        assert "not found" in resp.error

    def test_shutdown_removes_tf_instance(self):
        """Verify Shutdown removes the TF instance from the manager."""
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        from IasProtobufTypes.types_pb2 import IASType as ProtoIASType
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


class TestPyTfServerNetwork:
    """Network-level tests — real subprocess, TCP gRPC communication.

    Unlike ``TestPyTfServer`` and ``TestPyTfServerLifecycle``, this test
    class starts ``iasPyTfServer.py`` as an **independent child process**.
    The server binds to a real TCP port on localhost, and the test connects
    via a Python gRPC blocking stub over the actual network stack.

    **PYTHONPATH**: the full build and source path is propagated to the
    child process via ``os.environ`` so the generated proto stubs and IAS
    packages are importable.

    **Start**: ``subprocess.Popen`` with ``start_new_session=True`` on a
    free TCP port found via ``socket.bind(0)``.  The test waits 1 s for
    the server to be ready and checks that the process has not exited.

    **Terminate**: ``SIGINT`` is sent to the child's entire process group
    via ``os.killpg``.  A 5-second ``wait(timeout=5)`` follows.  If the
    process does not exit, ``SIGKILL`` is sent to the group as a fallback.
    ``channel.close()`` always runs first.
    """

    @staticmethod
    def _find_free_port():
        """Return a free TCP port on localhost."""
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind(("localhost", 0))
            return s.getsockname()[1]

    def setup_method(self):
        """Start the server as a child process on a random port and open a gRPC channel."""
        self.port = self._find_free_port()
        python_exe = sys.executable
        server_script = os.path.join(
            os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
            "main", "python", "iasPyTfServer.py",
        )
        env = os.environ.copy()
        env["PYTHONPATH"] = os.pathsep.join(sys.path)
        self.server_proc = subprocess.Popen(
            [python_exe, server_script, "-p", str(self.port)],
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            start_new_session=True,
        )
        time.sleep(1)
        assert self.server_proc.poll() is None, "Server process exited unexpectedly"
        self.channel = grpc.insecure_channel(f"localhost:{self.port}")

    def teardown_method(self):
        """Send SIGINT to the server process group, wait for clean exit, falling back to SIGKILL."""
        self.channel.close()
        try:
            os.killpg(os.getpgid(self.server_proc.pid), signal.SIGINT)
            self.server_proc.wait(timeout=5)
        except (subprocess.TimeoutExpired, ProcessLookupError):
            try:
                os.killpg(os.getpgid(self.server_proc.pid), signal.SIGKILL)
            except ProcessLookupError:
                pass
            self.server_proc.wait()

    def test_ping_returns_success(self):
        """Verify a single Ping RPC over the network returns success = True."""
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        stub = PyTfServiceStub(self.channel)
        response = stub.Ping(PingRequest(message="network-hello"))
        assert response.success is True

    def test_ping_multiple_times(self):
        """Verify multiple consecutive Ping RPCs over the network all succeed."""
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub
        stub = PyTfServiceStub(self.channel)
        for i in range(5):
            response = stub.Ping(PingRequest(message=f"net-ping-{i}"))
            assert response.success is True
