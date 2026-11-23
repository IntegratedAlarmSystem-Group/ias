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

    iasPyTfServer.py [-p PORT] [-i ID] [-c JCDB] [-b BSDBURL] [-f HBFREQ] [--log-level LEVEL] [--log-config FILE]

    The ``-p`` / ``--port`` option defaults to 50051.  The ``-i`` / ``--id``
    option defaults to ``PyTfServer-<hostname>-<port>``.  Logging flags are
    handled by ``IasLogging.Log``.  ``-c`` / ``--jcdb`` and ``-b`` / ``--bsdburl``
    are used to resolve the Kafka BSDB URL (same pattern as AlarmGui).  ``-f`` /
    ``--hbfrequency`` sets the heartbeat interval in seconds (default 5, 0 disables).

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
import socket
import typing
from concurrent import futures
import grpc

from IasProtobufTfserver.pytf_service_pb2_grpc import add_PyTfServiceServicer_to_server
from IasPyTfServer.tf_service import PyTfService

from IasBasicTypes.Identifier import Identifier
from IasBasicTypes.IdentifierType import IdentifierType
from IasLogging.log import Log
from IasHeartbeat.HbEngine import HbEngine
from IasHeartbeat.HbKafkaProducer import HbKafkaProducer
from IasHeartbeat.Heartbeat import Heartbeat
from IasHeartbeat.HeartbeatProducerType import HeartbeatProducerType
from IasHeartbeat.HeartbeatStatus import HeartbeatStatus
from IasHeartbeat.HeartbeatHelper import get_hb_frequency, HB_FREQUENCY_DEFAULT
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper

logger = logging.getLogger(__name__)

hb_engine: list[HbEngine | None] = [None]


def parse() -> dict[str, typing.Any]:
    """
    Parse the command line arguments.

    Returns:
        A dictionary with the params set in the command line:
            - 'port': int  port to listen on
            - 'id': str    server instance identifier
            - 'jcdb': str  parent folder of the IAS CDB (or None)
            - 'bsdburl': str  BSDB (Kafka) URL (or None)
            - 'hbfrequency': int  heartbeat frequency in seconds (or None)
    """
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
    parser.add_argument(
        '-c',
        '--jcdb',
        help='The parent folder of the IAS CDB (used to resolve Kafka brokers and heartbeat frequency)',
        action='store',
        type=str,
        default=None,
        required=False)
    parser.add_argument(
        '-b',
        '--bsdburl',
        help='Kafka BSDB URL (format server:port,server:port...). Overrides --jcdb',
        action='store',
        type=str,
        default=None,
        required=False)
    parser.add_argument(
        '-f',
        '--hbfrequency',
        help=f'Heartbeat frequency in seconds (0 to disable, default from CDB or {HB_FREQUENCY_DEFAULT})',
        action='store',
        type=int,
        default=None,
        required=False)
    Log.add_log_arguments_to_parser(parser)

    args = parser.parse_args()
    Log.init_log_from_cmdline_args(args, "PyTfServer")

    ret: dict[str, typing.Any] = {}
    ret["port"] = args.port
    ret["id"] = args.id
    ret["jcdb"] = args.jcdb
    ret["bsdburl"] = args.bsdburl
    ret["hbfrequency"] = args.hbfrequency

    return ret


def serve(server_id: str, port: int = 50051):
    """Start the gRPC server on ``localhost:<port>``.

    Registers the ``PyTfService`` servicer with a thread-pool executor
    (10 workers) and blocks until the process is interrupted.
    On shutdown, the global HB engine is closed.

    Args:
        port: TCP port to bind to. Defaults to 50051.
        server_id: Identifier for this server instance.
    """
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    add_PyTfServiceServicer_to_server(PyTfService(), server)
    server.add_insecure_port(f"localhost:{port}")
    if hb_engine[0] is not None:
        hb_engine[0].update_hb_state(HeartbeatStatus.RUNNING)
    server.start()
    logger.info(f"[PyTfServer] started on localhost:{port} (id={server_id})")
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        if hb_engine[0] is not None:
            hb_engine[0].update_hb_state(HeartbeatStatus.EXITING)
        logger.info("[PyTfServer] shutting down")
    finally:
        if hb_engine[0] is not None:
            hb_engine[0].close()
        server.stop(0)


if __name__ == "__main__":
    cmd_line_args = parse()

    server_id = cmd_line_args["id"]
    port = cmd_line_args["port"]
    jcdb = cmd_line_args["jcdb"]
    bsdburl = cmd_line_args["bsdburl"]
    hbfrequency = cmd_line_args["hbfrequency"]

    if not server_id:
        server_id = f"PyTfServer-{socket.gethostname()}-{port}"

    bsdb = IasKafkaHelper.get_bsdb_url(
        kafka_brokers=bsdburl,
        jCdb=jcdb)

    identifier = Identifier(server_id, IdentifierType.CLIENT)
    full_running_id = identifier.build_full_running_id()
    logger.info(f"[PyTfServer] fullRunningId: {full_running_id}")

    hb_frequency = get_hb_frequency(hbfrequency, jcdb)

    if bsdb and hb_frequency > 0:
        hb_producer = HbKafkaProducer(bsdb, f"PyTfServer-{server_id}")
        hb = Heartbeat(HeartbeatProducerType.CLIENT, server_id, socket.gethostname())
        hb_engine[0] = HbEngine(hb, hb_frequency, hb_producer)
        hb_engine[0].start()
        logger.info(f"[PyTfServer] Heartbeats enabled, frequency={hb_frequency}s, brokers={bsdb}")

    serve(server_id, port)
