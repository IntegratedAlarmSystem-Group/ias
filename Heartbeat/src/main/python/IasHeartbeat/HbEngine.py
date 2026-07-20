import logging
import time
from threading import Event, Thread, Lock
from IasHeartbeat.HeartbeatStatus import HeartbeatStatus
from IasHeartbeat.Heartbeat import Heartbeat
from IasHeartbeat.HbKafkaProducer import HbKafkaProducer

class HbEngine:
    """
    Python equivalent of HbEngine.scala.

    HbEngine immediately start sending HBs with the STARTING_UP state
    
    HBs are sent periodically and on change i.e. when the state changes.
    """

    INITIAL_STATE = HeartbeatStatus.STARTING_UP

    def __init__(self,
                 hb: Heartbeat,
                 frequency: int,
                 producer: HbKafkaProducer):
        """
        Constructor

        Params:
            hb The heartbeat to publish
            frequency The frequency to send HBs (seconds)
        """
        if frequency<=0:
            raise ValueError(f"Invalid frequency {frequency}: must be >0")
        if not hb:
            raise ValueError("Invanid None HB")
        if not producer:
            raise ValueError("Invalid None kafka producer")
        
        self._frequency = frequency
        self._hb = hb
        self._producer = producer

        self._logger = logging.getLogger(self.__class__.__name__)

        # The state to publish
        self._hb_state = HbEngine.INITIAL_STATE

        # Additional properties
        self._props: dict[str, str]|None = None

        # The flag to signal the thread to terminate
        self._interruped: Event = Event()

        # Mutual exclusion
        self._mutex = Lock()

        # The thread that periodicaly publish HBs
        self._thread: Thread = Thread(target=self.sender_thread, name="HbEngine-Thread", daemon=True)

    def start(self, hb_status: HeartbeatStatus = HeartbeatStatus.STARTING_UP) -> None:
        """
        Start the threads that periodically publish the HBs in the BSDB
        
        Parmas:
            hb_status The intial HB state to send
        """
        self._logger.debug("Starting to send HBs with initiial state %s", hb_status._name_)
        self.update_hb_state(hb_status=hb_status)
        self._thread.start()
        self._logger.info("HbEngine sender thread started")

    def close(self)->None:
        """
        Stop the thread to send HBs
        """
        self._logger.debug("Signaling the HB engine thread to terminate")
        self._interruped.set()
        self._producer.close()
        self._logger.debug("Wait for the termination of HB engine thread")
        if self._thread is not None:
            self._logger.debug("Waiting for thread termination")
            if self._thread.is_alive():
                self._thread.join(timeout=10 if self._frequency<10 else self._frequency+5)
                if self._thread.is_alive():
                    self._logger.warning("HB engine thread did not terminate in time")
        self._logger.info("HB engine closed")

    def sender_thread(self)->None:
        """
        The thread that periodically publishes HBs
        """
        self._logger.debug("HB engine thread running")
        while not self._interruped.is_set():
            with self._mutex:
                self._producer.send(hb=self._hb, hb_status=self._hb_state, props=self._props)
                self._logger.debug(f"HB sent to BSDB: {self._hb.stringRepr} {self._hb_state._name_}")
            time.sleep(self._frequency)
        self._logger.info("HB engine thread terminated")

    def update_hb_state(self, hb_status: HeartbeatStatus)->None:
        """
        Updates the state of the HB and immediately publish the HB
        """
        if not hb_status:
            self._logger.error("Cannot set a None HB state")
            raise ValueError("Cannot set a None HB state")
        with self._mutex:
            self._logger.debug(f"HB state transition: {self._hb_state._name_}->{hb_status._name_}")
            self._hb_state = hb_status
            if not self._interruped.is_set():
                self._producer.send(hb=self._hb, hb_status=self._hb_state)

    def update_props(self, props:dict[str,str]|None)->None:
        with self._mutex:
            self._props = props
            if not self._interruped.is_set():
                self._producer.send(hb=self._hb, hb_status=self._hb_state,props=self._props)
