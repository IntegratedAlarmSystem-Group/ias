# JEP → gRPC Phase 2 — Remaining Steps

## Objective

Complete the remaining steps to make the gRPC-based Python TF executor fully operational end-to-end.

## Current State (as of Nov 16 2026, end of session)

### Summary

The gRPC-based Python TF executor is **fully operational end-to-end**: ASCEs can run Python transfer functions through an external PyTfServer process over gRPC. All integration tests pass:

- **TestGrpcPythonExecutorIntegration** — 6 tests (executor-level Init/Eval/Shutdown over gRPC)
- **TestPyTF** — 17 tests (full ASCE lifecycle for every IAS type: LONG, INT, SHORT, BYTE, DOUBLE, FLOAT, BOOLEAN, CHAR, STRING, TIMESTAMP, ARRAYOFDOUBLES, ARRAYOFLONGS, ALARM + stress test of 15,000 calls)
- **TestMinMaxPyTF** — 4 tests (full ASCE lifecycle with the real `MinMaxThreshold` Python TF: initialize, eval, mode passthrough, threshold activation)

Total: **27 integration tests**, all passing, all self-managing the PyTfServer subprocess via `PyTfServerBase`.

### What's done beyond git HEAD

| Area | What was done | Why |
|---|---|---|
| **Python TF base files** | Copied `TransferFunction.py`, `IASIO.py`, `IasioInfo.py` from CompElement to `PyTfServer/src/main/python/IasTransferFunction/` | Make PyTfServer's `IasTransferFunction` package self-contained, avoiding PYTHONPATH ordering conflicts when both CompElement and PyTfServer packages are installed |
| **`MinMaxThreshold` default types** | Changed defaults from `int(sys.maxsize)` to `float(sys.maxsize)` in `MinMaxThreshold.py` | Match test expectations and avoid int/float comparison issues |
| **`IasTransferFunction/__init__.py`** | Added `from IasTransferFunction.MinMaxThreshold import MinMaxThreshold` | Ensure `MinMaxThreshold` is importable as `IasTransferFunction.MinMaxThreshold` |
| **Scala integration test relocation** | Moved `TestPyTF`, `TestMinMaxPyTF`, `TestGrpcPythonExecutorIntegration` from `src/test/scala` to `PyTfServer/src/integrationTest/scala/org/eso/ias/pytfserver/test/` | These tests require a running PyTfServer and cross-module communication — they are integration tests, not unit tests |
| **`runIntTests.sh`** | Updated to reference `org.eso.ias.pytfserver.test.*` package | Reflects relocated test classes |
| **`PyTfServer/build.gradle.kts`** | Added `java-library-distribution` and `scala` plugins; CompElement/BasicTypes/Tools dependencies; `integrationTest` Scala source set | Integration tests are Scala and depend on CompElement (for `GrpcPythonExecutor`, `ComputingElement`, types) — the `scala` plugin and dependencies are needed for compilation |
| **`implementation(libs.scala.library)`** | Added explicit dependency on Scala 3.9.0 library | Resolves Zinc compiler version conflict: `scalatest` brings transitive `scala3-library_3:3.1.3`, but Zinc must pick `3.9.0` to compile with `-java-output-version 21` (3.1.x compiler only accepts colon-separated version format) |
| **`CopyCompElementPyBase` Gradle task** | Created task to sync base `IasTransferFunction` files into build output | Ensures the self-contained files are in `build/lib/.../site-packages/` alongside the installed package |
| **`GrpcPythonExecutor.shutdown()`** | Added `channel.shutdown()` + `awaitTermination(5s)` + log line | Fixes gRPC `ManagedChannel` leak — channels were only cleaned up by JVM GC, producing `ManagedChannelOrphanWrapper` warnings |
| **`AGENTS.md`** | Updated integration test instructions for PyTfServer | Documents how to run integration tests (self-managed server via PyTfServerBase, no pre-running server needed) |

### Proto fix (DONE)

`grpc_tools.protoc`-generated stubs use flat imports (`import xxx_pb2`) that don't work from the installed package. A `fixPythonGrpcImports` build task and `fix_grpc_imports` script patch this after generation (both CompElement and BasicTypes).

The script must be run **twice per package** because two different file types import from different packages:

```
fix_grpc_imports <dir> <dep_package>  -g '*_pb2.py'       # cross-module (e.g. types_pb2 → IasProtobufTypes)
fix_grpc_imports <dir> <own_package>  -g '*_pb2_grpc.py'  # own-module  (e.g. pytf_service_pb2 → IasProtobufTfserver)
```

Running both is necessary — `_pb2.py` may import protos from a dependency package, while `_pb2_grpc.py` imports the sibling `_pb2.py` from the same package.

### Key design decisions

- **`PyTfServerBase` trait for server management** — Rather than having each test class manage its own PyTfServer subprocess, a shared trait (`PyTfServerBase`) encapsulates start/stop, port allocation, and PYTHONPATH extension. Tests just call `startServer()` in `beforeAll()` and `stopServer()` in `afterAll()`. This eliminates code duplication across 3 test classes and ensures consistent behavior.

- **Dynamic port allocation per test suite** — Each test class gets its own randomly-assigned port. This means all three test classes can run in parallel (when invoked via `iasRun` from `runIntTests.sh`) without port conflicts.

- **`iasPyTfServer` via PATH, not `python3 .../iasPyTfServer.py`** — The server script is invoked as `iasPyTfServer` (from `$IAS_ROOT/bin/`, on PATH via `ias-bash-profile`). This inherits the full PYTHONPATH and classpath that `iasRun` already sets up. No manual path construction needed — only the test Python modules directory (`src/test/python`) is prepended to PYTHONPATH so that `test_tf_module` is importable by the server.

- **`javaComp` created in `beforeAll()` for TestMinMaxPyTF** — The ComputingElement must be created *after* the server starts so it can read the server port. Creating it as a class field (eager initialization) meant port 0. The fix: declare as a nullable var and instantiate in `beforeAll()` after `startServer()`.

- **`serverProps()` helper** — `PyTfServerBase` provides `serverProps()` returning a Properties map with `pyTfServerAddr` set to the dynamic port. Test classes use this instead of `new Properties()`, ensuring the executor connects to the correct server.

- **`IasTransferFunction` self-contained in PyTfServer** — Rather than relying on CompElement's `IasTransferFunction` package at runtime, PyTfServer includes the base files (`TransferFunction.py`, `IASIO.py`, `IasioInfo.py`) in its own source tree. This avoids PYTHONPATH ordering issues where two installed packages could shadow each other. The `CopyCompElementPyBase` Gradle task syncs these files into the build output.

---

## Server Architecture

```
iasPyTfServer process (1 per Supervisor)
└── gRPC Server (insecure, localhost:port)
    └── PyTfService / tf_service.PyTfService (1 instance — gRPC servicer)
        └── TfManager (1 instance — singleton)
            └── tf_instances: Dict[asceRunningId, TransferFunction]
                ├── "SupervId:DAS-ID:COMP-ID:run1" → TF_A
                └── "SupervId:DAS-ID:COMP-ID2:run2" → TF_B
```

One `TfManager` per server process, shared across all ASCEs. Each ASCE identified by its unique `asceRunningId`. `tf_service.py` handles proto ↔ Python conversion and delegates to `TfManager`. `iasPyTfServer.py` is the CLI entry point.

---

## Plan

### Step 0 — Ping health check in tf_service.py (DONE)

Added `Ping` as a health check RPC to `tf_service.PyTfService`, preserving it when Step A replaces the committed Ping-only server with the real Initialize/Eval/Shutdown implementation. `Ping` returns `PingResponse(success=True)` and logs the number of active ASCE instances in the `TfManager`.

Corresponding unit tests in `test_server.py` start the server in-process with a random localhost port and use a Python gRPC client to verify two Ping calls succeed. Network tests spawn the server as a child process, connect over TCP on localhost, and verify Ping with clean teardown via `start_new_session=True` + `SIGINT` to `os.killpg`. All 32 unit tests pass.

**Files changed:**

| File | Change |
|---|---|
| `PyTfServer/src/main/python/IasPyTfServer/tf_service.py` | Added `PingResponse` import; added `Ping(self, request, context)` method to `PyTfService` |
| `PyTfServer/src/test/python/test_server.py` | **NEW** — 2 in-process unit tests + 2 network-level tests (all passing) |

### Step A — Align proto + GrpcPythonExecutor with developed code

The developed `tf_service.py` / `tf_manager.py` implement Initialize/Eval/Shutdown. The committed proto is now aligned, but `GrpcPythonExecutor` and `iasPyTfServer.py` still need updating.

1. **`pytf_service.proto` (DONE)** — Added `Initialize`, `Eval`, `Shutdown` RPCs with their request/response messages. `InitializeRequest` uses `optional string templateInstance` for proto3 field presence. `EvalRequest` uses `map<string, IasIO>` for inputs, `ShutdownRequest` carries `asceRunningId`. All 32 Python unit tests validate the proto alignment.
2. **`GrpcPythonExecutor.scala`** — `initialize()`, `eval()`, and `shutdown()` all call real RPCs using `ProtoUtil` (DONE).
3. **`iasPyTfServer.py`** — Replaced inline Ping handler with `PyTfService` from `tf_service.py` (DONE).
4. **`build.gradle.kts` (both CompElement and BasicTypes) (DONE)** — `fixPythonGrpcImports` tasks wired using `fix_grpc_imports` script. `PyTfServer/pytest` depends on `:CompElement:CopyPyProtoMods` to ensure fresh stubs.

### Step B — Build fix: flat import in generated gRPC stubs (DONE) – merged into Step A.1

`grpc_tools.protoc` generates `import pytf_service_pb2` instead of `from IasProtobufTfserver import pytf_service_pb2`. This breaks when the stubs are installed as a package.

- **New file**: `CompElement/src/main/python/fix_grpc_imports` — patches flat imports to package-relative form. Accepts `-g` flag to target either `*_pb2.py` (cross-module) or `*_pb2_grpc.py` (own-module). Must be run **twice**: once for each pattern with the appropriate target package.
- **CompElement/build.gradle.kts**: `fixPythonGrpcImportsPb2` (cross-module, target `IasProtobufTypes`) → `fixPythonGrpcImportsGrpc` (own-module, target `IasProtobufTfserver`) → `CopyPyProtoMods` (depends on the second fix). Chain: `generatePythonProto` → fix → copy.
- **BasicTypes/build.gradle.kts**: `fixPythonGrpcImports` (own-module, target `IasProtobufTypes`) → `CopyPyProtoMods`. References the same script from CompElement.

**Verified**: After `CopyPyProtoMods`, the copied stubs in `build/lib/.../site-packages/IasProtobufTfserver/` have correct imports. Network tests pass with build-generated stubs.

### Step A.4 — Python Server Lifecycle Tests (DONE)

Added `TestPyTfServerLifecycle` class in `test_server.py` with 11 tests covering the full Initialize/Eval/Shutdown lifecycle via gRPC:

| Test | Coverage |
|---|---|
| `test_initialize_and_shutdown_lifecycle` | Full Init → Shutdown roundtrip via gRPC |
| `test_initialize_verifies_args` | Verifies asceId, asceRunningId, validityTimeFrame, props reach the TF |
| `test_initialize_with_template_instance` | Verifies optional `templateInstance` string→int conversion reaches the TF |
| `test_double_initialize_fails` | Second Initialize for same asceRunningId returns error |
| `test_shutdown_nonexistent_fails` | Shutdown for non-existent asceRunningId returns error |
| `test_shutdown_removes_tf_instance` | Shutdown removes TF from TfManager's `tf_instances` dict |
| `test_eval_passthrough_returns_input_value` | Eval passes input through unchanged (PassThroughTF) |
| `test_eval_doubling_doubles_input_value` | Eval doubles input value (DoublingTF) |
| `test_eval_uninitialized_fails` | Eval returns error for non-existent asceRunningId |
| `test_eval_double_roundtrip` | Multiple consecutive Eval calls on the same TF work correctly |

Also fixed `tf_service.py` bug: `request.hasTemplateInstance` → `request.HasField('templateInstance')` (protobuf 4.x API).

### Step A.5 — Scala Integration Tests (DONE)

Extended `TestGrpcPythonExecutorIntegration.scala` with 6 integration tests (3 original + 3 Eval) that start a real `iasPyTfServer.py` subprocess and verify the Scala `GrpcPythonExecutor` communicates correctly:

| Test | Coverage |
|---|---|
| `initialize and shutdown via gRPC` | GrpcPythonExecutor → real PyTfServer Init/Shutdown roundtrip |
| `fail on double initialize` | Second initialize through executor throws exception |
| `receive correct props and asceId in Python TF` | Uses raw gRPC stub to verify props flow through Initialize/Shutdown |
| `eval pass through returns input value` | GrpcPythonExecutor.eval() → real PyTfServer with PassThroughTF |
| `eval doubling tf doubles input value` | GrpcPythonExecutor.eval() with DoublingTF verifies computation |
| `eval fails when not initialized` | GrpcPythonExecutor.eval() throws exception when initialize() was not called |

**Files changed (Steps A.2–A.5):**

| File | Change |
|---|---|
| `CompElement/src/main/scala/.../GrpcPythonExecutor.scala` | `initialize()`, `eval()`, `shutdown()` all call real gRPC RPCs using `ProtoUtil` (fully operational) |
| `PyTfServer/src/main/python/iasPyTfServer.py` | Replaced inline Ping handler with `from IasPyTfServer.tf_service import PyTfService` |
| `PyTfServer/src/main/python/IasPyTfServer/tf_service.py` | Fixed `hasTemplateInstance` → `HasField('templateInstance')` for protobuf 4.x |
| `PyTfServer/src/test/python/test_server.py` | Added `TestPyTfServerLifecycle` class (6 tests) |
| `PyTfServer/src/test/scala/.../TestGrpcPythonExecutorIntegration.scala` | Extended with 3 Eval tests (6 total) |

### Step A.7 — Move integration tests to `src/integrationTest` (DONE)

All three Scala integration tests were relocated from `src/test/scala` to `PyTfServer/src/integrationTest/scala/org/eso/ias/pytfserver/test/`:

- **`TestGrpcPythonExecutorIntegration`** — 6 tests (executor-level gRPC communication)
- **`TestPyTF`** — 17 tests (full ASCE lifecycle, every IAS type, stress test)
- **`TestMinMaxPyTF`** — 4 tests (MinMaxThreshold threshold activation with real Python TF)

**`PyTfServerBase`** (NEW) — shared trait that starts/stops the PyTfServer subprocess per test suite. Invokes `iasPyTfServer` from PATH (set by `ias-bash-profile`), allocates a random port, prepends `src/test/python` to PYTHONPATH, and provides `serverProps()` with the correct `pyTfServerAddr`.

**`runIntTests.sh`** — invokes all three test classes via `iasRun`. No pre-running server needed — each test class manages its own server through `PyTfServerBase`.

**`PyTfServer/build.gradle.kts`** — retains `scala` plugin, CompElement/BasicTypes/Tools dependencies (needed by integration tests), and `integrationTest` Scala source set. (Step G's plan to revert to `base` plugin only is abandoned — the integration tests are Scala and require CompElement.)

**`GrpcPythonExecutor.shutdown()`** — fixed gRPC channel leak: `channel.shutdown()` + `awaitTermination(5s)` now called on executor shutdown. No more `ManagedChannelOrphanWrapper` warnings.

**`AGENTS.md`** — updated integration test instructions to document PyTfServer's self-managed server lifecycle.

### Verification (updated)

- `./gradlew :CompElement:test` — all Scala/Java tests pass
- `./gradlew :PyTfServer:pytest` — 60 passed (pytest unit tests)
- `./gradlew :PyTfServer:compileIntegrationTestScala` — compiles 3 integration test classes (PyTfServerBase, TestGrpcPythonExecutorIntegration, TestPyTF, TestMinMaxPyTF)
- Integration tests (after `install`, via `runIntTests.sh`): **27 tests pass** (6 + 17 + 4)

### Step A.6 — Refactor shared test paths (DONE)

Eliminated duplicated `sys.path` setup across three Python test files. Created `PyTfServer/src/test/python/_test_paths.py` that adds only the generated protobuf directories to `sys.path` at import time (Gradle already provides all subprojects' site-packages and `src/test/python` via the PYTHONPATH environment variable). Each test file now does a single `import _test_paths`.

**Files changed (Steps A.2–A.7, full list):**

| File | Change |
|---|---|
| `CompElement/src/main/scala/.../GrpcPythonExecutor.scala` | Full gRPC RPCs; `shutdown()` now also shuts down the `ManagedChannel` with log |
| `PyTfServer/src/main/python/IasTransferFunction/__init__.py` | Exports `MinMaxThreshold` |
| `PyTfServer/src/main/python/IasTransferFunction/MinMaxThreshold.py` | Default thresholds use `float(sys.maxsize)` instead of `int` |
| `PyTfServer/src/main/python/IasTransferFunction/TransferFunction.py` | Copied from CompElement (self-contained base) |
| `PyTfServer/src/main/python/IasTransferFunction/IASIO.py` | Copied from CompElement (self-contained base) |
| `PyTfServer/src/main/python/IasTransferFunction/IasioInfo.py` | Copied from CompElement (self-contained base) |
| `PyTfServer/src/main/python/iasPyTfServer.py` | Replaced inline Ping handler with `from IasPyTfServer.tf_service import PyTfService` |
| `PyTfServer/src/main/python/IasPyTfServer/tf_service.py` | Fixed `hasTemplateInstance` → `HasField('templateInstance')` for protobuf 4.x |
| `PyTfServer/src/test/python/test_server.py` | Added Eval tests to `TestPyTfServerLifecycle`; path setup via `_test_paths` |
| `PyTfServer/src/test/python/test_service.py` | Path setup via `_test_paths` |
| `PyTfServer/src/test/python/test_tf_manager.py` | Path setup via `_test_paths` |
| `PyTfServer/src/test/python/_test_paths.py` | **NEW** — shared `sys.path` setup (adds generated proto dirs only) |
| `PyTfServer/src/test/python/test_tf_module.py` | Mock TF implementations (PassThroughTF, DoublingTF) |
| `PyTfServer/src/test/python/TestTF.py` | TestTransferFunction (pass-through TF for type coverage tests) |
| `PyTfServer/src/integrationTest/scala/.../PyTfServerBase.scala` | **NEW** — shared trait for PyTfServer subprocess lifecycle |
| `PyTfServer/src/integrationTest/scala/.../TestGrpcPythonExecutorIntegration.scala` | 6 tests, uses `PyTfServerBase` (moved from `src/test/scala`) |
| `PyTfServer/src/integrationTest/scala/.../TestPyTF.scala` | 17 tests, uses `PyTfServerBase` (moved from CompElement) |
| `PyTfServer/src/integrationTest/scala/.../TestMinMaxPyTF.scala` | 4 tests, uses `PyTfServerBase` (moved from CompElement) |
| `PyTfServer/src/integrationTest/runIntTests.sh` | Updated class references to `org.eso.ias.pytfserver.test.*` |
| `PyTfServer/build.gradle.kts` | `scala` plugin, CompElement/BasicTypes/Tools deps, `integrationTest` Scala source set |
| `CompElement/src/main/python/fix_grpc_imports` | Patches flat protobuf imports to package-relative form |
| `AGENTS.md` | Updated integration test instructions for PyTfServer |

### Step C — Step 4: GrpcChannelManager

Each `GrpcPythonExecutor` currently creates its own `ManagedChannel`. Plan: shared channel with ref counting so all ASCEs in a Supervisor share one OS connection.

### Step H — Fail-fast: check PyTfServer availability before using it

When the Supervisor (or an ASCE / DASU that needs Python TF execution) starts, it must verify that the PyTfServer is reachable before proceeding.  If the server is not available, the component should terminate with a clear error (fail-fast).

**Plan:**

1. Add a `ping` (health check) call in `GrpcPythonExecutor` — e.g., in the constructor or in `initialize()`, send a `Ping` RPC before any Initialize/Eval work.  If the ping fails (connection refused, timeout, or `success=false`), throw an exception with a clear message indicating the PyTfServer address and that the server must be running.
2. Ensure `Supervisor` or `DasuImpl` catches this early failure and aborts startup rather than continuing with broken Python TF paths.

This provides a clear diagnostic at startup time rather than a silent failure later when `eval` is called on an unreachable server.

### Step D — Step 6: Configuration threading

Wire `pyTfServerAddr` from `SupervisorDao` (already defined, validated, unused) down to each `GrpcPythonExecutor` via Supervisor → DasuImpl → ASCE properties.

### Step E — Re-enable Python TF tests (DONE — tests moved to PyTfServer integration tests)

`TestPyTF` and `TestMinMaxPyTF` were moved from CompElement to `PyTfServer/src/integrationTest/` (Step A.7). They no longer require a pre-running server on `localhost:50051` — each test class manages its own PyTfServer subprocess through `PyTfServerBase`. Both test suites pass (17 + 4 = 21 tests).

### Step G — Move integration tests to `src/integrationTest` (DONE — superseded by Step A.7)

All integration tests are now under `PyTfServer/src/integrationTest/scala/org/eso/ias/pytfserver/test/`, managed by `PyTfServerBase`, and run via `runIntTests.sh` through `iasRun`. See Step A.7 for details.

**Deviation from original plan:** The `PyTfServer/build.gradle.kts` was NOT reverted to `base` plugin only. The integration tests are Scala and depend on CompElement classes (`GrpcPythonExecutor`, `ComputingElement`, types). The `scala` plugin, CompElement/BasicTypes/Tools dependencies, and `integrationTest` Scala source set are required and will remain.

### Step F — Step 8: Full integration tests (later)

Full end-to-end: Supervisor starts, connects to external PyTfServer, evaluates real Python TFs.
