#! /usr/bin/env python3
"""Test module for the PyTfServer gRPC server (Ping + Network tests).

This module contains two categories of tests:

========================= =================== ========
Category                  Requires server?   Network?
========================= =================== ========
TestPyTfServer (Ping)     In-process gRPC    No
TestPyTfServerNetwork     Separate subprocess  Yes (TCP)
========================= =================== ========

**TestPyTfServer** — Starts the ``PyTfService`` servicer in-process on a
random localhost port using ``grpc.server(ThreadPoolExecutor)``.  Verifies
only the ``Ping`` health-check RPC.  No ASCE lifecycle is involved.
Termination: ``server.stop(0)`` in ``teardown_method``.

**TestPyTfServerNetwork** — Spawns ``iasPyTfServer.py`` as an independent
child process on a random TCP port.  The test connects via a Python gRPC
client over the real network stack.  Termination: ``SIGINT`` is sent to
the child's process group, with a ``SIGKILL`` fallback on timeout.

The Initialize/Eval/Shutdown lifecycle tests moved to
``test_server_lifecycle.py``.

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

import _test_paths  # noqa: F401 adds generated proto dirs to sys.path

import grpc
from concurrent import futures

from IasProtobufTfserver.pytf_service_pb2 import PingRequest
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
