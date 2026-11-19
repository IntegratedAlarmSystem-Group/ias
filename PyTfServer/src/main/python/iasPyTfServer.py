#! /usr/bin/env -S python3 -u
"""CLI entry point for the PyTfServer gRPC server.

Binds an insecure gRPC server to ``localhost:<port>`` with a thread pool
of 10 workers and registers the ``PyTfService`` servicer.  Handles SIGINT
for graceful shutdown.

This process is intended to be managed externally (systemd, supervisor, or
a process manager).  The IAS Supervisor connects to it via the
``GrpcPythonExecutor`` gRPC client.  Only one instance is needed per
Supervisor.

**CLI usage**

    iasPyTfServer.py [-p PORT] [-i ID] [--log-level LEVEL] [--log-config FILE]

    The ``-p`` / ``--port`` option defaults to 50051.  The ``-i`` / ``--id``
    option defaults to ``PyTfServer-<hostname>-<port>``.  Logging flags are
    handled by ``IasLogging.Log``.

**Starting**

- On the command line::

    python3 -u iasPyTfServer.py -p 50051

- From tests: the ``iasPyTfServer.py`` script is spawned as a subprocess
  with a ``PYTHONPATH`` that includes the build tree of BasicTypes,
  CompElement, Tools, and the PyTfServer source and generated proto
  directories.  See ``test_server.py`` for the exact layout.

**Termination**

- SIGINT (Ctrl-C) triggers ``interrupt_handler`` which exits cleanly.
- The gRPC server calls ``server.stop(0)`` for graceful drain.
"""

import argparse
import logging
import signal
import socket
import sys
from concurrent import futures
import grpc

from IasProtobufTfserver.pytf_service_pb2_grpc import add_PyTfServiceServicer_to_server
from IasPyTfServer.tf_service import PyTfService

from IasLogging.log import Log

logger = logging.getLogger(__name__)


def serve(port: int = 50051, server_id: str):
    """Start the gRPC server on ``localhost:<port>``.

    Registers the ``PyTfService`` servicer with a thread-pool executor
    (10 workers) and blocks until the process is interrupted.

    Args:
        port: TCP port to bind to. Defaults to 50051.
        server_id: Identifier for this server instance.
    """
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    add_PyTfServiceServicer_to_server(PyTfService(), server)
    server.add_insecure_port(f"localhost:{port}")
    server.start()
    logger.info(f"[PyTfServer] started on localhost:{port} (id={server_id})")
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
    parser.add_argument(
        '-i',
        '--id',
        help='Identifier of this server instance. Defaults to PyTfServer-<hostname>-<port>',
        action='store',
        type=str,
        default='',
        required=False)
    Log.add_log_arguments_to_parser(parser)

    args = parser.parse_args()
    Log.init_log_from_cmdline_args(args, "PyTfServer")

    if not args.id:
        args.id = f"PyTfServer-{socket.gethostname()}-{args.port}"

    def interrupt_handler(signum, frame):
        logger.info("[PyTfServer] received interrupt signal")
        sys.exit(0)

    signal.signal(signal.SIGINT, interrupt_handler)
    serve(args.port, args.id)
