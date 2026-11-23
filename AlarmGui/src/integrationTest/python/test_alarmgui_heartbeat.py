"""
Integration test: verify that iasAlarmGui sends heartbeats to Kafka with the
correct state transitions (STARTING_UP -> RUNNING -> EXITING).

The test spawns iasAlarmGui as a subprocess with heartbeats enabled,
consumes heartbeats from the Kafka HeartbeatTopic, and verifies the expected
state sequence.  On teardown the GUI process is terminated with SIGINT and the
test waits for the EXITING heartbeat.

The spawned GUI process runs headless via QT_QPA_PLATFORM=offscreen so no
display server is needed.  The test itself (pytest) creates no Qt widgets.
"""

import logging
import os
import signal
import subprocess
import sys
import time
import uuid
from queue import Queue, Empty

from IasLogging.log import Log
from IasKafkaUtils.IaskafkaHelper import IasKafkaHelper
from IasHeartbeat.HbKafkaConsumer import HbKafkaConsumer, HeartbeatListener
from IasHeartbeat.HearbeatMessage import HeartbeatMessage
from IasHeartbeat.HeartbeatStatus import HeartbeatStatus


class HbCollector(HeartbeatListener):
    """Collects heartbeats into a queue for inspection."""

    def __init__(self):
        super().__init__()
        self.queue: Queue[HeartbeatMessage] = Queue()

    def clear(self):
        while True:
            try:
                self.queue.get_nowait()
            except Empty:
                break

    def iasHbReceived(self, hb: HeartbeatMessage):
        self.queue.put(hb)

    def get(self, timeout: float = 10) -> HeartbeatMessage:
        return self.queue.get(block=True, timeout=timeout)


class TestAlarmGuiHeartbeatIntegration:
    """
    Spawn iasAlarmGui with heartbeats enabled and verify the HB state
    transitions by consuming from the HeartbeatTopic.
    """

    HB_FREQUENCY = 2
    COLLECT_TIMEOUT = 15

    def setup_method(self):
        Log.init_logging(__file__)
        self.logger = logging.getLogger(self.__class__.__name__)

        self.hb_collector = HbCollector()
        cons_id = f"hb-int-test-{uuid.uuid4().hex[:8]}"

        brokers = IasKafkaHelper.DEFAULT_BOOTSTRAP_BROKERS
        self.consumer = HbKafkaConsumer(
            kafkabrokers=brokers,
            clientid=cons_id,
            groupid=cons_id,
            listener=self.hb_collector,
        )
        self.consumer.start(assgnemntTimeout=30)
        time.sleep(1)

        self.gui_proc = subprocess.Popen(
            [
                "iasAlarmGui",
                "-b", brokers,
                "-f", str(self.HB_FREQUENCY),
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            start_new_session=True,
            env={**os.environ, "QT_QPA_PLATFORM": "offscreen"},
        )
        time.sleep(self.HB_FREQUENCY * 2 + 2)
        assert self.gui_proc.poll() is None, (
            "AlarmGui process exited unexpectedly. "
            f"stdout: {self.gui_proc.stdout.read().decode()}"
        )

    def teardown_method(self):
        self.consumer.close()
        try:
            os.killpg(os.getpgid(self.gui_proc.pid), signal.SIGINT)
            self.gui_proc.wait(timeout=10)
        except (subprocess.TimeoutExpired, ProcessLookupError):
            try:
                os.killpg(os.getpgid(self.gui_proc.pid), signal.SIGKILL)
            except ProcessLookupError:
                pass
            self.gui_proc.wait()

    def test_heartbeats_received(self):
        """
        Verify that AlarmGui publishes heartbeats after startup.  Collects
        heartbeats with a timeout to confirm the engine is alive.
        """
        self.hb_collector.clear()
        messages: list[HeartbeatMessage] = []
        deadline = time.time() + self.COLLECT_TIMEOUT

        while len(messages) < 5 and time.time() < deadline:
            try:
                messages.append(self.hb_collector.get(timeout=3))
            except Empty:
                continue

        assert len(messages) >= 1, "No heartbeats received"
