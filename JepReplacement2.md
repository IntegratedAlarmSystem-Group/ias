# JEP → gRPC Phase 2 — Remaining Steps

## Objective

Complete the remaining steps to make the gRPC-based Python TF executor fully operational end-to-end.

## Actual Code State (as of Nov 10 2026)

### What's committed (git HEAD)

The committed code has the **Ping-only** PoC plumbing from Step 1. Files reflect this state:

| File | State |
|---|---|
| `pytf_service.proto` | `Ping` RPC only; `IasIO`/`IasioInfoProto` messages present |
| `GrpcPythonExecutor.scala` | Calls `Ping` from `initialize`, `eval`, `shutdown`; `eval` returns `actualOutput` unchanged |
| `iasPyTfServer.py` (renamed from `server.py`) | Inline `Ping` handler only |
| `ProtoUtil.scala` (CompElement) | Full `iasIOFrom`/`iasIOTo`/`iasioInfoFrom`/`iasioInfoTo` — unused by Ping |

### What's developed but uncommitted

The following untracked files implement Step 2/3/5 (Initialize/Eval/Shutdown) but are **not yet reflected in the committed proto or GrpcPythonExecutor**:

| File | State |
|---|---|
| `PyTfServer/src/main/python/IasPyTfServer/tf_manager.py` | Full TfManager with Initialize/Eval/Shutdown, dynamic TF import |
| `PyTfServer/src/main/python/IasPyTfServer/tf_service.py` | Full PyTfService servicer with proto ↔ Python conversion; added `Ping` health check |
| `PyTfServer/src/test/python/test_tf_manager.py` | 10 TfManager unit tests |
| `PyTfServer/src/test/python/test_service.py` | 18 proto conversion tests |
| `PyTfServer/src/test/python/test_tf_module.py` | Mock TF implementations |
| `PyTfServer/src/test/python/test_server.py` | 2 in-process server unit tests (Ping health check only) |

**The proto and GrpcPythonExecutor still need to be updated to match these.**

### Proto fix

Renamed `server.py` → `iasPyTfServer.py`. The `grpc_tools.protoc`-generated `_pb2_grpc.py` files use flat imports (`import pytf_service_pb2`) that don't work from the installed package. A `fixPythonGrpcImports` build task and `fix_grpc_imports.py` script are needed to patch this after generation (both CompElement and BasicTypes).

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

Corresponding unit test `test_server.py` starts the server in-process with a random localhost port and uses a Python gRPC client to verify two Ping calls succeed. The test **cannot run yet** — `tf_service.py` imports `InitializeResponse`/`EvalResponse`/`ShutdownResponse` which don't exist in the committed Ping-only proto stubs. It will pass once Step A updates the proto.

**Files changed:**

| File | Change |
|---|---|
| `PyTfServer/src/main/python/IasPyTfServer/tf_service.py` | Added `PingResponse` import; added `Ping(self, request, context)` method to `PyTfService` |
| `PyTfServer/src/test/python/test_server.py` | **NEW** — 2 unit tests: `test_ping_returns_success`, `test_ping_multiple_times` |

### Step A — Align proto + GrpcPythonExecutor with developed code

The developed `tf_service.py` / `tf_manager.py` implement Initialize/Eval/Shutdown. The committed proto and `GrpcPythonExecutor` are still at Ping.

1. **`pytf_service.proto`** — Keep existing `Ping` RPC (health check) and add `Initialize`, `Eval`, `Shutdown` RPCs with their request/response messages (as described in JepReplacement.md Step 2).
2. **`GrpcPythonExecutor.scala`** — Replace Ping calls with real Initialize/Eval/Shutdown calls using `ProtoUtil.iasIOFrom`/`iasIOTo`. `eval` should serialize inputs, send `EvalRequest`, deserialize response.
3. **`iasPyTfServer.py`** — Replace inline Ping handler with the `PyTfService` from `tf_service.py`.
4. **`build.gradle.kts` (both CompElement and BasicTypes)** — Add `fixPythonGrpcImports` task using `fix_grpc_imports.py` to patch flat imports in generated `_pb2_grpc.py` files.

### Step B — Build fix: flat import in generated gRPC stubs

`grpc_tools.protoc` generates `import pytf_service_pb2` instead of `from IasProtobufTfserver import pytf_service_pb2`. This breaks when the stubs are installed as a package.

- **New file**: `CompElement/src/main/python/fix_grpc_imports.py` — patches `_pb2_grpc.py` imports to use package-relative form.
- **CompElement/build.gradle.kts**: New `fixPythonGrpcImports` task between `generatePythonProto` and `CopyPyProtoMods`.
- **BasicTypes/build.gradle.kts**: Same fix task (referencing the same script file from CompElement), for future proofing.

### Step C — Step 4: GrpcChannelManager

Each `GrpcPythonExecutor` currently creates its own `ManagedChannel`. Plan: shared channel with ref counting so all ASCEs in a Supervisor share one OS connection.

### Step D — Step 6: Configuration threading

Wire `pyTfServerAddr` from `SupervisorDao` (already defined, validated, unused) down to each `GrpcPythonExecutor` via Supervisor → DasuImpl → ASCE properties.

### Step E — Step 7: Re-enable Python TF tests

Remove `TestPyTF` and `TestMinMaxPyTF` excludes from `CompElement/build.gradle.kts`. A running PyTfServer on `localhost:50051` is required.

### Step F — Step 8: Integration tests

Full end-to-end: Supervisor starts, connects to external PyTfServer, evaluates real Python TFs.
