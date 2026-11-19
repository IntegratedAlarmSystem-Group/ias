# JEP → gRPC Phase 2 — Remaining Steps

## Objective

Complete the remaining open steps from JepReplacement2.md.

## Where We Left Off

See [JepReplacement2.md](./JepReplacement2.md) for the full history of Steps 0 through L (all DONE).

### Summary of JepReplacement2.md (DONE)

The gRPC-based Python TF executor is fully operational end-to-end:

- **29 integration tests** pass (6 executor-level + 17 full ASCE lifecycle + 4 MinMaxThreshold + 2 Supervisor-level)
- Full wiring chain: CDB `SupervisorDao` → Supervisor → DasuImpl → ComputingElement → GrpcPythonExecutor → PyTfServer
- **Step L**: Graceful ASCE shutdown on Supervisor Ctrl-C (`DasuImpl.cleanUp()` calls `asce.shutdown()`), plus orphaned TF auto-cleanup on PyTfServer for unclean Supervisor crashes
- **Step J**: `MultiplicityThreshold` Python TF (31 unit tests)
- **Step K**: CdbChecker validates `pyTfServerAddr` consistency

### Server Architecture

```
iasPyTfServer process (1 per Supervisor)
└── gRPC Server (insecure, localhost:port)
    └── PyTfService / tf_service.PyTfService (1 instance — gRPC servicer)
        └── TfManager (1 instance — singleton)
            └── tf_instances: Dict[asceRunningId, TransferFunction]

GrpcPythonExecutor (Scala, one per ASCE with Python TF)
└── ManagedChannel → PyTfServer (Initialize / Eval / Shutdown RPCs)
```

### Remaining Issues from JepReplacement2.md

| Step | Description | Priority |
|---|---|---|
| **D.2 cleanup** | Ping failure in `Supervisor.scala` uses exception-based graceful shutdown (was `sys.exit(1)`) | DONE |
| **H** | Fail-fast Ping RPC on executor startup (deferred) | Operational robustness |
| **C** | Shared gRPC channel with ref counting | Performance optimization |
| **I** | PyTfServer sends IAS heartbeats via Kafka using the Heartbeat module Python classes | Monitoring |
| **L** | Graceful ASCE shutdown on Supervisor stop + orphan cleanup on PyTfServer | DONE |

### Bug Fix — `serve()` signature SyntaxError (DONE)

`iasPyTfServer.py` had `def serve(port: int = 50051, server_id: str)` — a parameter with a default preceding one without. Python rejected the file at parse time with `SyntaxError: parameter without a default follows parameter with a default`. This was introduced when the `-i`/`--id` argument was added. It prevented the `TestPyTfServerNetwork` tests from passing (the server subprocess exited immediately). Fixed by swapping the parameter order to `def serve(server_id: str, port: int = 50051)`.

| File | Change |
|---|---|
| `PyTfServer/src/main/python/iasPyTfServer.py` | Swapped `serve()` parameters to fix SyntaxError |

### `iasPyTfServer.py` — `-i`/`--id` CLI Argument (DONE)

Added optional `-i`/`--id` argument to identify the server instance in logs. If omitted, the default is `PyTfServer-<hostname>-<port>`. Uses `-` as separator (not `:`) to avoid conflicts with the `fullRunningId` `id:type` parsing syntax.

| File | Change |
|---|---|
| `PyTfServer/src/main/python/iasPyTfServer.py` | Added `--id` argument, default ID via hostname+port, updated startup log |

---

### ⚠️ Backward Incompatible Change — Property Rename (carried over)

The property name `org.eso.ias.tf.mutliplicity.threshold` was misspelled (typo: `mutliplicity` → `multiplicity`) across both the Scala `MultiplicityTF` and the Python `MultiplicityThreshold`. It has been corrected to `org.eso.ias.tf.multiplicity.threshold` in all 5 locations:

1. `CompElement/.../MultiplicityTF.scala` — Scala TF
2. `PyTfServer/.../MultiplicityThreshold.py` — Python TF
3. `PyTfServer/.../test_multiplicity_threshold.py` — Python unit tests
4. `DistributedUnit/.../ASCE-AlarmsThreshold.json` — test CDB
5. `DistributedUnit/.../ACKASCE3.json` — test CDB

**This release must be marked as backward incompatible.** Any production CDB that references `org.eso.ias.tf.mutliplicity.threshold` will need to be updated to `org.eso.ias.tf.multiplicity.threshold` before upgrading.

---

## Complete Summary of All Steps

### Completed (DONE)

| Step | Description | Where documented |
|---|---|---|
| **0** | Ping health check in `tf_service.py` | JepReplacement2.md |
| **A.1** | Proto alignment: Initialize/Eval/Shutdown RPCs | JepReplacement2.md |
| **A.2** | GrpcPythonExecutor: real gRPC RPCs | JepReplacement2.md |
| **A.3** | iasPyTfServer.py: real PyTfService handler | JepReplacement2.md |
| **A.4** | Python Server Lifecycle Tests (6 tests) | JepReplacement2.md |
| **A.5** | Scala Integration Tests (6 executor tests) | JepReplacement2.md |
| **A.6** | Refactor shared test paths (`_test_paths.py`) | JepReplacement2.md |
| **A.7** | Move integration tests to `src/integrationTest` | JepReplacement2.md |
| **B / A.1** | Fix flat imports in generated gRPC stubs | JepReplacement2.md |
| **D.1** | `JsonSupervisorDao.equals()/hashCode()` bug fix | JepReplacement2.md |
| **D.2** | Fail-fast Ping in Supervisor constructor | JepReplacement2.md |
| **D.3** | DasuImpl constructor + apply overload with pyTfServerAddr | JepReplacement2.md |
| **D.4** | Supervisor main reads address from CDB | JepReplacement2.md |
| **D.5** | Explicit pyTfServerAddr on ComputingElement + GrpcPythonExecutor | JepReplacement2.md |
| **D** | Wire pyTfServerAddr from SupervisorDao to GrpcPythonExecutor | JepReplacement2.md |
| **E** | Re-enable Python TF tests (moved to PyTfServer) | JepReplacement2.md |
| **F** | Full end-to-end SupervisorPyTfTest (2 tests) | JepReplacement2.md |
| **G** | Move integration tests to src/integrationTest (superseded by A.7) | JepReplacement2.md |
| **J** | MultiplicityThreshold Python TF (31 unit tests) | JepReplacement2.md |
| **K** | CdbChecker validates pyTfServerAddr consistency | JepReplacement2.md |
| **L.1** | DasuImpl.cleanUp() calls ASCE shutdown (clean stop) | JepReplacement2.md |
| **L.2** | TfManager.initialize() auto-cleans orphaned TFs (unclean stop) | JepReplacement2.md |
| **`serve()` fix** | SyntaxError: swapped `serve()` parameters | JepReplacement3.md |
| **`-i`/`--id`** | CLI argument for PyTfServer identification | JepReplacement3.md |
| **D.2 cleanup** | Exception-based graceful shutdown on ping failure (replaces `sys.exit(1)`) | JepReplacement3.md |
| **M** | gRPC call deadline + `shutdown()` error handling + detailed log messages | JepReplacement3.md |

### Step M — Details (DONE)

**Problem:** Without a deadline, gRPC calls to PyTfServer could block for up to ~2 minutes (internal retry backoff) if the server is unreachable. Since the DASU runs TFs sequentially on a single thread, a blocked `eval()` would freeze the entire DASU.

**Solution:** All four gRPC calls (`eval`, `initialize`, `shutdown`, `checkServerRunning` ping) now use `stub.withDeadline(Deadline.after(...))` with a configurable timeout (default 5s). The timeout is read from the Java system property `org.eso.ias.rpc.timeout` (in seconds), accessible via `GrpcPythonExecutor.rpcTimeoutSeconds`.

**`shutdown()` resilience:** The gRPC `shutdown()` RPC call is wrapped in try/catch so that a dead PyTfServer during Supervisor termination does not prevent channel cleanup. The `channel.shutdown()` + `awaitTermination(5s)` always runs regardless, with an error log if the channel fails to terminate in time.

**`eval()` and `initialize()` logging:** Both methods catch `DEADLINE_EXCEEDED` specifically and log a clear `logger.error` message stating: which ASCE failed, the PyTfServer address, the timeout value, and the consequence (ASCE transitions to TFBroken). Other exceptions are logged with full stack trace. The exception is re-thrown so the existing `Try` wrapper in `PythonTransfer.transfer()` handles the `TFBroken` state transition.

**Files changed:**

| File | Change |
|---|---|
| `CompElement/.../GrpcPythonExecutor.scala` | Added `RpcTimeoutProperty` + `rpcTimeoutSeconds` to companion object; `withDeadline` on all 4 RPC calls; `shutdown()` wrapped in try/catch; `eval()` and `initialize()` catch `DEADLINE_EXCEEDED` with detailed `logger.error`; `awaitTermination` failure logged with `logger.error` |

### ⚠️ Documentation Required (before release)

The following must be clearly stated in the **release notes** and in the **user documentation**:

1. **`org.eso.ias.rpc.timeout`** — Java system property (in seconds, default 5) controlling the deadline for all gRPC calls between the Supervisor and PyTfServer (Initialize, Eval, Shutdown, Ping). If the server does not reply within this time, the call fails with `DEADLINE_EXCEEDED` and the ASCE transitions to `TFBroken`. Operators can override it via `-Dorg.eso.ias.rpc.timeout=<seconds>` when launching the Supervisor.
2. **Shutting down or restarting PyTfServer requires restarting the Supervisor.** If PyTfServer is killed while the Supervisor is running, the next gRPC call from any Python ASCE will fail. The affected ASCE transitions to `TFBroken` and **cannot recover** — the ASCE is permanently broken for the lifetime of the Supervisor process. Only restarting the Supervisor (after PyTfServer is back up) will re-initialize the ASCEs. The DASU continues running, but downstream ASCEs that depend on the broken ASCE's output will operate on stale data.
3. **`shutdown()` is resilient.** If PyTfServer is already dead when the Supervisor terminates (e.g., Ctrl-C), the shutdown RPC failure is logged at error level and the gRPC channel is still cleaned up locally. It does not affect cleanup of other ASCEs.

### Remaining

| Step | Description | Priority |
|---|---|---|
| **C** | Shared gRPC channel with ref counting | Performance optimization |
| **I** | PyTfServer sends IAS heartbeats via Kafka using the Heartbeat module Python classes | Monitoring |

**Total: 28 steps completed, 2 remaining.**
