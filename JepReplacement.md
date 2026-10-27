# JEP → gRPC Replacement Plan

## Overview

Replace JEP (embedded Python in JVM) with gRPC for executing Python Transfer Functions from Java code. The Supervisor runs one external Python gRPC server that executes all Python TFs of the ComputingElements deployed in that Supervisor.

## Architecture

### Current (after Step 0)

```
Supervisor → DASU → ASCE (Java/Scala) → [PythonTransfer[T]] → PythonExecutorTF (JEP)
                                                              ↓
                                                     Embedded Python per ASCE
                                                     (one SharedInterpreter + one thread per ASCE)
```

### Target

```
Supervisor → DASU → ASCE (Java/Scala) → GrpcPythonExecutorTF (gRPC client)
                                                        ↓ (shared channel)
                                            [External] Python gRPC Server (one per Supervisor)
                                                        ↓
                                               Dict[asceId → TF instance]
```

## Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Server location | New `PyTfServer` module | Clean separation, own build lifecycle |
| Server lifecycle | Externally managed | Started via systemd/supervisor; IAS only connects |
| Connection model | Single shared gRPC channel per Supervisor | Most efficient, one OS connection |
| Migration | Full replacement | Remove JEP entirely |
| gRPC call style | Blocking synchronous | Drop-in for JEP sync model, preserves timeout handling |

---

## Step 0 — Separate Scala / Java / Python Execution Paths (DONE)

Python and Java TFs previously shared `JavaTransfer[T]` and `JavaTransferExecutor`. We've split them so each language now has its own dedicated trait and executor:

| Language | Trait | Executor Base |
|---|---|---|
| Scala | `ScalaTransfer[T]` | `ScalaTransferExecutor` (Scala, user-provided) |
| Java | `JavaTransfer[T]` | `JavaTransferExecutor` (Java, user-provided) |
| Python | `PythonTransfer[T]` | `PythonTransferExecutor` (Scala abstract, `IasIOJ` types) |

**Key detail:** `PythonTransferExecutor` is a Scala abstract class (like `ScalaTransferExecutor`) but uses Java collection types (`JavaMap`, `JavaSet`, `IasIOJ`) so that `PythonExecutorTF` (Java) can extend it. The `@throws[Exception]` annotation is used instead of Java's `throws` keyword for Scala 3 compatibility.

**Files changed (Step 0):**

- **NEW** `CompElement/src/main/scala/org/eso/ias/asce/transfer/PythonTransfer.scala`
- **NEW** `CompElement/src/main/scala/org/eso/ias/asce/transfer/PythonTransferExecutor.scala`
- **MODIFIED** `CompElement/src/main/scala/org/eso/ias/asce/ComputingElement.scala` — Python case now uses `PythonTransfer[T]`
- **MODIFIED** `CompElement/src/main/java/org/eso/ias/asce/transfer/PythonExecutorTF.java` — now extends `PythonTransferExecutor`
- **MODIFIED** `CompElement/src/test/scala/org/eso/ias/asce/test/TestPyTF.scala` — uses `PythonTransfer`
- **MODIFIED** `CompElement/src/test/scala/org/eso/ias/asce/test/TestMinMaxPyTF.scala` — uses `PythonTransfer`

All 16 existing tests pass.

---

## Step 1 — Protobuf Definitions

**Location:** `CompElement/src/main/proto/ias/transfer/`

### `types.proto` — Shared Data Types

```protobuf
syntax = "proto3";
package ias.transfer;

enum OperationalMode {
  STARTUP = 1;
  INITIALIZATION = 2;
  CLOSING = 3;
  SHUTTEDDOWN = 4;
  MAINTENANCE = 5;
  OPERATIONAL = 6;
  DEGRADED = 7;
  UNKNOWN = 8;
  MALFUNCTIONING = 9;
}

enum IASType {
  LONG = 1;
  INT = 2;
  SHORT = 3;
  BYTE = 4;
  DOUBLE = 5;
  FLOAT = 6;
  BOOLEAN = 7;
  CHAR = 8;
  STRING = 9;
  TIMESTAMP = 10;
  ARRAYOFDOUBLES = 11;
  ARRAYOFLONGS = 12;
  ALARM = 13;
}

enum AlarmState {
  CLEAR_UNACK = 1;
  CLEAR_ACK = 2;
  SET_UNACK = 3;
  SET_ACK = 4;
}

enum Priority {
  LOW = 1;
  MEDIUM = 2;
  HIGH = 3;
  CRITICAL = 4;
}

enum Validity {
  RELIABLE = 1;
  UNRELIABLE = 2;
}

message Value {
  oneof value {
    int64 int64Value = 1;
    double doubleValue = 2;
    bool boolValue = 3;
    string stringValue = 4;
    DoubleArray doubleArrayValue = 5;
    Int64Array int64ArrayValue = 6;
    Alarm alarmValue = 7;
  }
}

message DoubleArray {
  repeated double values = 1;
}

message Int64Array {
  repeated int64 values = 1;
}

message Alarm {
  AlarmState state = 1;
  Priority priority = 2;
}

message IasIO {
  string id = 1;
  string runningId = 2;
  OperationalMode mode = 3;
  IASType iasType = 4;
  Validity validity = 5;
  Value value = 6;
  int64 productionTStamp = 7;
  map<string, string> props = 8;
}

message IasioInfo {
  string id = 1;
  IASType iasType = 2;
}
```

### `pytf_service.proto` — gRPC Service

```protobuf
syntax = "proto3";
package ias.transfer;

import "ias/transfer/types.proto";

service PyTfService {
  rpc Initialize(InitRequest) returns (InitResponse);
  rpc Eval(EvalRequest) returns (EvalResponse);
  rpc Shutdown(ShutdownRequest) returns (ShutdownResponse);
}

message InitRequest {
  string asceId = 1;
  string asceRunningId = 2;
  string pythonClassName = 3;
  int64 validityTimeFrame = 4;
  map<string, string> props = 5;
  int32 templateInstance = 6;
  repeated IasioInfo inputsInfo = 7;
  IasioInfo outputInfo = 8;
}

message InitResponse {
  bool success = 1;
  string error = 2;
}

message EvalRequest {
  string asceId = 1;
  map<string, IasIO> compInputs = 2;
  IasIO actualOutput = 3;
}

message EvalResponse {
  bool success = 1;
  string error = 2;
  IasIO result = 3;
}

message ShutdownRequest {
  string asceId = 1;
}

message ShutdownResponse {
  bool success = 1;
  string error = 2;
}
```

### Build Changes

**`gradle/libs.versions.toml`** — add:
```toml
[versions]
protobuf = "4.x"
grpc = "1.x"

[libraries]
protobuf-java = { module = "com.google.protobuf:protobuf-java", version.ref = "protobuf" }
grpc-netty = { module = "io.grpc:grpc-netty-shaded", version.ref = "grpc" }
grpc-protobuf = { module = "io.grpc:grpc-protobuf", version.ref = "grpc" }
grpc-stub = { module = "io.grpc:grpc-stub", version.ref = "grpc" }
grpc-services = { module = "io.grpc:grpc-services", version.ref = "grpc" }
```

**`CompElement/build.gradle.kts`** — add protobuf/gRPC plugin, generate both Java and Python stubs.

---

## Step 2 — New `PyTfServer` Module

### Structure

```
PyTfServer/
  build.gradle.kts
  src/main/python/
    IasPyTfServer/
      __init__.py
      server.py        # Entry point — starts gRPC server
      service.py       # PyTfServicer implementation
      tf_manager.py    # Per-ASCE TF instance lifecycle
```

### `tf_manager.py` — Core State Management

```python
class TfManager:
    def __init__(self):
        self.tf_instances: dict[str, TransferFunction] = {}

    def initialize(self, asceId: str, pythonClassName: str, ...) -> InitResponse:
        # Dynamically import pythonClassName
        # Instantiate: cls(asceId, asceRunningId, validityTimeFrame, props, templateInstance)
        # Call tf.initialize(inputsInfo, outputInfo)
        # Store in self.tf_instances[asceId]

    def eval(self, asceId: str, compInputs: dict, actualOutput: IASIO) -> IasIO:
        tf = self.tf_instances[asceId]
        return tf.eval(compInputs, actualOutput)

    def shutdown(self, asceId: str) -> ShutdownResponse:
        tf = self.tf_instances.pop(asceId)
        tf.shutdown()
```

### `service.py` — gRPC Servicer

- Implements `PyTfServiceServicer` from generated stubs
- Converts proto messages ↔ Python `IASIO` / `IasioInfo` objects
- Delegates all logic to `TfManager`

### `server.py` — Entry Point

- Parses CLI args: `--port`
- Starts gRPC server (plaintext, no TLS needed for local communication)
- Registers servicer

### `build.gradle.kts`

- Depends on `:CompElement` for proto file access
- Generates Python proto files during build
- Installs Python package to `$IAS_ROOT/lib/pythonX.Y/site-packages/IasPyTfServer/`
- Installs `server.py` as a script in `$IAS_ROOT/bin/`

---

## Step 3 — Java gRPC Client

### New `GrpcPythonExecutorTF.java`

Replaces `PythonExecutorTF.java`. Extends `PythonTransferExecutor[T]` (not `JavaTransferExecutor`).

```java
public class GrpcPythonExecutorTF<T> extends PythonTransferExecutor<T> {
    private final String asceId;
    private final PyTfServiceBlockingStub stub;
    private IASTypes outputType;

    // 6-arg constructor: asceId, asceRunningId, validityTimeFrame, props, pythonClassName, serverAddress
    public GrpcPythonExecutorTF(String cEleId, String cEleRunningId,
                                  long validityTimeFrame, Properties props,
                                  String pythonClassName, String serverAddress) {
        super(cEleId, cEleRunningId, validityTimeFrame, props);
        this.asceId = cEleId;
        ManagedChannel channel = GrpcChannelManager.getChannel(serverAddress);
        this.stub = PyTfServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void initialize(Set<IasioInfo> inputsInfo, IasioInfo outputInfo) {
        InitRequest req = buildInitRequest(inputsInfo, outputInfo);
        InitResponse resp = stub.initialize(req);
        if (!resp.getSuccess()) throw new Exception(resp.getError());
        outputType = outputInfo.iasioType();
    }

    @Override
    public IasIOJ<T> eval(Map<String, IasIOJ<?>> compInputs, IasIOJ<T> actualOutput) {
        EvalRequest req = protoUtil.toEvalRequest(asceId, compInputs, actualOutput);
        EvalResponse resp = stub.eval(req);
        if (!resp.getSuccess()) throw new Exception(resp.getError());
        return protoUtil.toIasIOJ(resp.getResult(), outputType);
    }

    @Override
    public void shutdown() {
        // Does NOT shutdown the shared channel
        stub.shutdown(ShutdownRequest.newBuilder().setAsceId(asceId).build());
    }
}
```

### New `GrpcChannelManager.java` — Shared Channel

```java
public class GrpcChannelManager {
    private static final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> refCounts = new ConcurrentHashMap<>();

    public static ManagedChannel getChannel(String serverAddress) {
        ManagedChannel ch = channels.computeIfAbsent(serverAddress, addr -> {
            String[] parts = addr.split(":");
            return ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1]))
                .usePlaintext()
                .build();
        });
        refCounts.computeIfAbsent(serverAddress, k -> new AtomicInteger(0)).incrementAndGet();
        return ch;
    }

    public static void releaseChannel(String serverAddress) {
        AtomicInteger count = refCounts.get(serverAddress);
        if (count != null && count.decrementAndGet() == 0) {
            ManagedChannel ch = channels.remove(serverAddress);
            if (ch != null) ch.shutdown();
        }
    }
}
```

### New `ProtoUtil.java` — Serialization Utilities

Bidirectional conversion:
- `IasIOJ<?>` ↔ `proto.IasIO`
- `IasioInfo` ↔ `proto.IasioInfo`
- Handles all 13 IAS types with proper oneof handling in `Value`

---

## Step 4 — Thread Configuration Through the Chain

The existing `SupervisorDao.pyTfServerAddr` field (already defined, validated, but **unused**) needs to flow down to each `GrpcPythonExecutorTF`.

### GAP 1 — `Supervisor.scala` (~line 604)

Read `pyTfServerAddr` and pass it to the DASU factory:

```scala
val pyTfServerAddr = supervDao.getPyTfServerAddr()

val factory = (dd: DasuDao, i: Identifier, op: OutputPublisher, id: InputSubscriber) =>
  DasuImpl(dd, i, op, id, refreshRate, validityThreshold, pyTfServerAddr)
```

### GAP 2 — `Supervisor.scala` — factory type signature

Change from:
```scala
dasuFactory: (DasuDao, Identifier, OutputPublisher, InputSubscriber) => Dasu
```
To:
```scala
dasuFactory: (DasuDao, Identifier, OutputPublisher, InputSubscriber, String) => Dasu
```

This affects all constructor overloads (~line 81, 231, 266) and the DASU instantiation call (~line 174).

### GAP 3 — `DasuImpl.scala` (~line 705, 39)

Add `pyTfServerAddr` parameter to `DasuImpl.apply` and the constructor.

### GAP 4 — `DasuImpl.scala` (~line 78-85) — merge address into ASCE properties

```scala
val propsForAsce = new Properties()
propsForAsce.setProperty("pyTfServerAddr", pyTfServerAddr)
asce.getProps.forEach(p => propsForAsce.setProperty(p.getName, p.getValue))
```

This is the least-invasive approach: the `Properties` object already flows through the entire chain to the executor.

### GAP 5 — `TransferFunctionSetting.scala` (~line 231-233)

For Python TF constructor args, add server address from properties:

```scala
val serverAddr = props.getProperty("pyTfServerAddr", "")
val args: Array[Any] = Array(asceId, asceRunningId, validityTimeFrame.toLong,
                              props, className, serverAddr)
```

Also update the constructor detection at ~line 204 to look for 6-arg constructors for Python TFs.

---

## Phase 1 — Remove JEP (DONE)

Executed before Step 1 to establish a clean baseline with zero JEP dependency.

### What was done

1. **Replaced `PythonExecutorTF.java`** — Removed all JEP imports (`jep.Interpreter`, `jep.SharedInterpreter`, `jep.python.PyObject`) and all JEP logic (interpreter lifecycle, single-thread executor, Java↔Python type conversion). Replaced with a stub that extends `PythonTransferExecutor`, keeps the same 5-arg constructor signature (so `TransferFunctionSetting` compiles unchanged), and throws `UnsupportedOperationException` from `eval()` and `initialize()`. Prints a clear warning message at construction time to stdout.

2. **Deleted `TestJep.java`** — Removed `CompElement/src/test/java/org/eso/ias/asce/test/transfer/TestJep.java`.

3. **`CompElement/build.gradle.kts`** — Removed `implementation(libs.jep)` dependency. Removed JEP-specific test configuration (`java.library.path` for `libjep.so`, JEP PYTHONPATH setup). Added `exclude` rules for `TestPyTF` and `TestMinMaxPyTF` (these tests cannot run until Phase 2 provides `GrpcPythonExecutorTF`).

4. **`gradle/libs.versions.toml`** — Removed `jep` version and library entries.

5. **`AGENTS.md`** — Removed `jep` from pip dependencies. Updated Java/Python interop note to say "gRPC (JEP removed, Phase 1 complete)". Removed `Python_ROOT_DIR` JEP-specific description and `LD_LIBRARY_PATH` requirement. Removed `JEP_JVM` reference.

6. **`.github/workflows/ias-build-install.yml`** — Removed `pip install jep`, removed `LD_LIBRARY_PATH` and `JEP_JVM` configuration, removed `libjep.so` verification step.

7. **`JenkinsFiles/python-requirements.txt`** — Removed `jep`.

8. **`buildSrc/src/main/kotlin/IasBuild.kt`** — Updated comment (line 257): "run python code" (removed "with jep").

9. **`BasicTypes/src/main/python/IasBasicTypes/Alarm.py`** — Updated comment about `string_repr`: now references both `PythonExecutorTF.java` and `GrpcPythonExecutorTF`.

10. **`PythonTransferExecutor.scala`** — Updated docstring: removed "runs via JEP which requires Java types".

11. **`TransferFunctionSetting.scala`** — Updated docstring: removed reference to JEP GitHub link.

### Files changed (Phase 1)

| File | Change |
|---|---|
| `CompElement/src/main/java/org/eso/ias/asce/transfer/PythonExecutorTF.java` | JEP stub (replaces full JEP impl) |
| `CompElement/src/test/java/org/eso/ias/asce/test/transfer/TestJep.java` | **Deleted** |
| `CompElement/build.gradle.kts` | Removed JEP dep + test config; exclude Python TF tests |
| `gradle/libs.versions.toml` | Removed `jep` entries |
| `AGENTS.md` | Removed `jep` from pip deps and docs |
| `.github/workflows/ias-build-install.yml` | Removed JEP setup steps |
| `JenkinsFiles/python-requirements.txt` | Removed `jep` |
| `buildSrc/src/main/kotlin/IasBuild.kt` | Comment update |
| `BasicTypes/src/main/python/IasBasicTypes/Alarm.py` | Comment update |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/PythonTransferExecutor.scala` | Comment update |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/TransferFunctionSetting.scala` | Comment update |

### Verification

- `./gradlew build` — **SUCCESS** (184 tasks, all pass)
- Zero JEP references remain in source/build files
- Python TF tests (`TestPyTF`, `TestMinMaxPyTF`) are excluded from the test run (will be re-enabled in Phase 2)

---

## Step 6 — Python TF Compatibility

The Python `TransferFunction` base class, `IASIO`, and `IasioInfo` require **no changes**. They remain unchanged. User-written Python TFs are unaffected. The gRPC server's `service.py` handles proto ↔ Python object conversion transparently.

---

## Step 7 — Tests & CI

### Unit Tests

- New `TestGrpcPythonExecutorTF.java` — uses in-process gRPC server stub
- New `test_tf_manager.py` — tests TfManager lifecycle
- New `test_service.py` — tests proto conversion and servicer methods

### Integration Tests

- Update existing Python TF integration tests to use gRPC
- End-to-end: Supervisor starts, connects to external PyTfServer, evaluates Python TFs

### CI

- `AGENTS.md` updated: `pip install grpcio` (replaces `pip install jep`)
- Verify `grpcio` in CI Python environment setup

---

## File Summary (all steps)

### New Files (Step 0 — done)

| File | Description |
|---|---|
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/PythonTransfer.scala` | Python-specific transfer trait |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/PythonTransferExecutor.scala` | Python-specific executor base (Scala abstract) |

### Modified Files (Phase 1 — JEP removed, done)

| File | Changes |
|---|---|
| `CompElement/src/main/java/org/eso/ias/asce/transfer/PythonExecutorTF.java` | Replaced JEP implementation with stub (throws `UnsupportedOperationException`) |
| `CompElement/src/test/java/org/eso/ias/asce/test/transfer/TestJep.java` | **Deleted** |
| `CompElement/build.gradle.kts` | Removed JEP dep, removed JEP test config, excluded Python TF tests |
| `gradle/libs.versions.toml` | Removed `jep` entries |
| `AGENTS.md` | Removed `jep` from pip deps and JEP-specific docs |
| `.github/workflows/ias-build-install.yml` | Removed JEP setup steps |
| `JenkinsFiles/python-requirements.txt` | Removed `jep` |
| `buildSrc/src/main/kotlin/IasBuild.kt` | Comment update |
| `BasicTypes/src/main/python/IasBasicTypes/Alarm.py` | Comment update |

### New Files (future steps)

| File | Description |
|---|---|
| `CompElement/src/main/proto/ias/transfer/types.proto` | Shared data type definitions |
| `CompElement/src/main/proto/ias/transfer/pytf_service.proto` | gRPC service definition |
| `CompElement/src/main/java/org/eso/ias/asce/transfer/GrpcPythonExecutorTF.java` | Java gRPC client replacing PythonExecutorTF |
| `CompElement/src/main/java/org/eso/ias/asce/transfer/GrpcChannelManager.java` | Shared channel management with ref counting |
| `CompElement/src/main/java/org/eso/ias/asce/transfer/ProtoUtil.java` | IasIOJ ↔ proto serialization utilities |
| `PyTfServer/` | New module (entire directory) |
| `PyTfServer/build.gradle.kts` | Module build config |
| `PyTfServer/src/main/python/IasPyTfServer/__init__.py` | Package init |
| `PyTfServer/src/main/python/IasPyTfServer/server.py` | Server entry point |
| `PyTfServer/src/main/python/IasPyTfServer/service.py` | gRPC servicer implementation |
| `PyTfServer/src/main/python/IasPyTfServer/tf_manager.py` | Per-ASCE TF lifecycle management |

### Modified Files (Step 0 — done)

| File | Changes |
|---|---|
| `CompElement/src/main/scala/org/eso/ias/asce/ComputingElement.scala` | Python case uses `PythonTransfer[T]` |
| `CompElement/src/main/java/org/eso/ias/asce/transfer/PythonExecutorTF.java` | Extends `PythonTransferExecutor` |
| `CompElement/src/test/scala/org/eso/ias/asce/test/TestPyTF.scala` | Uses `PythonTransfer` |
| `CompElement/src/test/scala/org/eso/ias/asce/test/TestMinMaxPyTF.scala` | Uses `PythonTransfer` |

### Modified Files (future steps)

| File | Changes |
|---|---|
| `Supervisor/src/main/scala/org/eso/ias/supervisor/Supervisor.scala` | Read pyTfServerAddr, pass to DASU factory, update factory signature |
| `DistributedUnit/src/main/scala/org/eso/ias/dasu/DasuImpl.scala` | Accept pyTfServerAddr, merge into ASCE properties |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/TransferFunctionSetting.scala` | Pass server address to Python executor (6-arg constructor) |
| `CompElement/build.gradle.kts` | Remove JEP, add gRPC/protobuf plugin and dependencies |
| `gradle/libs.versions.toml` | Remove JEP, add gRPC/protobuf versions |
| `settings.gradle.kts` | Add `PyTfServer` module |
| `AGENTS.md` | Remove JEP from pip deps, add grpcio |

### Deleted Files (Step 5)

| File |
|---|
| `CompElement/src/main/java/org/eso/ias/asce/transfer/PythonExecutorTF.java` |
| `CompElement/src/test/java/org/eso/ias/asce/test/transfer/TestJep.java` |

---

## Implementation Order

1. ~~Phase 1: Remove JEP (DONE)~~ — zero JEP dependency, stub `PythonExecutorTF`, tests excluded
2. ~~Step 0: Separate Scala / Java / Python execution paths (DONE)~~
3. **Step 1: Proto definitions + build config** — protobuf generation for Java and Python
3. **Step 2: PyTfServer module** — Python server skeleton with mock service
4. **Step 3: GrpcPythonExecutorTF + GrpcChannelManager + ProtoUtil** — Java client with in-process server tests
5. **Step 4: Configuration threading** — wire `pyTfServerAddr` from Supervisor → DASU → ASCE → executor
6. **Step 5: Remove JEP** — delete old code, clean up build and test configuration
7. **Step 6: Python TF compatibility** — verify no changes needed
8. **Step 7: Integration tests** — full end-to-end with real Python TFs over gRPC
