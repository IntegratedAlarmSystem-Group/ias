#! /usr/bin/env python3

import sys
import os

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

from IasProtobufTfserver.pytf_service_pb2 import PingRequest
from IasProtobufTfserver.pytf_service_pb2_grpc import add_PyTfServiceServicer_to_server
from IasPyTfServer.tf_service import PyTfService


class TestPyTfServer:
    """Unit tests for the PyTfServer gRPC server in isolation.

    Starts the server in-process with a random port, and uses a Python gRPC
    client to verify the Ping health check RPC.  No ASCE lifecycle
    (Initialize/Eval/Shutdown) is involved here.
    """

    def setup_method(self):
        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        add_PyTfServiceServicer_to_server(PyTfService(), self.server)
        self.port = self.server.add_insecure_port("localhost:0")
        self.server.start()
        self.channel = grpc.insecure_channel(f"localhost:{self.port}")

    def teardown_method(self):
        self.channel.close()
        self.server.stop(0)

    def test_ping_returns_success(self):
        stub = self.channel
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub as PyTfServiceBlockingStub
        blocking_stub = PyTfServiceBlockingStub(self.channel)
        response = blocking_stub.Ping(PingRequest(message="hello"))
        assert response.success is True

    def test_ping_multiple_times(self):
        from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceStub as PyTfServiceBlockingStub
        stub = PyTfServiceBlockingStub(self.channel)
        for i in range(5):
            response = stub.Ping(PingRequest(message=f"ping-{i}"))
            assert response.success is True
