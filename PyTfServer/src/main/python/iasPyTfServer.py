#! /usr/bin/env -S python3 -u
import argparse
import logging
import signal
import sys
from concurrent import futures
import grpc

from IasProtobufTfserver.pytf_service_pb2 import PingResponse
from IasProtobufTfserver.pytf_service_pb2_grpc import PyTfServiceServicer, add_PyTfServiceServicer_to_server

from IasLogging.log import Log

logger = logging.getLogger(__name__)

class PyTfService(PyTfServiceServicer):

    def Ping(self, request, context):
        logger.info(f"[PyTfServer] received Ping: message='{request.message}'")
        return PingResponse(success=True)


def serve(port: int = 50051):
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    add_PyTfServiceServicer_to_server(PyTfService(), server)
    server.add_insecure_port(f"localhost:{port}")
    server.start()
    logger.info(f"[PyTfServer] started on localhost:{port}")
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("[PyTfServer] shutting down")
        server.stop(0)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='PyTf gRPC server')
    parser.add_argument(
        '-p',
        '--port',
        help='The port to listen on (default: 50051)',
        action='store',
        type=int,
        default=50051,
        required=False)
    Log.add_log_arguments_to_parser(parser)

    args = parser.parse_args()
    Log.init_log_from_cmdline_args(args, "PyTfServer")

    def interrupt_handler(signum, frame):
        logger.info("[PyTfServer] received interrupt signal")
        sys.exit(0)

    signal.signal(signal.SIGINT, interrupt_handler)
    serve(args.port)
