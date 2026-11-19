# JEP → gRPC Phase 2 — Remaining Steps

## Objective

Complete the remaining steps to make the gRPC-based Python TF executor fully operational end-to-end.

## Current State (as of Nov 20 2026, end of session)

### Summary

The gRPC-based Python TF executor is **fully operational end-to-end**: ASCEs can run Python transfer functions through an external PyTfServer process over gRPC. All integration tests pass:

- **TestGrpcPythonExecutorIntegration** — 6 tests (executor-level Init/Eval/Shutdown over gRPC)
- **TestPyTF** — 17 tests (full ASCE lifecycle for every IAS type: LONG, INT, SHORT, BYTE, DOUBLE, FLOAT, BOOLEAN, CHAR, STRING, TIMESTAMP, ARRAYOFDOUBLES, ARRAYOFLONGS, ALARM + stress test of 15,000 calls)
- **TestMinMaxPyTF** — 4 tests (full ASCE lifecycle with the real `MinMaxThreshold` Python TF: initialize, eval, mode passthrough, threshold activation)
- **SupervisorPyTfTest** — 2 tests (full Supervisor-level end-to-end: in-process Supervisor + external PyTfServer subprocess + 3 DASUs (2 Python TFs + 1 Scala TF) + live Kafka data flow)

Total: **29 integration tests**, all passing. PyTfServer tests manage the PyTfServer subprocess via `PyTfServerBase`. The Supervisor-level test uses an **in-process Supervisor** (instantiated via `new Supervisor(...)`) with an external PyTfServer subprocess.

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
| **`DasuImpl.cleanUp()`** | Added `asces.foreach(_.shutdown())` before releasing publisher | Graceful ASCE shutdown on Supervisor Ctrl-C — sends gRPC Shutdown RPC to PyTfServer to remove TFs from registry |
| **`TfManager.initialize()`** | Auto-shutdowns orphaned TF on duplicate `asceRunningId`, then re-attempts init | Handles unclean Supervisor crash/KILL — old TF is cleaned before new Supervisor re-initializes |
| **`AGENTS.md`** | Updated integration test instructions for PyTfServer | Documents how to run integration tests (self-managed server via PyTfServerBase, no pre-running server needed) |
| **SupervisorPyTfTest** | Supervisor instantiated in-process via `new Supervisor(...)` (not via ProcessBuilder) with a factory passing `pyTfServerAddr` to `DasuImpl.apply` | Follows `SupervisorWithKafkaTest` pattern; cleaner lifecycle management via `supervisor.close()`. PyTfServer still starts as a subprocess. Alarm map keys by `iasio.id` instead of `fullRunningId`. `waitAlarms` accepts a list of expected alarm IDs. Consumer type filter removed to capture all IASIOs. |

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

### Deployment model

PyTfServer has **no entry in the CDB**. It is started independently by the operator (or systemd/supervisor) before the Supervisor, taking the port from the command line (`--port`). The Supervisor learns the PyTfServer address from `supervDao.getPyTfServerAddr()` (already defined in the `SUPERVISOR` CDB entry as `pyTfServerAddr: "host:port"`) and wires it down through DasuImpl → ASCE → GrpcPythonExecutor. One PyTfServer process per Supervisor, typically co-located on the same host.

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
- `./gradlew :PyTfServer:pytest` — 91 passed (pytest unit tests, including 31 for MultiplicityThreshold)
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

### Step D.1 — `JsonSupervisorDao.equals()/hashCode()` bug fix (DONE)

`pyTfServerAddr` was missing from `JsonSupervisorDao.hashCode()` and `equals()`, meaning two instances with the same id, hostName, logLevel, and dasusToDeploy but different `pyTfServerAddr` values compared as equal. Fixed by adding `pyTfServerAddr` to both methods.

### Step D.2 — Fail-fast ping in Supervisor constructor (DONE, needs cleanup)

When `pyTfServerAddr` is defined in the CDB, the Supervisor constructor sends a gRPC `Ping` RPC to the PyTfServer at `host:port`. If the ping fails, the Supervisor refuses to start with a clear error. When `pyTfServerAddr` is empty (default, most CDBs), the check is skipped entirely — existing Supervisors are unaffected.

**Refactored:** The `Ping` logic lives in `GrpcPythonExecutor.checkServerRunning(serverAddr)` (CompElement companion object) rather than in `Supervisor.scala`. This keeps gRPC dependencies confined to CompElement — the Supervisor only calls the static method via `implementation(project(":CompElement"))` with no direct gRPC imports. The initial implementation had inline gRPC code in Supervisor and explicit gRPC dependencies in its `build.gradle.kts` (needed because the Scala 3 compiler failed to resolve protobuf inner classes like `GeneratedMessage$Builder` from transitive deps alone), but this was reverted after the refactoring proved unnecessary.

**TODO (Step D.2 cleanup):** The ping failure currently uses `sys.exit(1)` (`Supervisor.scala:163-167`), which is not clean — it bypasses `AutoCloseable.close()` and can leak resources already allocated (e.g. `CdbReader`, `KafkaSubscriber`, `SimpleStringProducer`). Fix: throw an exception from the constructor so `main` can catch it in a `try/catch`, release resources with `reader.shutdown()` + `supervisor.close()` (if constructed), and exit gracefully.

### Step D.3 — DasuImpl constructor + apply overload (DONE)

Added `pyTfServerAddr: Option[String] = None` as a 7th parameter to `DasuImpl` constructor and a 7-arg overload of `apply`. The 6-arg `apply` delegates to the 7-arg version with `None`. All existing 6-arg callers (DistributedUnit tests, Supervisor unit tests, integration tests) compile and run unchanged via the default parameter.

### Step D.4 — Supervisor main reads address from CDB (DONE)

In `Supervisor.main`, before creating the DASU factory, the Supervisor reads `pyTfServerAddr` from the CDB `SupervisorDao` and passes it through the factory lambda to the 7-arg `DasuImpl.apply`. The factory type signature `(DasuDao, Identifier, OutputPublisher, InputSubscriber) => Dasu` remains unchanged — the address is captured from the `main` scope.

### Step L — Graceful shutdown of ASCEs on Supervisor stop (DONE)

Two changes to handle TF orphaning when the Supervisor stops or crashes:

**Step L.1 — DasuImpl.cleanUp() calls ASCE shutdown (clean stop, e.g. Ctrl-C)**

`DasuImpl.cleanUp()` was missing the critical step of shutting down its ASCEs. When the Supervisor was terminated via Ctrl-C, `Supervisor.close()` called `dasus.foreach(_.cleanUp())`, but `DasuImpl.cleanUp()` only cleaned up the statistics collector, subscriber, and publisher — it never called `shutdown()` on any of its ASCEs.

**Consequence (clean stop):** `ComputingElement.shutdown()` is now called, which triggers `GrpcPythonExecutor.shutdown()` → gRPC `Shutdown` RPC → `TfManager.shutdown()` on PyTfServer, removing the TF from `tf_instances`.

**Step L.2 — TfManager.initialize() auto-cleans orphaned TFs (unclean stop, e.g. crash/KILL)**

For an unclean Supervisor termination (SIGKILL, crash, power loss), Step L.1's shutdown chain is never reached. The `asceRunningId` is purely static (derived from CDB names: `SupervisorId@DasuId@AsceId`), so after restart the new Supervisor sends `initialize` with the same ID and PyTfServer would reject it.

**Fix:** `TfManager.initialize()` now detects an already-present TF, shuts it down via `self.shutdown(asceRunningId)`, and re-attempts the initialization. A second collision (impossible after shutdown) is still rejected to protect against genuine errors. This is safe because the Scala side already guards against double initialize (`assert(!initialized)` in `TransferFunctionSetting`, state machine assertion in `ComputingElement`).

The Scala side (`GrpcPythonExecutor.initialize()`) does not need changes — the cleanup is handled on PyTfServer.

**Step L.1 — Fix:** `DasuImpl.cleanUp()` now iterates over all ASCEs and calls `shutdown()` on each before cleaning up the publisher. Each call is wrapped in `Try` so a failure on one ASCE does not prevent the others from shutting down.

The state machine already prevents TF evaluation after shutdown — `ShuttingDown` and `Closed` are both in `AsceStates.inhibitorStates`, so `canRunTF()` returns `false`. No additional guard in `PythonTransfer.transfer()` or `ScalaTransfer.transfer()` is needed.

**Alternatives considered and discarded:**

1. **Idempotent shutdown in GrpcPythonExecutor.initialize()** — send a `shutdown` RPC before `initialize` on the Scala side. Simple one-line change, but shifts the cleanup logic away from the server where the orphaned state lives. Rejected in favor of handling it on PyTfServer.

2. **Generation counter in runningId** — add a runtime-specific component (PID, timestamp, UUID) to the `asceRunningId` so each Supervisor instance gets a fresh ID. This requires modifying the fundamental `Identifier` class in BasicTypes — `runningId` is used everywhere (Kafka topics, ASCE state, logs, commands, heartbeats). A system-wide, backward-incompatible change for a single PyTfServer problem. Rejected as too invasive.

3. **Supervisor registers with PyTfServer via Ping** — the Supervisor sends its `fullRunningId` in the `Ping` RPC periodically, and PyTfServer tracks liveness, removing TFs for dead Supervisors after a timeout. Requires a ping timer in the Supervisor, a registration/cleanup mechanism in PyTfServer, and proto changes. This is essentially Step I (PyTfServer heartbeats) but over gRPC instead of Kafka. Deferred — scope matches Step I.

4. **PyTfServer listens to Kafka heartbeats** — PyTfServer becomes a Kafka consumer for the heartbeat topic and removes TFs whose Supervisors stop sending heartbeats. Adds Kafka dependency to PyTfServer. Deferred to Step I.

The chosen approach (Steps L.1 + L.2) is the least invasive: two small changes confined to `DasuImpl.cleanUp()` and `TfManager.initialize()`, no proto changes, no system-wide identifier changes, and it handles both clean and unclean Supervisor termination. The longer-term heartbeats approach (Step I) will provide broader monitoring and detection of other failure scenarios.

**Files changed:**

| File | Change |
|---|---|
| `DistributedUnit/src/main/scala/org/eso/ias/dasu/DasuImpl.scala` | `cleanUp()` now iterates `asces` and calls `shutdown()` on each, wrapped in `Try` with logging |
| `PyTfServer/src/main/python/IasPyTfServer/tf_manager.py` | `initialize()` auto-shutdowns orphaned TF on collision, then re-attempts init |
| `PyTfServer/src/test/python/test_tf_manager.py` | `test_double_initialize_fails` renamed to `test_double_initialize_shuts_down_orphan` (succeeds after cleanup) |
| `PyTfServer/src/test/python/test_server_lifecycle.py` | Same test renamed and updated for new behavior |
| `PyTfServer/src/integrationTest/scala/.../TestGrpcPythonExecutorIntegration.scala` | `fail on double initialize` renamed to `succeed on double initialize after cleaning orphaned TF` |

### Step D.5 — Explicit pyTfServerAddr on ComputingElement and GrpcPythonExecutor (DONE)

The `pyTfServerAddr` is no longer hidden inside `Properties`. It is now an explicit constructor parameter at every level:

- **`ComputingElement`**: added `pyTfServerAddr: Option[String] = None` as 7th constructor parameter (defaulted, so 30+ existing test call sites compile unchanged). The `apply` factory accepts it as a 5th parameter. For Scala/Java TFs it is `None`; for Python TFs it carries the address.
- **`ComputingElement.initialize()`**: passes `pyTfServerAddr` to `tfSetting.initialize()`.
- **`TransferFunctionSetting.initialize()`**: accepts `pyTfServerAddr: Option[String] = None` and threads it to `instantiateExecutor()`. The `instantiateExecutor()` now looks for a 6-arg constructor for Python (was 5) and passes `pyTfServerAddr.getOrElse("localhost:50051")` as the 6th argument via reflection.
- **`GrpcPythonExecutor`**: constructor now takes `val serverAddr: String` as an explicit 6th parameter instead of reading it from `props`. The `apply` factory updated to match.
- **`DasuImpl`**: no longer sets `pyTfServerAddr` in properties; passes it directly to `ComputingElement.apply` as the 7th parameter.

**This completes the wiring chain:** `SupervisorDao.getPyTfServerAddr()` → `Supervisor.main` → `DasuImpl.apply` (7-arg) → `ComputingElement.apply` (7-arg) → `ComputingElement.initialize()` → `TransferFunctionSetting.initialize()` → `instantiateExecutor()` → `GrpcPythonExecutor` constructor (explicit 6th param).

### Step D — Step 6: Configuration threading (DONE)

The full chain is now operational with explicit parameters at every layer. See sub-steps D.1 through D.5.

**Files changed (Steps D.1–D.5):**

| File | Change |
|---|---|
| `Cdb/.../JsonSupervisorDao.java` | Added `pyTfServerAddr` to `hashCode()` and `equals()` |
| `CompElement/.../ComputingElement.scala` | Added `pyTfServerAddr: Option[String] = None` constructor param + factory param; threads address to `PythonTF` branch and `tfSetting.initialize()` |
| `CompElement/.../TransferFunctionSetting.scala` | `initialize()` and `instantiateExecutor()` accept `pyTfServerAddr`; 6-arg constructor for Python via reflection |
| `CompElement/.../GrpcPythonExecutor.scala` | `serverAddr` is explicit 6th constructor param (no longer read from props); `apply` factory updated |
| `DistributedUnit/.../DasuImpl.scala` | Passes `pyTfServerAddr` directly to `ComputingElement.apply` (no longer via properties) |
| `Supervisor/.../Supervisor.scala` | Main reads `pyTfServerAddr` from CDB and passes to `DasuImpl.apply`; ping delegates to `GrpcPythonExecutor.checkServerRunning` |
| `Supervisor/build.gradle.kts` | Added `implementation(project(":CompElement"))` (no direct gRPC deps) |
| `CompElement/.../TestGrpcPythonExecutor.scala` | Constructor args updated; 2 unit tests for `checkServerRunning` |
| `PyTfServer/.../TestGrpcPythonExecutorIntegration.scala` | All 5 `GrpcPythonExecutor` instantiations pass explicit `serverAddr` |

### Step E — Re-enable Python TF tests (DONE — tests moved to PyTfServer integration tests)

`TestPyTF` and `TestMinMaxPyTF` were moved from CompElement to `PyTfServer/src/integrationTest/` (Step A.7). They no longer require a pre-running server on `localhost:50051` — each test class manages its own PyTfServer subprocess through `PyTfServerBase`. Both test suites pass (17 + 4 = 21 tests).

### Step G — Move integration tests to `src/integrationTest` (DONE — superseded by Step A.7)

All integration tests are now under `PyTfServer/src/integrationTest/scala/org/eso/ias/pytfserver/test/`, managed by `PyTfServerBase`, and run via `runIntTests.sh` through `iasRun`. See Step A.7 for details.

**Deviation from original plan:** The `PyTfServer/build.gradle.kts` was NOT reverted to `base` plugin only. The integration tests are Scala and depend on CompElement classes (`GrpcPythonExecutor`, `ComputingElement`, types). The `scala` plugin, CompElement/BasicTypes/Tools dependencies, and `integrationTest` Scala source set are required and will remain.

### Step J — MultiplicityThreshold Python TF (DONE)

Added `MultiplicityThreshold` Python TF to `PyTfServer/src/main/python/IasTransferFunction/` as the Python equivalent of the Scala `MultiplicityTF`.

**Behavior:** Takes alarm inputs, activates the output alarm when the number of SET alarms >= threshold from CDB properties. The output priority comes from the `org.eso.ias.tf.alarm.priority` property (defaults to MEDIUM). Properties from SET inputs are propagated and merged (comma-separated) on the output. The `IdsOfAlarmsSet` property lists the IDs of active alarms. Validity constraints are computed based on reliable inputs.

**Files changed:**

| File | Change |
|---|---|
| `PyTfServer/src/main/python/IasTransferFunction/MultiplicityThreshold.py` | **NEW** — Python port of Scala MultiplicityTF |
| `PyTfServer/src/main/python/IasTransferFunction/__init__.py` | Added `MultiplicityThreshold` export |
| `PyTfServer/src/test/python/test_multiplicity_threshold.py` | **NEW** — 31 unit tests covering init validation, threshold activation, property propagation/merge, mode propagation, and validity constraints |

### Step I — PyTfServer sends IAS heartbeats

The `PyTfServer` process should emit IAS heartbeats to the Kafka heartbeat topic, using the same Python heartbeat infrastructure from the `Heartbeat` module that `AlarmGui` and other IAS components use. This ensures the central monitoring system can detect when a PyTfServer process goes down, just as it does for any other IAS component.

### Step F — Full integration tests (DONE)

`SupervisorPyTfTest` validates the full chain end-to-end:
1. Starts a PyTfServer subprocess on the port from the CDB (`pyTfServerAddr`).
2. Instantiates a Supervisor **in-process** (not via ProcessBuilder) with the CDB that has `pyTfServerAddr` configured.
3. The Supervisor runs 3 DASUs: 2 Python TFs (MinMaxThreshold, MultiplicityThreshold) + 1 Scala TF (BoolToAlarm).
4. Pushes inputs via Kafka and verifies all 3 outputs on the BSDB.
5. Verifies alarms SET when inputs exceed threshold, and CLEAR when inputs drop below.
6. **2 tests pass** (produce outputs from all DASUs, clear alarms when inputs go below threshold).

The Supervisor uses the same pattern as `SupervisorWithKafkaTest`: an in-process `new Supervisor(...)` with a factory that passes `pyTfServerAddr` to `DasuImpl.apply` via the 7-arg overload. The `KafkaSubscriber` handles input consumption. The `iasiosConsumer` type filter was removed to capture all IASIOs. The alarm map now keys by `iasio.id` instead of `fullRunningId`. The `waitAlarms` method now accepts a list of expected alarm IDs and clears the map before waiting.

---

### Step K — CdbChecker pyTfServerAddr validation (DONE)

The `CdbChecker` now validates that a Supervisor's `pyTfServerAddr` is consistent with the TFs deployed in its ASCEs:
- If a Supervisor deploys at least one ASCE with a Python TF, `pyTfServerAddr` **must** be defined.
- If a Supervisor has `pyTfServerAddr` defined, at least one of its ASCEs must use a Python TF.
- The `pyTfServerAddr` format (`host:port`) must be valid (delegates to `SupervisorDao.isValidPyTfAddress`).

**Implementation:**
- New `checkSupervisor(supervisorDao, dasusOfSuperv)` method in `CdbChecker.scala` (line 649) navigates Supervisor → DASUs → ASCEs → TFs to check `implLang == PYTHON`.
- The `check()` method (line 224) iterates all supervisors from `mapOfSupervisors`, calls `checkSupervisor`, and propagates errors via `errorsFound = errorsFound || checkSupervisor(...)`.

**Test CDB data** (covers all 4 combinations via the existing `check()` loop over all supervisors):

| Supervisor | pyTfServerAddr | Python TF | Expected |
|---|---|---|---|
| `SupervisorID` | no | no | valid |
| `SupervisoWithPySrvAddrWrong` | yes | no | error |
| `SupervisorWithPyTF_NoAddr` | no | yes | error |
| `SupervisorWithPyTF_WithAddr` | yes | yes | valid |

**Files changed:**

| File | Change |
|---|---|
| `CdbChecker/.../CdbChecker.scala` | Added `checkSupervisor` method; updated `check()` to call it for each supervisor |
| `CdbChecker/.../TF/tfs.json` | Added Python TF entry: `PyTestTF` with `implLang = PYTHON` |
| `CdbChecker/.../IASIO/iasios.json` | Added `PyAsceIn`, `DasuWithPyTF-OutID` IASIOs |
| `CdbChecker/.../ASCE/PyAsceOnDasuWithPyTF-ID1.json` | **NEW** — Python ASCE definition |
| `CdbChecker/.../DASU/DasuWithPyTF.json` | **NEW** — DASU with Python ASCE |
| `CdbChecker/.../SUPERVISOR/SupervisorWithPyTF_NoAddr.json` | **NEW** — Python TFs, no address (should error) |
| `CdbChecker/.../SUPERVISOR/SupervisorWithPyTF_WithAddr.json` | **NEW** — Python TFs, with address (valid) |

**Bugs fixed:**
- `checkSupervisor` return value was discarded in `check()` — now propagated via `errorsFound = errorsFound || ...`
- `checkSupervisor` `else` branch always reported an error when `pyTfServerAddr` was empty — fixed with `if (hasPyTf)` guard

---

## Remaining Work to Complete the Task

The gRPC-based Python TF executor is fully operational at the ASCE level: 27 PyTfServer integration tests confirm that ComputingElements with Python transfer functions initialize, evaluate, and shut down correctly over gRPC, covering every IAS data type and including a 15,000-call stress test. Plus 2 Supervisor-level integration tests for full end-to-end validation: **29 integration tests total**, all passing.

The full wiring chain from the CDB `SupervisorDao` through `DasuImpl` to `GrpcPythonExecutor` is in place with explicit `pyTfServerAddr` parameters at every layer. If a Python TF is configured without an address, `TransferFunctionSetting.instantiateExecutor` throws an `IllegalStateException` and the ASCE is marked broken.

The lifecycle is also correctly closed: when the Supervisor shuts down, `DasuImpl.cleanUp()` now calls `shutdown()` on each ASCE, which sends a gRPC `Shutdown` RPC to PyTfServer to remove the TF from its registry (Step L). This ensures that after a Supervisor restart, ASCEs can re-initialize cleanly on PyTfServer.

The full code path is validated by a Supervisor-level integration test: `SupervisorPyTfTest` uses an **in-process Supervisor** (`new Supervisor(...)`) with a factory that passes `pyTfServerAddr` to `DasuImpl.apply` (7-arg overload). The PyTfServer starts as a subprocess. 3 DASUs run simultaneously: 2 Python TFs (MinMaxThreshold, MultiplicityThreshold) + 1 Scala TF (BoolToAlarm). Alarm map keys by `iasio.id`; `waitAlarms` accepts a list of expected alarm IDs. **2 tests pass** (produce outputs, clear alarms).

**Step C — Shared gRPC channel** is a performance optimization. Currently each `GrpcPythonExecutor` (one per ASCE) creates its own gRPC `ManagedChannel`. A shared channel with reference counting would mean all ASCEs under a Supervisor share a single OS connection to the PyTfServer, reducing resource usage.

**Step F — Full end-to-end integration** is DONE. See above.

| Step | Description | Priority |
|---|---|---|
| **D.1** | `JsonSupervisorDao.equals()/hashCode()` bug fix | DONE |
| **D.2** | Fail-fast Ping in Supervisor constructor | DONE |
| **D.3** | DasuImpl constructor + apply overload with pyTfServerAddr | DONE |
| **D.4** | Supervisor main reads address from CDB and passes to DasuImpl | DONE |
| **D.5** | Explicit pyTfServerAddr on ComputingElement + GrpcPythonExecutor (no more property hiding) | DONE |
| **D.6** | Integration tests pass explicit serverAddr via 7th param | FIXED |
| **D** | Wire `pyTfServerAddr` from SupervisorDao through to GrpcPythonExecutor | DONE |
| **H** | Fail-fast Ping RPC on executor startup | Operational robustness (deferred) |
| **C** | Shared gRPC channel with ref counting | Performance optimization |
| **D.6** | Integration tests pass explicit pyTfServerAddr as 7th ComputingElement param | FIXED |
| **I** | PyTfServer sends IAS heartbeats to Kafka | Monitoring |
| **J** | MultiplicityThreshold Python TF | DONE |
| **K** | CdbChecker validates pyTfServerAddr consistency | DONE |
| **F** | Full end-to-end Supervisor to PyTfServer integration (SupervisorPyTfTest) | DONE (2 tests pass, in-process Supervisor) |
| **L** | Graceful ASCE shutdown on Supervisor stop + orphan cleanup on PyTfServer | DONE |

### ⚠️ Backward Incompatible Change — Property Rename

The property name `org.eso.ias.tf.mutliplicity.threshold` was misspelled (typo: `mutliplicity` → `multiplicity`) across both the Scala `MultiplicityTF` and the Python `MultiplicityThreshold`. It has been corrected to `org.eso.ias.tf.multiplicity.threshold` in all 5 locations. **This release must be marked as backward incompatible.** Any production CDB referencing `org.eso.ias.tf.mutliplicity.threshold` must be updated before upgrading.

## Note

Further development continues in [JepReplacement3.md](./JepReplacement3.md).
