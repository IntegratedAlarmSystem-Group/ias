# JEP → gRPC Phase 2 — Remaining Steps

## Objective

Complete the remaining steps to make the gRPC-based Python TF executor fully operational end-to-end.

## Actual Code State (as of Nov 16 2026)

### What's committed (git HEAD)

The committed code includes the full proto, Python server, executor, tests, and build fix:

| File | State |
|---|---|
| `pytf_service.proto` | `Ping`, `Initialize`, `Eval`, `Shutdown` RPCs; all request/response messages; `IasIO`/`IasioInfoProto` present |
| `GrpcPythonExecutor.scala` | `initialize()` and `shutdown()` call real gRPC RPCs using `ProtoUtil`; `eval()` still uses Ping stub — **needs update** |
| `iasPyTfServer.py` | Uses full `tf_service.PyTfService` (Initialize/Eval/Shutdown) |
| `ProtoUtil.scala` (CompElement) | Full `iasIOFrom`/`iasIOTo`/`iasioInfoFrom`/`iasioInfoTo` — used by Initialize |
| `PyTfServer/src/main/python/IasPyTfServer/tf_manager.py` | Full TfManager with Initialize/Eval/Shutdown, dynamic TF import |
| `PyTfServer/src/main/python/IasPyTfServer/tf_service.py` | Full PyTfService servicer with proto ↔ Python conversion; `HasField('templateInstance')` fix |
| `PyTfServer/src/test/python/test_tf_manager.py` | 10 TfManager unit tests |
| `PyTfServer/src/test/python/test_service.py` | 18 proto conversion tests |
| `PyTfServer/src/test/python/test_tf_module.py` | Mock TF implementations |
| `PyTfServer/src/test/python/test_server.py` | 2 Ping in-process + 2 Ping network tests + 6 lifecycle tests (6 in-process Init/Shutdwn) |
| `PyTfServer/src/test/scala/.../TestGrpcPythonExecutorIntegration.scala` | 3 Scala tests (needs moving to integration test, see Step G) |
| `CompElement/src/main/python/fix_grpc_imports` | Patches flat protobuf imports to package-relative form |

**All tests pass: 38 PyTfServer pytest + CompElement Scala/Java tests.**

### Proto fix (DONE)

`grpc_tools.protoc`-generated stubs use flat imports (`import xxx_pb2`) that don't work from the installed package. A `fixPythonGrpcImports` build task and `fix_grpc_imports` script patch this after generation (both CompElement and BasicTypes).

The script must be run **twice per package** because two different file types import from different packages:

```
fix_grpc_imports <dir> <dep_package>  -g '*_pb2.py'       # cross-module (e.g. types_pb2 → IasProtobufTypes)
fix_grpc_imports <dir> <own_package>  -g '*_pb2_grpc.py'  # own-module  (e.g. pytf_service_pb2 → IasProtobufTfserver)
```

Running both is necessary — `_pb2.py` may import protos from a dependency package, while `_pb2_grpc.py` imports the sibling `_pb2.py` from the same package.

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
2. **`GrpcPythonExecutor.scala`** — `initialize()` and `shutdown()` replaced with real RPCs using `ProtoUtil` (DONE). `eval()` still uses Ping stub — needs update.
3. **`iasPyTfServer.py`** — Replaced inline Ping handler with `PyTfService` from `tf_service.py` (DONE).
4. **`build.gradle.kts` (both CompElement and BasicTypes) (DONE)** — `fixPythonGrpcImports` tasks wired using `fix_grpc_imports` script. `PyTfServer/pytest` depends on `:CompElement:CopyPyProtoMods` to ensure fresh stubs.

### Step B — Build fix: flat import in generated gRPC stubs (DONE) – merged into Step A.1

`grpc_tools.protoc` generates `import pytf_service_pb2` instead of `from IasProtobufTfserver import pytf_service_pb2`. This breaks when the stubs are installed as a package.

- **New file**: `CompElement/src/main/python/fix_grpc_imports` — patches flat imports to package-relative form. Accepts `-g` flag to target either `*_pb2.py` (cross-module) or `*_pb2_grpc.py` (own-module). Must be run **twice**: once for each pattern with the appropriate target package.
- **CompElement/build.gradle.kts**: `fixPythonGrpcImportsPb2` (cross-module, target `IasProtobufTypes`) → `fixPythonGrpcImportsGrpc` (own-module, target `IasProtobufTfserver`) → `CopyPyProtoMods` (depends on the second fix). Chain: `generatePythonProto` → fix → copy.
- **BasicTypes/build.gradle.kts**: `fixPythonGrpcImports` (own-module, target `IasProtobufTypes`) → `CopyPyProtoMods`. References the same script from CompElement.

**Verified**: After `CopyPyProtoMods`, the copied stubs in `build/lib/.../site-packages/IasProtobufTfserver/` have correct imports. Network tests pass with build-generated stubs.

### Step A.4 — Python Server Lifecycle Tests (DONE)

Added `TestPyTfServerLifecycle` class in `test_server.py` with 6 tests covering the Initialize/Eval/Shutdown lifecycle via gRPC:

| Test | Coverage |
|---|---|
| `test_initialize_and_shutdown_lifecycle` | Full Init → Shutdown roundtrip via gRPC |
| `test_initialize_verifies_args` | Verifies asceId, asceRunningId, validityTimeFrame, props reach the TF |
| `test_initialize_with_template_instance` | Verifies optional `templateInstance` string→int conversion reaches the TF |
| `test_double_initialize_fails` | Second Initialize for same asceRunningId returns error |
| `test_shutdown_nonexistent_fails` | Shutdown for non-existent asceRunningId returns error |
| `test_shutdown_removes_tf_instance` | Shutdown removes TF from TfManager's `tf_instances` dict |

Also fixed `tf_service.py` bug: `request.hasTemplateInstance` → `request.HasField('templateInstance')` (protobuf 4.x API).

### Step A.5 — Scala Integration Tests (DONE)

Added `TestGrpcPythonExecutorIntegration.scala` with 3 integration tests that start a real `iasPyTfServer.py` subprocess and verify the Scala `GrpcPythonExecutor` communicates correctly:

| Test | Coverage |
|---|---|
| `initialize and shutdown via gRPC` | GrpcPythonExecutor → real PyTfServer roundtrip |
| `fail on double initialize` | Second initialize through executor throws exception |
| `receive correct props and asceId in Python TF` | Uses raw gRPC stub to verify props flow through Initialize/Shutdown |

Tests hardcode `python3.12` and construct `PYTHONPATH` from the build tree. Server stdout/stderr captured for diagnostics.

**Files changed (Steps A.2–A.5):**

| File | Change |
|---|---|
| `CompElement/src/main/scala/.../GrpcPythonExecutor.scala` | `initialize()` calls real `Initialize` RPC; `shutdown()` calls real `Shutdown` RPC |
| `PyTfServer/src/main/python/iasPyTfServer.py` | Replaced inline Ping handler with `from IasPyTfServer.tf_service import PyTfService` |
| `PyTfServer/src/main/python/IasPyTfServer/tf_service.py` | Fixed `hasTemplateInstance` → `HasField('templateInstance')` for protobuf 4.x |
| `PyTfServer/src/test/python/test_server.py` | Added `TestPyTfServerLifecycle` class (6 tests) |
| `PyTfServer/src/test/scala/.../TestGrpcPythonExecutorIntegration.scala` | 3 Scala tests (currently under PyTfServer test, to move to integration — see Step G) |

### Verification

- `./gradlew :CompElement:test` — all Scala/Java tests pass
- `./gradlew :PyTfServer:pytest` — 38 passed (32 existing + 6 new lifecycle tests)

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

### Step E — Step 7: Re-enable Python TF tests

Remove `TestPyTF` and `TestMinMaxPyTF` excludes from `CompElement/build.gradle.kts`. A running PyTfServer on `localhost:50051` is required.

### Step G — Move TestGrpcPythonExecutorIntegration to integration tests

The `TestGrpcPythonExecutorIntegration` test currently lives as a unit test under `PyTfServer/src/test/scala`.  Unit tests should only exercise sources of their own module, but this test exercises both `GrpcPythonExecutor` (CompElement) and `iasPyTfServer` (PyTfServer) over gRPC — it is an integration test between two modules.

**Plan:**

1. Move `TestGrpcPythonExecutorIntegration.scala` to `PyTfServer/src/integrationTest/` — following the IAS convention used by all other integration tests.
2. Create `PyTfServer/src/integrationTest/runIntTests.sh` — starts a PyTfServer from `$IAS_ROOT/bin/iasPyTfServer`, runs the test via the Gradle test harness against the installed artifacts, then tears down the server.
3. The test will run via `./gradlew :PyTfServer:integrationTest --continue` after a prior `./gradlew install` (same pattern as other IAS integration tests).
4. **Cleanup `PyTfServer/build.gradle.kts`** — revert to the original `base` plugin only. Remove the `scala` plugin, all `implementation`/`testImplementation` dependencies (CompElement, BasicTypes, Tools, gRPC libs), and the `tasks.test` / `sourceSets` blocks that were added only to support the misplaced test. The file should return to its minimal form (3 plugins lines + pytest dependency).

**Result:** PyTfServer is back to a pure Python module (`base` plugin only). The cross-module integration test lives where it belongs and runs via the standard `integrationTest` task against installed code in `$IAS_ROOT`.

### Step F — Step 8: Full integration tests (later)

Full end-to-end: Supervisor starts, connects to external PyTfServer, evaluates real Python TFs.
