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

for path in [comp_element_build, basictypes_build, tools_build, pytfserver_build, pytfserver_src]:
    if path not in sys.path:
        sys.path.insert(0, path)

from IasPyTfServer.tf_manager import TfManager


class TestTfManager:

    def setup_method(self):
        self.manager = TfManager()

    def test_initialize_and_shutdown(self):
        success, error = self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={"key1": "val1"},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )
        assert success is True
        assert error == ""
        assert "test:run1" in self.manager.tf_instances

        success, error = self.manager.shutdown("test:run1")
        assert success is True
        assert error == ""
        assert "test:run1" not in self.manager.tf_instances

    def test_double_initialize_fails(self):
        success, error = self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )
        assert success is True

        success, error = self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )
        assert success is False
        assert "already initialized" in error

        self.manager.shutdown("test:run1")

    def test_multiple_asces_concurrent(self):
        success, _ = self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test1",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )
        assert success is True

        success, _ = self.manager.initialize(
            asceRunningId="test:run2",
            asceId="test2",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=2000,
            props={},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )
        assert success is True

        assert len(self.manager.tf_instances) == 2

        self.manager.shutdown("test:run1")
        assert len(self.manager.tf_instances) == 1
        assert "test:run2" in self.manager.tf_instances

        self.manager.shutdown("test:run2")
        assert len(self.manager.tf_instances) == 0

    def test_shutdown_nonexistent_fails(self):
        success, error = self.manager.shutdown("nonexistent:run")
        assert success is False
        assert "not found" in error

    def test_eval_nonexistent_fails(self):
        success, error, result = self.manager.eval("nonexistent:run", {}, None)
        assert success is False
        assert "not initialized" in error
        assert result is None

    def test_eval_with_passthrough_tf(self):
        from IasTransferFunction.IASIO import IASIO
        from IasBasicTypes.OperationalMode import OperationalMode
        from IasBasicTypes.Validity import Validity
        from IasBasicTypes.IasType import IASType

        self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )

        actual_output = IASIO(
            id="out",
            runningId="test:run1:out",
            mode=OperationalMode.OPERATIONAL,
            iasType=IASType.LONG,
            validity=Validity.RELIABLE,
            value=0,
            productionTStamp=1000,
            props={}
        )

        comp_inputs = {
            "in1": IASIO(
                id="in1",
                runningId="test:run1:in1",
                mode=OperationalMode.OPERATIONAL,
                iasType=IASType.LONG,
                validity=Validity.RELIABLE,
                value=42,
                productionTStamp=1000,
                props={}
            )
        }

        success, error, result = self.manager.eval("test:run1", comp_inputs, actual_output)
        assert success is True
        assert error == ""
        assert result is not None
        assert result.value == 42

        self.manager.shutdown("test:run1")

    def test_eval_with_doubling_tf(self):
        from IasTransferFunction.IASIO import IASIO
        from IasBasicTypes.OperationalMode import OperationalMode
        from IasBasicTypes.Validity import Validity
        from IasBasicTypes.IasType import IASType

        self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="test_tf_module.DoublingTF",
            validityTimeFrame=1000,
            props={},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )

        actual_output = IASIO(
            id="out",
            runningId="test:run1:out",
            mode=OperationalMode.OPERATIONAL,
            iasType=IASType.LONG,
            validity=Validity.RELIABLE,
            value=0,
            productionTStamp=1000,
            props={}
        )

        comp_inputs = {
            "in1": IASIO(
                id="in1",
                runningId="test:run1:in1",
                mode=OperationalMode.OPERATIONAL,
                iasType=IASType.LONG,
                validity=Validity.RELIABLE,
                value=21,
                productionTStamp=1000,
                props={}
            )
        }

        success, error, result = self.manager.eval("test:run1", comp_inputs, actual_output)
        assert success is True
        assert result.value == 42

        self.manager.shutdown("test:run1")

    def test_invalid_class_name(self):
        success, error = self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="nonexistent.module.NonExistentTF",
            validityTimeFrame=1000,
            props={},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )
        assert success is False
        assert error != ""

    def test_props_passed_to_tf(self):
        success, _ = self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={"threshold": "42", "name": "mytf"},
            templateInstance=None,
            inputsInfo=[],
            outputInfo=None
        )
        assert success is True
        tf = self.manager.tf_instances["test:run1"]
        assert tf.props.get("threshold") == "42"
        assert tf.props.get("name") == "mytf"

        self.manager.shutdown("test:run1")

    def test_template_instance_passed(self):
        success, _ = self.manager.initialize(
            asceRunningId="test:run1",
            asceId="test",
            pythonClassName="test_tf_module.PassThroughTF",
            validityTimeFrame=1000,
            props={},
            templateInstance=3,
            inputsInfo=[],
            outputInfo=None
        )
        assert success is True
        tf = self.manager.tf_instances["test:run1"]
        assert tf.instance == 3

        self.manager.shutdown("test:run1")
