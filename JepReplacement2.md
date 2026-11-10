# JEP → gRPC Phase 2 — Remaining Steps

## Objective

Complete the remaining steps to make the gRPC-based Python TF executor fully operational end-to-end.

## Actual Code State (as of Nov 10 2026)

### What's committed (git HEAD)

The committed code includes the full proto, Python server, tests, and build fix. Files reflect this state:

| File | State |
|---|---|
| `pytf_service.proto` | `Ping`, `Initialize`, `Eval`, `Shutdown` RPCs; all request/response messages; `IasIO`/`IasioInfoProto` present |
| `GrpcPythonExecutor.scala` | Calls `Ping` from `initialize`, `eval`, `shutdown`; `eval` returns `actualOutput` unchanged — **needs update (Step A.2)** |
| `iasPyTfServer.py` (renamed from `server.py`) | Inline `Ping` handler only — **needs update (Step A.3)** |
| `ProtoUtil.scala` (CompElement) | Full `iasIOFrom`/`iasIOTo`/`iasioInfoFrom`/`iasioInfoTo` — unused by Ping |
| `PyTfServer/src/main/python/IasPyTfServer/tf_manager.py` | Full TfManager with Initialize/Eval/Shutdown, dynamic TF import |
| `PyTfServer/src/main/python/IasPyTfServer/tf_service.py` | Full PyTfService servicer with proto ↔ Python conversion; `Ping` health check |
| `PyTfServer/src/test/python/test_tf_manager.py` | 10 TfManager unit tests |
| `PyTfServer/src/test/python/test_service.py` | 18 proto conversion tests |
| `PyTfServer/src/test/python/test_tf_module.py` | Mock TF implementations |
| `PyTfServer/src/test/python/test_server.py` | 2 in-process unit tests (Ping) + 2 network-level tests (Ping over TCP) |
| `CompElement/src/main/python/fix_grpc_imports` | Patches flat protobuf imports to package-relative form |

**All Python unit tests pass (32 tests). The proto is aligned. GrpcPythonExecutor.scala and iasPyTfServer.py still need updating.**

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
2. **`GrpcPythonExecutor.scala`** — Replace Ping calls with real Initialize/Eval/Shutdown calls using `ProtoUtil.iasIOFrom`/`iasIOTo`. `eval` should serialize inputs, send `EvalRequest`, deserialize response.
3. **`iasPyTfServer.py`** — Replace inline Ping handler with the `PyTfService` from `tf_service.py`.
4. **`build.gradle.kts` (both CompElement and BasicTypes) (DONE)** — `fixPythonGrpcImports` tasks wired using `fix_grpc_imports` script. `PyTfServer/pytest` depends on `:CompElement:CopyPyProtoMods` to ensure fresh stubs.

### Step B — Build fix: flat import in generated gRPC stubs (DONE) – merged into Step A.1

`grpc_tools.protoc` generates `import pytf_service_pb2` instead of `from IasProtobufTfserver import pytf_service_pb2`. This breaks when the stubs are installed as a package.

- **New file**: `CompElement/src/main/python/fix_grpc_imports` — patches flat imports to package-relative form. Accepts `-g` flag to target either `*_pb2.py` (cross-module) or `*_pb2_grpc.py` (own-module). Must be run **twice**: once for each pattern with the appropriate target package.
- **CompElement/build.gradle.kts**: `fixPythonGrpcImportsPb2` (cross-module, target `IasProtobufTypes`) → `fixPythonGrpcImportsGrpc` (own-module, target `IasProtobufTfserver`) → `CopyPyProtoMods` (depends on the second fix). Chain: `generatePythonProto` → fix → copy.
- **BasicTypes/build.gradle.kts**: `fixPythonGrpcImports` (own-module, target `IasProtobufTypes`) → `CopyPyProtoMods`. References the same script from CompElement.

**Verified**: After `CopyPyProtoMods`, the copied stubs in `build/lib/.../site-packages/IasProtobufTfserver/` have correct imports. Network tests pass with build-generated stubs.

### Step C — Step 4: GrpcChannelManager

Each `GrpcPythonExecutor` currently creates its own `ManagedChannel`. Plan: shared channel with ref counting so all ASCEs in a Supervisor share one OS connection.

### Step D — Step 6: Configuration threading

Wire `pyTfServerAddr` from `SupervisorDao` (already defined, validated, unused) down to each `GrpcPythonExecutor` via Supervisor → DasuImpl → ASCE properties.

### Step E — Step 7: Re-enable Python TF tests

Remove `TestPyTF` and `TestMinMaxPyTF` excludes from `CompElement/build.gradle.kts`. A running PyTfServer on `localhost:50051` is required.

### Step F — Step 8: Integration tests

Full end-to-end: Supervisor starts, connects to external PyTfServer, evaluates real Python TFs.
