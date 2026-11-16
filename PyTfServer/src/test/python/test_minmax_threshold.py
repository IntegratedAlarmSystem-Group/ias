#! /usr/bin/env python3
"""Unit tests for MinMaxThreshold Python TF in isolation.

Tests the MinMaxThreshold transfer function directly via TfManager,
without involving gRPC or the external PyTfServer process.  The tests
mirror the scenarios covered by the Scala and Java versions
(TestMinMaxThreshold.scala) — high threshold activation with hysteresis,
low threshold activation with hysteresis, mode propagation, and the
actualValue property.

Run with::

    pytest PyTfServer/src/test/python/test_minmax_threshold.py
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


def make_input_iasio(value, mode=OperationalMode.UNKNOWN):
    """Build an input IASIO of type LONG with the given value and mode."""
    return IASIO(
        id="MPointID",
        runningId="test:run:MPointID",
        mode=mode,
        iasType=IASType.LONG,
        validity=Validity.RELIABLE,
        value=value,
        productionTStamp=1,
        props={}
    )


def make_output_iasio():
    """Build the initial output IASIO of type ALARM (no alarm value set)."""
    return IASIO(
        id="OutputId",
        runningId="test:run:OutputId",
        mode=OperationalMode.UNKNOWN,
        iasType=IASType.ALARM,
        validity=Validity.RELIABLE,
        value=None,
        productionTStamp=1,
        props={}
    )


def check_alarm(result, expected_is_set, expected_priority=Priority.CRITICAL):
    """Assert that the output carries the expected alarm state and priority."""
    assert result is not None
    assert result.value is not None
    alarm = result.value
    assert alarm.is_set() == expected_is_set, (
        f"Expected is_set={expected_is_set} but got {alarm.is_set()}"
    )
    assert alarm.priority == expected_priority, (
        f"Expected priority={expected_priority} but got {alarm.priority}"
    )


class TestMinMaxThreshold:
    """Test MinMaxThreshold TF in isolation via TfManager."""

    HIGH_ON = 50
    HIGH_OFF = 40
    LOW_ON = -20
    LOW_OFF = -10

    PROPS = {
        "HighOn": str(HIGH_ON),
        "HighOff": str(HIGH_OFF),
        "LowOn": str(LOW_ON),
        "LowOff": str(LOW_OFF),
        "Priority": Priority.CRITICAL.to_string(),
    }

    PYTHON_CLASS = "IasTransferFunction.MinMaxThreshold"

    def setup_method(self):
        self.manager = TfManager()
        self._init_minmax()

    def teardown_method(self):
        self.manager.shutdown("test:run")

    def _init_minmax(self):
        """Initialize the MinMaxThreshold TF via TfManager."""
        inputs_info = [IasioInfo("MPointID", IASType.LONG)]
        output_info = IasioInfo("OutputId", IASType.ALARM)

        success, error = self.manager.initialize(
            asceRunningId="test:run",
            asceId="test",
            pythonClassName=self.PYTHON_CLASS,
            validityTimeFrame=1000,
            props=self.PROPS,
            templateInstance=None,
            inputsInfo=inputs_info,
            outputInfo=output_info,
        )
        assert success, f"Initialize failed: {error}"

    def _eval(self, value, actual_output):
        """Convenience: run eval with a single LONG input value."""
        comp_inputs = {"MPointID": make_input_iasio(value)}
        success, error, result = self.manager.eval("test:run", comp_inputs, actual_output)
        assert success, f"Eval failed: {error}"
        return result

    # -- constructor / initialization --

    def test_init_loads_thresholds_from_props(self):
        tf = self.manager.tf_instances["test:run"]
        assert tf.highOn == self.HIGH_ON
        assert tf.highOff == self.HIGH_OFF
        assert tf.lowOn == self.LOW_ON
        assert tf.lowOff == self.LOW_OFF
        assert tf.priority == Priority.CRITICAL

    def test_default_thresholds_when_props_missing(self):
        import sys
        default_props = {}
        other_mgr = TfManager()
        inputs_info = [IasioInfo("MPointID", IASType.LONG)]
        output_info = IasioInfo("OutputId", IASType.ALARM)
        success, error = other_mgr.initialize(
            asceRunningId="test:default",
            asceId="test",
            pythonClassName=self.PYTHON_CLASS,
            validityTimeFrame=1000,
            props=default_props,
            templateInstance=None,
            inputsInfo=inputs_info,
            outputInfo=output_info,
        )
        assert success, f"Init failed: {error}"
        tf = other_mgr.tf_instances["test:default"]
        assert tf.highOn == float(sys.maxsize)
        assert tf.lowOn == -float(sys.maxsize)
        other_mgr.shutdown("test:default")

    # -- high threshold activation with hysteresis --

    def test_normal_value_does_not_activate(self):
        output = make_output_iasio()
        result = self._eval(1, output)
        check_alarm(result, expected_is_set=False)

    def test_high_activation(self):
        output = make_output_iasio()
        result = self._eval(55, output)
        check_alarm(result, expected_is_set=True)

    def test_remain_active_above_high_off(self):
        output = make_output_iasio()
        result = self._eval(55, output)
        check_alarm(result, expected_is_set=True)
        result = self._eval(45, result)
        check_alarm(result, expected_is_set=True)

    def test_deactivate_below_high_off(self):
        output = make_output_iasio()
        result = self._eval(55, output)
        check_alarm(result, expected_is_set=True)
        result = self._eval(35, output)
        check_alarm(result, expected_is_set=False)

    # -- low threshold activation with hysteresis --

    def test_low_activation(self):
        output = make_output_iasio()
        result = self._eval(-25, output)
        check_alarm(result, expected_is_set=True)

    def test_between_low_off_and_low_on_does_not_activate(self):
        output = make_output_iasio()
        result = self._eval(-15, output)
        check_alarm(result, expected_is_set=False)

    def test_remain_active_below_low_off(self):
        output = make_output_iasio()
        result = self._eval(-25, output)
        check_alarm(result, expected_is_set=True)
        result = self._eval(-15, result)
        check_alarm(result, expected_is_set=True)

    def test_deactivate_above_low_off(self):
        output = make_output_iasio()
        result = self._eval(-25, output)
        check_alarm(result, expected_is_set=True)
        result = self._eval(0, output)
        check_alarm(result, expected_is_set=False)

    # -- combined high then low sequence (matches Scala test) --

    def test_full_high_threshold_cycle(self):
        output = make_output_iasio()
        result = self._eval(5, output)
        check_alarm(result, expected_is_set=False)

        result = self._eval(100, output)
        check_alarm(result, expected_is_set=True)

        result = self._eval(150, result)
        check_alarm(result, expected_is_set=True)

        result = self._eval(40, result)
        check_alarm(result, expected_is_set=True)

        result = self._eval(10, output)
        check_alarm(result, expected_is_set=False)

    def test_full_low_threshold_cycle(self):
        output = make_output_iasio()
        result = self._eval(-15, output)
        check_alarm(result, expected_is_set=False)

        result = self._eval(-30, output)
        check_alarm(result, expected_is_set=True)

        result = self._eval(-40, result)
        check_alarm(result, expected_is_set=True)

        result = self._eval(-15, result)
        check_alarm(result, expected_is_set=True)

        result = self._eval(0, output)
        check_alarm(result, expected_is_set=False)

    # -- operational mode propagation --

    def test_mode_propagated_from_input(self):
        output = make_output_iasio()
        result = self._eval(1, output)
        assert result.mode == OperationalMode.UNKNOWN

        result = self._eval(1, output)
        input_operational = make_input_iasio(1, OperationalMode.OPERATIONAL)
        comp_inputs = {"MPointID": input_operational}
        success, error, result = self.manager.eval("test:run", comp_inputs, output)
        assert success, f"Eval failed: {error}"
        assert result.mode == OperationalMode.OPERATIONAL

    # -- actualValue property --

    def test_actual_value_property_set(self):
        output = make_output_iasio()
        result = self._eval(100, output)
        assert result.props is not None
        assert "actualValue" in result.props
        assert result.props["actualValue"] == "100"

    def test_alarm_priority_from_props(self):
        output = make_output_iasio()
        result = self._eval(55, output)
        check_alarm(result, expected_is_set=True, expected_priority=Priority.CRITICAL)

    def test_increase_value_keeps_alarm_set(self):
        output = make_output_iasio()
        result = self._eval(55, output)
        check_alarm(result, expected_is_set=True)
        result = self._eval(200, result)
        check_alarm(result, expected_is_set=True)

    def test_decrease_below_high_off_clears(self):
        output = make_output_iasio()
        result = self._eval(55, output)
        check_alarm(result, expected_is_set=True)
        result = self._eval(10, output)
        check_alarm(result, expected_is_set=False)
