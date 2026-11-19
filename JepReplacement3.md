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
| **I.1** | PyTfServer CLI: fullRunningId, HB infrastructure, CDB integration | JepReplacement3.md |
| **I** | PyTfServer sends IAS heartbeats via Kafka (integration test) | JepReplacement3.md |

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

### Step C — CANCELLED

**Step C** (Shared gRPC channel with ref counting) is cancelled. The current per-ASCE channel design provides isolation: a failure in one Python TF cannot affect other TFs on the same Supervisor. A shared channel would introduce a shared failure point where a channel-level error (server crash, network issue) breaks all Python TFs. The small resource saving (fewer TCP connections to localhost) does not justify the added risk and complexity. Python TFs are primarily for testing and prototyping, and a typical Supervisor won't have many Python ASCEs. If this becomes a real concern in production, it can be revisited then.

### Step I.1 — PyTfServer CLI: fullRunningId, HB infrastructure, CDB integration (DONE)

Infrastructure plumbing for Step I (PyTfServer heartbeats). The following was implemented across three modules:

**`iasPyTfServer.py`** — New CLI arguments and heartbeat lifecycle:

| Option | Type | Default | Description |
|---|---|---|---|
| `-c` / `--jcdb` | str | `None` | Parent folder of the IAS CDB (resolves Kafka brokers + HB frequency) |
| `-b` / `--bsdburl` | str | `None` | Kafka BSDB URL directly. Overrides `--jcdb` |
| `-f` / `--hbfrequency` | int | `None` | HB frequency in seconds (0 disables). Defaults to CDB value, then 5s |

A `parse()` function follows the AlarmGui pattern, returning a dictionary of CLI parameters. The `serve()` function is refactored to manage the global HB engine: transitions the state to `RUNNING` before `server.start()`, and closes the engine in the `finally` block on shutdown. SIGINT is handled by Python's default `KeyboardInterrupt` mechanism — no custom signal handler needed.

The `hb_engine` is stored as a single-element list (`[None]`) at module scope to work around Python's restriction on reassigning type-annotated global variables (PEP 526 `global` error).

**`HeartbeatHelper.py`** (new file in Heartbeat module):

- `get_hb_frequency(frequency_from_cmdline, jCdb)` — resolves HB frequency with fallback chain: CLI → CDB via `--jcdb` → CDB via `IAS_CDB` env var → default (5s)
- `get_hb_frequency_from_cdb(cdb_parent_folder)` — reads `hbFrequency` from the CDB `ias.json`
- `HB_FREQUENCY_DEFAULT = 5`

**`test_heartbeat_helper.py`** (new test, 10 tests):

- Tests all code paths: CLI override, CDB read, env var, fallback to default, invalid CDB path, and direct `get_hb_frequency_from_cdb` calls. Uses a `src/test/CDB/ias.json` fixture with `hbFrequency: 7`.

**BSDB resolution** delegates to `IasKafkaHelper.get_bsdb_url()` (existing), with the same fallback chain as AlarmGui: `--bsdburl` → `--jcdb` → `IAS_CDB` env → `DEFAULT_BOOTSTRAP_BROKERS` (`localhost:9092`).

| File | Change |
|---|---|
| `PyTfServer/src/main/python/iasPyTfServer.py` | Added `parse()`, HB engine lifecycle in `serve()`, CLI args `-c`, `-b`, `-f` |
| `Heartbeat/src/main/python/IasHeartbeat/HeartbeatHelper.py` | New module: `get_hb_frequency()`, `get_hb_frequency_from_cdb()`, `HB_FREQUENCY_DEFAULT` |
| `Heartbeat/src/test/CDB/ias.json` | Test CDB fixture |
| `Heartbeat/src/test/python/test_heartbeat_helper.py` | 10 unit tests for HeartbeatHelper |

### Step I — PyTfServer sends IAS heartbeats via Kafka (DONE)

Building on I.1, the HB engine lifecycle is fully wired into `serve()`:

- **STATE transitions**: `STARTING_UP` (on `hb_engine.start()`) → `RUNNING` (before `server.start()`) → `EXITING` (on `KeyboardInterrupt`) → close
- **Shutdown**: Python's default `KeyboardInterrupt` on SIGINT triggers EXITING state, then `finally` closes HB engine and calls `server.stop(0)`
- **Global HB engine** stored as single-element list (`[None]`) to work around Python's PEP 526 restriction on `global` with type-annotated variables
- **Integration test** (`test_pytfserver_heartbeat.py`) spawns PyTfServer as a subprocess with HB enabled, uses `iasPyTfServer` from `$IAS_ROOT/bin`, and collects heartbeats from the Kafka topic via `HbKafkaConsumer`. A single test (`test_heartbeats_received`) waits for up to 5 heartbeats to confirm the engine is alive.

| File | Change |
|---|---|
| `PyTfServer/src/integrationTest/python/test_pytfserver_heartbeat.py` | Integration test: verifies HBs are published to Kafka |
| `PyTfServer/src/integrationTest/runIntTests.sh` | Added Python pytest run |

### Remaining

**All steps complete.**

| Step | Description | Status |
|---|---|---|
| **I** | PyTfServer sends IAS heartbeats via Kafka using the Heartbeat module Python classes | DONE |

**Total: 31 steps completed (30 done + 1 cancelled), 0 remaining.**

---

## TODO — Ticket Needed: HbEngine.close() unconditionally closes the producer

`HbEngine.close()` always calls `self._producer.close()` (line 76 of `HbEngine.py`), regardless of whether the engine created the producer or received it as a constructor argument. This means the engine takes ownership of an externally-provided producer and closes it, which can cause issues if the producer is shared. The `__init__` should track whether it owns the producer (e.g. via an `own_producer` flag) and only close it in `close()` when it does. Open a GitHub issue if one does not already exist.

---

## Appendix — PyTfServer Mid-Flight Failure: Investigation & Mitigation Options

When PyTfServer is killed while the Supervisor is running, the next gRPC call from a Python ASCE fails and the ASCE transitions to `TFBroken`. The ASCE cannot recover without restarting the Supervisor. This section documents the investigation of possible mitigations.

### Current behavior (after Step M)

With the 5-second gRPC deadline in place, a dead PyTfServer no longer blocks the DASU thread for up to 2 minutes. After the deadline, the ASCE transitions to `TFBroken` and the DASU continues running. Other ASCEs are unaffected, but downstream ASCEs depending on the broken ASCE's output operate on stale data. The operator must restart the Supervisor after PyTfServer is back up.

### Option 1 — Return last output as UNRELIABLE on eval timeout

`GrpcPythonExecutor.eval()` receives `actualOutput` (the last known output). On `DEADLINE_EXCEEDED`, instead of throwing, it could return `actualOutput` with validity forced to `UNRELIABLE`, allowing the DASU to keep producing degraded output rather than freezing.

**Problem:** after the timeout, the TF instance no longer exists on PyTfServer (if the server was killed/restarted). The next `eval()` call would arrive at a fresh server with an empty `tf_instances` dict, and the server would return an error ("ASCE not initialized"). The ASCE would then fail and transition to `TFBroken` anyway. This option alone is insufficient.

### Option 2 — Auto re-initialize on eval failure

After a `DEADLINE_EXCEEDED` or "not initialized" error in `eval()`, the executor could automatically call `initialize()` again before retrying `eval()`. Step L.2 already handles orphan cleanup on the server side, so re-initialization after a fresh PyTfServer start would succeed.

**Problems:**
- `eval()` would now have a side effect (calling initialize), making the code harder to reason about.
- There is a race condition if multiple inputs arrive between the failed eval and the re-init.
- The executor would need stateful failure tracking (e.g., a counter of consecutive failures) to avoid infinite re-init loops if PyTfServer remains unreachable.
- `initialize()` itself is a blocking gRPC call — if the server is still dead, the DASU thread blocks again (though the deadline limits this to 5s).

### Option 3 — Hybrid: stale output with bounded retries + auto re-init

Combine options 1 and 2:
1. On `DEADLINE_EXCEEDED` in `eval()`, cache the consecutive failure count.
2. If failures < N: try `initialize()`, then retry `eval()`. If both succeed, reset counter and return normally. If either fails, return `actualOutput` with UNRELIABLE validity.
3. If failures >= N: give up and throw → ASCE goes to `TFBroken`.

**Problems:**
- Adds significant complexity to `GrpcPythonExecutor.eval()` (stateful counter, re-init logic, retry semantics).
- N would need to be configurable, adding another system property.
- The interaction between re-init, DASU throttling, and input buffering is subtle and needs careful testing.

### Decision — Defer

**None of these options are implemented at this stage.** The rationale:

- Python TFs are primarily intended for **testing and prototyping** before being ported to Scala. In production, the supervisor and PyTfServer will have stable, co-managed lifecycles (systemd units with proper ordering, or eventually Step I's Kafka heartbeats for liveness monitoring).
- The current behavior — clear error log, fast failure after the deadline, ASCE marked as `TFBroken` — is sufficient for the expected use. The operator action (restart Supervisor) is documented.
- If the need arises (e.g., Python TFs used in production with an independent PyTfServer lifecycle), Option 3 can be implemented then with full test coverage.

**The problem is known.** The mitigation path is documented here. No new step is added to the plan.
