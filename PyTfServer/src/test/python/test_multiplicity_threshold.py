"""Unit tests for MultiplicityThreshold Python TF in isolation.

Tests the MultiplicityThreshold transfer function directly via TfManager,
mirroring the scenarios from TestMultiplicityTF.scala — threshold activation,
property propagation with merge, mode propagation, validity constraints,
and constructor validation.

Run with::

    pytest PyTfServer/src/test/python/test_multiplicity_threshold.py
"""

import _test_paths  # noqa: F401 adds generated proto dirs to sys.path

from IasPyTfServer.tf_manager import TfManager
from IasTransferFunction.IASIO import IASIO
from IasTransferFunction.IasioInfo import IasioInfo
from IasBasicTypes.Alarm import Alarm
from IasBasicTypes.Priority import Priority
from IasBasicTypes.IasType import IASType
from IasBasicTypes.OperationalMode import OperationalMode
from IasBasicTypes.Validity import Validity


THRESHOLD_PROP = "org.eso.ias.tf.mutliplicity.threshold"
PRIORITY_PROP = "org.eso.ias.tf.alarm.priority"
INPUT_ALARMS_SET_PROP = "IdsOfAlarmsSet"


def make_alarm_iasio(
    input_id,
    alarm=None,
    mode=OperationalMode.OPERATIONAL,
    validity=Validity.RELIABLE,
    props=None,
):
    """Build an alarm input IASIO."""
    if alarm is None:
        alarm = Alarm.get_initial_alarmstate()
    if props is None:
        props = {}
    return IASIO(
        id=input_id,
        runningId="test:run:" + input_id,
        mode=mode,
        iasType=IASType.ALARM,
        validity=validity,
        value=alarm,
        productionTStamp=1,
        props=props,
    )


def make_output_iasio():
    """Build the initial ALARM output IASIO (no alarm value set)."""
    return IASIO(
        id="OutputAlarm",
        runningId="test:run:OutputAlarm",
        mode=OperationalMode.UNKNOWN,
        iasType=IASType.ALARM,
        validity=Validity.RELIABLE,
        value=None,
        productionTStamp=1,
        props={},
    )


PYTHON_CLASS = "IasTransferFunction.MultiplicityThreshold"


class TestMultiplicityThresholdInit:
    """Constructor and initialization validation."""

    def setup_method(self):
        self.manager = TfManager()

    def _init(self, asce_running_id, props, num_inputs=5):
        inputs_info = [IasioInfo(f"InputAlarm{i}", IASType.ALARM)
                       for i in range(num_inputs)]
        output_info = IasioInfo("OutputAlarm", IASType.ALARM)
        return self.manager.initialize(
            asceRunningId=asce_running_id,
            asceId="test",
            pythonClassName=PYTHON_CLASS,
            validityTimeFrame=1000,
            props=props,
            templateInstance=None,
            inputsInfo=inputs_info,
            outputInfo=output_info,
        )

    def test_rejects_missing_threshold(self):
        success, error = self._init("test:run", {})
        assert not success
        assert "threshold" in error.lower() or "ThresholdPropName" in error or "Missing" in error

    def test_rejects_empty_threshold(self):
        success, error = self._init("test:run", {THRESHOLD_PROP: ""})
        assert not success

    def test_rejects_non_numeric_threshold(self):
        success, error = self._init("test:run", {THRESHOLD_PROP: "abc"})
        assert not success

    def test_rejects_zero_threshold(self):
        success, error = self._init("test:run", {THRESHOLD_PROP: "0"})
        assert not success

    def test_rejects_negative_threshold(self):
        success, error = self._init("test:run", {THRESHOLD_PROP: "-1"})
        assert not success

    def test_accepts_valid_threshold(self):
        success, error = self._init("test:run", {THRESHOLD_PROP: "3"})
        assert success, f"Init failed: {error}"
        self.manager.shutdown("test:run")

    def test_loads_priority_from_props(self):
        props = {THRESHOLD_PROP: "3", PRIORITY_PROP: "LOW"}
        success, error = self._init("test:run", props)
        assert success
        tf = self.manager.tf_instances["test:run"]
        assert tf.priorityFromCDB == Priority.LOW
        self.manager.shutdown("test:run")

    def test_default_priority_when_not_set(self):
        props = {THRESHOLD_PROP: "3"}
        success, error = self._init("test:run", props)
        assert success
        tf = self.manager.tf_instances["test:run"]
        assert tf.priorityFromCDB == Priority.get_default_priority()
        self.manager.shutdown("test:run")

    def test_rejects_too_few_inputs_below_threshold(self):
        props = {THRESHOLD_PROP: "3"}
        success, error = self._init("test:run", props, num_inputs=2)
        assert not success
        assert "few" in error.lower() or "threshold" in error.lower()

    def test_allows_inputs_equal_to_threshold(self):
        props = {THRESHOLD_PROP: "3"}
        success, error = self._init("test:run", props, num_inputs=3)
        assert success, f"Init failed: {error}"
        self.manager.shutdown("test:run")


class TestMultiplicityThresholdActivation:
    """Core threshold activation logic — mirror of Scala MultiplicityTF tests."""

    THRESHOLD = 3
    NUM_INPUTS = 5

    def setup_method(self):
        self.manager = TfManager()
        self.props = {THRESHOLD_PROP: str(self.THRESHOLD), PRIORITY_PROP: "LOW"}
        inputs_info = [IasioInfo(f"InputAlarm{i}", IASType.ALARM)
                       for i in range(self.NUM_INPUTS)]
        output_info = IasioInfo("OutputAlarm", IASType.ALARM)
        success, error = self.manager.initialize(
            asceRunningId="test:run",
            asceId="test",
            pythonClassName=PYTHON_CLASS,
            validityTimeFrame=1000,
            props=self.props,
            templateInstance=None,
            inputsInfo=inputs_info,
            outputInfo=output_info,
        )
        assert success, f"Init failed: {error}"

    def teardown_method(self):
        self.manager.shutdown("test:run")

    def _eval(self, comp_inputs, actual_output):
        """Run eval and return the result IASIO."""
        success, error, result = self.manager.eval("test:run", comp_inputs, actual_output)
        assert success, f"Eval failed: {error}"
        return result

    def _build_inputs(self, n_set, all_operational=True, validity=None):
        """Build comp_inputs with n_set alarms SET, rest cleared.

        :param n_set: number of alarms to set
        :param all_operational: if True all inputs use OPERATIONAL mode
        :param validity: per-input validity override, None for RELIABLE
        """
        comp_inputs = {}
        for i in range(self.NUM_INPUTS):
            alarm = Alarm.get_initial_alarmstate(Priority.HIGH).set() if i < n_set \
                else Alarm.get_initial_alarmstate()
            mode = OperationalMode.OPERATIONAL if all_operational else OperationalMode.UNKNOWN
            input_validity = validity if validity is not None else Validity.RELIABLE
            comp_inputs[f"InputAlarm{i}"] = make_alarm_iasio(
                f"InputAlarm{i}", alarm=alarm, mode=mode, validity=input_validity)
        return comp_inputs

    def test_all_cleared_does_not_activate(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(0), output)
        assert result.value is not None
        assert not result.value.is_set()

    def test_one_set_does_not_activate(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(1), output)
        assert not result.value.is_set()

    def test_two_set_does_not_activate(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(2), output)
        assert not result.value.is_set()

    def test_threshold_set_activates(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(self.THRESHOLD), output)
        assert result.value.is_set()

    def test_above_threshold_activates(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(4), output)
        assert result.value.is_set()

    def test_all_set_activates(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(self.NUM_INPUTS), output)
        assert result.value.is_set()

    def test_clearing_below_threshold_deactivates(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(self.NUM_INPUTS), output)
        assert result.value.is_set()
        result = self._eval(self._build_inputs(self.THRESHOLD - 1), output)
        assert not result.value.is_set()

    def test_full_cycle(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(0), output)
        assert not result.value.is_set()

        result = self._eval(self._build_inputs(self.NUM_INPUTS), output)
        assert result.value.is_set()

        result = self._eval(self._build_inputs(2), output)
        assert not result.value.is_set()

        result = self._eval(self._build_inputs(self.THRESHOLD), output)
        assert result.value.is_set()

        result = self._eval(self._build_inputs(self.NUM_INPUTS), output)
        assert result.value.is_set()

        result = self._eval(self._build_inputs(0), output)
        assert not result.value.is_set()

    def test_alarm_priority_from_props(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(self.THRESHOLD), output)
        assert result.value.priority == Priority.LOW

    def test_output_mode_when_set_is_mode_of_active_alarms(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(self.THRESHOLD), output)
        assert result.value.is_set()
        assert result.mode == OperationalMode.OPERATIONAL

    def test_output_mode_when_cleared_uses_all_input_modes(self):
        output = make_output_iasio()
        comp_inputs = {}
        for i in range(self.NUM_INPUTS):
            mode = OperationalMode.DEGRADED if i == 0 else OperationalMode.OPERATIONAL
            input_validity = Validity.RELIABLE
            comp_inputs[f"InputAlarm{i}"] = make_alarm_iasio(
                f"InputAlarm{i}",
                alarm=Alarm.get_initial_alarmstate(),
                mode=mode,
                validity=input_validity,
            )
        result = self._eval(comp_inputs, output)
        assert not result.value.is_set()
        assert result.mode == OperationalMode.UNKNOWN

    def test_output_mode_unknown_when_active_modes_differ(self):
        output = make_output_iasio()
        comp_inputs = {}
        for i in range(self.NUM_INPUTS):
            alarm = Alarm.get_initial_alarmstate(Priority.HIGH).set() if i < 3 \
                else Alarm.get_initial_alarmstate()
            mode = OperationalMode.DEGRADED if i == 0 else OperationalMode.OPERATIONAL
            comp_inputs[f"InputAlarm{i}"] = make_alarm_iasio(
                f"InputAlarm{i}", alarm=alarm, mode=mode)
        result = self._eval(comp_inputs, output)
        assert result.value.is_set()
        assert result.mode == OperationalMode.UNKNOWN


class TestMultiplicityThresholdProperties:
    """Property propagation and merge — mirror of Scala tests."""

    THRESHOLD = 3
    NUM_INPUTS = 5

    def setup_method(self):
        self.manager = TfManager()
        props = {THRESHOLD_PROP: str(self.THRESHOLD)}
        inputs_info = [IasioInfo(f"InputAlarm{i}", IASType.ALARM)
                       for i in range(self.NUM_INPUTS)]
        output_info = IasioInfo("OutputAlarm", IASType.ALARM)
        success, error = self.manager.initialize(
            asceRunningId="test:run",
            asceId="test",
            pythonClassName=PYTHON_CLASS,
            validityTimeFrame=1000,
            props=props,
            templateInstance=None,
            inputsInfo=inputs_info,
            outputInfo=output_info,
        )
        assert success, f"Init failed: {error}"

    def teardown_method(self):
        self.manager.shutdown("test:run")

    def _eval(self, comp_inputs, actual_output):
        success, error, result = self.manager.eval("test:run", comp_inputs, actual_output)
        assert success, f"Eval failed: {error}"
        return result

    def _build_inputs(self, n_set):
        comp_inputs = {}
        for i in range(self.NUM_INPUTS):
            alarm = Alarm.get_initial_alarmstate(Priority.HIGH).set() if i < n_set \
                else Alarm.get_initial_alarmstate()
            comp_inputs[f"InputAlarm{i}"] = make_alarm_iasio(
                f"InputAlarm{i}", alarm=alarm)
        return comp_inputs

    def test_ids_of_alarms_set_property_when_active(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(self.THRESHOLD), output)
        assert result.value.is_set()
        assert INPUT_ALARMS_SET_PROP in result.props
        active_ids = result.props[INPUT_ALARMS_SET_PROP].split(",")
        assert len(active_ids) == self.THRESHOLD

    def test_no_properties_when_cleared(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(self.THRESHOLD), output)
        assert result.value.is_set()
        assert INPUT_ALARMS_SET_PROP in result.props
        result = self._eval(self._build_inputs(0), output)
        assert not result.value.is_set()
        assert not result.props

    def test_propagate_properties_from_set_inputs_only(self):
        output = make_output_iasio()
        comp_inputs = {}
        for i in range(self.NUM_INPUTS):
            alarm = Alarm.get_initial_alarmstate(Priority.HIGH).set() if i < self.THRESHOLD \
                else Alarm.get_initial_alarmstate()
            if i < self.THRESHOLD:
                inp_props = {f"InputAlarm{i}": "SET"}
            else:
                inp_props = {f"InputAlarm{i}": "CLEARED"}
            comp_inputs[f"InputAlarm{i}"] = make_alarm_iasio(
                f"InputAlarm{i}", alarm=alarm, props=inp_props)
        result = self._eval(comp_inputs, output)
        assert result.value.is_set()
        for i in range(self.THRESHOLD):
            assert result.props[f"InputAlarm{i}"] == "SET"

    def test_merge_properties_with_same_key(self):
        output = make_output_iasio()
        comp_inputs = {}
        for i in range(self.NUM_INPUTS):
            alarm = Alarm.get_initial_alarmstate(Priority.HIGH).set() if i < self.THRESHOLD \
                else Alarm.get_initial_alarmstate()
            if i < self.THRESHOLD:
                inp_props = {"TestKey": "SET", f"InputAlarm{i}": str(i)}
            else:
                inp_props = {"TestKey": "CLEARED", f"InputAlarm{i}": str(i)}
            comp_inputs[f"InputAlarm{i}"] = make_alarm_iasio(
                f"InputAlarm{i}", alarm=alarm, props=inp_props)
        result = self._eval(comp_inputs, output)
        assert result.value.is_set()
        assert "TestKey" in result.props
        merged_values = result.props["TestKey"].split(",")
        assert len(merged_values) == self.THRESHOLD
        assert all(v == "SET" for v in merged_values)

    def test_ids_of_active_inputs_subset(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs(self.THRESHOLD), output)
        assert result.value.is_set()
        active_ids = set(result.props[INPUT_ALARMS_SET_PROP].split(","))
        expected_ids = {f"InputAlarm{i}" for i in range(self.THRESHOLD)}
        assert active_ids == expected_ids


class TestMultiplicityThresholdValidity:
    """Validity constraint logic — mirror of Scala tests (issue #201)."""

    THRESHOLD = 3
    NUM_INPUTS = 5

    def setup_method(self):
        self.manager = TfManager()
        props = {THRESHOLD_PROP: str(self.THRESHOLD)}
        inputs_info = [IasioInfo(f"InputAlarm{i}", IASType.ALARM)
                       for i in range(self.NUM_INPUTS)]
        output_info = IasioInfo("OutputAlarm", IASType.ALARM)
        success, error = self.manager.initialize(
            asceRunningId="test:run",
            asceId="test",
            pythonClassName=PYTHON_CLASS,
            validityTimeFrame=1000,
            props=props,
            templateInstance=None,
            inputsInfo=inputs_info,
            outputInfo=output_info,
        )
        assert success, f"Init failed: {error}"

    def teardown_method(self):
        self.manager.shutdown("test:run")

    def _eval(self, comp_inputs, actual_output):
        success, error, result = self.manager.eval("test:run", comp_inputs, actual_output)
        assert success, f"Eval failed: {error}"
        return result

    def _build_inputs_with_validity(self, n_set, make_valid_set):
        """Build inputs where SET alarms get reliability per make_valid_set.

        :param n_set: number of alarms set
        :param make_valid_set: set of indices whose SET alarms are RELIABLE
        """
        comp_inputs = {}
        for i in range(self.NUM_INPUTS):
            alarm = Alarm.get_initial_alarmstate(Priority.HIGH).set() if i < n_set \
                else Alarm.get_initial_alarmstate()
            if i < n_set:
                validity = Validity.RELIABLE if i in make_valid_set else Validity.UNRELIABLE
            else:
                validity = Validity.UNRELIABLE
            comp_inputs[f"InputAlarm{i}"] = make_alarm_iasio(
                f"InputAlarm{i}", alarm=alarm, validity=validity)
        return comp_inputs

    def _build_inputs_all_unreliable(self, n_set):
        comp_inputs = {}
        for i in range(self.NUM_INPUTS):
            alarm = Alarm.get_initial_alarmstate(Priority.HIGH).set() if i < n_set \
                else Alarm.get_initial_alarmstate()
            comp_inputs[f"InputAlarm{i}"] = make_alarm_iasio(
                f"InputAlarm{i}", alarm=alarm, validity=Validity.UNRELIABLE)
        return comp_inputs

    def test_empty_when_not_reliable_set(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs_all_unreliable(self.NUM_INPUTS), output)
        assert result.value.is_set()
        assert not getattr(result, '_validityConstraint', None)

    def test_empty_when_not_enough_reliable_set(self):
        output = make_output_iasio()
        result = self._eval(
            self._build_inputs_with_validity(self.NUM_INPUTS, {0, 1}), output)
        assert result.value.is_set()
        assert not getattr(result, '_validityConstraint', None)

    def test_nonempty_when_enough_reliable_set(self):
        output = make_output_iasio()
        result = self._eval(
            self._build_inputs_with_validity(self.NUM_INPUTS, {0, 1, 2}), output)
        assert result.value.is_set()
        constraint = getattr(result, '_validityConstraint', None)
        assert constraint is not None
        assert len(constraint) >= self.THRESHOLD

    def test_empty_when_no_validity_constraint_due_to_cleared(self):
        output = make_output_iasio()
        result = self._eval(self._build_inputs_all_unreliable(0), output)
        assert not result.value.is_set()
        assert not getattr(result, '_validityConstraint', None)
