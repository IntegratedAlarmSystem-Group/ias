"""Add generated protobuf directories to sys.path at import time.

The Gradle pytest task already sets PYTHONPATH with all subprojects'
site-packages and ``src/test/python``.  Only the two ``generatePythonProto``
output directories are missing and are added here.
"""

import sys
import os

_base = os.path.dirname(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
)

for _p in (
    os.path.join(_base, "BasicTypes", "build", "generated", "python", "IasProtobufTypes"),
    os.path.join(_base, "CompElement", "build", "generated", "python", "IasProtobufTfserver"),
):
    if _p not in sys.path:
        sys.path.insert(0, _p)
del _p, _base
