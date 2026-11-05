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

Completed across Steps 1c and 1d. See those sections.

**`gradle/libs.versions.toml`** — `protobuf = "4.28.2"`, `grpc = "1.68.0"`, gRPC library entries added.

**`CompElement/build.gradle.kts`** — protobuf/gRPC plugin, generate Java and Python stubs.

---

## Step 2 — gRPC Service RPCs

Add Initialize/Eval/Shutdown RPCs to `pytf_service.proto`. The proto already has `IasIO` and `IasioInfoProto` messages (Step 1d). Need to define the RPC messages and update the service.

## Step 3 — New `PyTfServer` Module (Python gRPC Server)

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

## Step 4 — GrpcPythonExecutor Real RPCs

### `GrpcPythonExecutor.scala`

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

## Step 5 — Thread Configuration Through the Chain

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
| `CompElement/src/main/proto/pytf_service.proto` | gRPC service definition (Initialize/Eval/Shutdown RPCs) |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/GrpcChannelManager.scala` | Shared channel management with ref counting |
| `PyTfServer/src/main/python/IasPyTfServer/service.py` | gRPC servicer implementation |
| `PyTfServer/src/main/python/IasPyTfServer/tf_manager.py` | Per-ASCE TF lifecycle management |

### New Files (Step 1c — BasicTypes proto, done)

| File | Description |
|---|---|
| `BasicTypes/src/main/proto/types.proto` | 5 shared IAS type enums (IASType, Validity, OperationalMode, AlarmState, Priority) |
| `BasicTypes/src/main/python/IasProtobufTypes/__init__.py` | Package init for generated stubs |
| `BasicTypes/src/test/scala/org/eso/ias/basictypes/test/TestProtoTypes.scala` | 6 Scala unit tests for enum round-trip conversion |
| `BasicTypes/src/test/python/test_proto_types.py` | 6 Python unit tests for enum round-trip conversion |

### Modified Files (Step 0 — done)

| File | Changes |
|---|---|
| `CompElement/src/main/scala/org/eso/ias/asce/ComputingElement.scala` | Python case uses `PythonTransfer[T]` |
| `CompElement/src/main/java/org/eso/ias/asce/transfer/PythonExecutorTF.java` | Extends `PythonTransferExecutor` |
| `CompElement/src/test/scala/org/eso/ias/asce/test/TestPyTF.scala` | Uses `PythonTransfer` |
| `CompElement/src/test/scala/org/eso/ias/asce/test/TestMinMaxPyTF.scala` | Uses `PythonTransfer` |

### Modified Files (Step 1c — done)

| File | Changes |
|---|---|
| `BasicTypes/build.gradle.kts` | Added protobuf plugin, gRPC deps, `generatePythonProto`, `CopyPyProtoMods`, source set adjustment |

### New Files (Step 1d — IasIO proto + conversions, done)

| File | Description |
|---|---|
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/ProtoUtil.scala` | IasIO/IasioInfo protobuf conversions with Scaladoc |
| `CompElement/src/test/scala/org/eso/ias/asce/test/TestProtobufIasIO.scala` | 10 round-trip tests for IasIO and IasioInfo |

### Modified Files (Step 1d — done)

| File | Changes |
|---|---|
| `BasicTypes/src/main/proto/types.proto` | Added `DoubleArray`, `Int64Array`, `Alarm`, `Value` messages |
| `BasicTypes/src/main/scala/org/eso/ias/types/ProtoUtil.scala` | Added `alarmFrom`/`To`, `valueFrom`/`To`; Scaladoc on all methods |
| `BasicTypes/src/test/scala/org/eso/ias/basictypes/test/TestProtoTypes.scala` | Expanded to 18 tests (all 13 Value types, Alarm, errors) |
| `CompElement/src/main/proto/pytf_service.proto` | Added `import "types.proto"`, `IasioInfoProto`, `IasIO` messages |
| `CompElement/build.gradle.kts` | Added `addIncludeDir` for BasicTypes proto; updated Python proto gen |

### Modified Files (future steps)

| File | Changes |
|---|---|
| `Supervisor/src/main/scala/org/eso/ias/supervisor/Supervisor.scala` | Read pyTfServerAddr, pass to DASU factory, update factory signature |
| `DistributedUnit/src/main/scala/org/eso/ias/dasu/DasuImpl.scala` | Accept pyTfServerAddr, merge into ASCE properties |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/TransferFunctionSetting.scala` | Pass server address to Python executor (6-arg constructor) |
| `CompElement/build.gradle.kts` | Replace Ping proto with full IAS-type proto; add `GrpcChannelManager` |
| `AGENTS.md` | Add grpcio to pip deps |

### Deleted Files (Step 5)

| File |
|---|
| `CompElement/src/main/java/org/eso/ias/asce/transfer/PythonExecutorTF.java` |
| `CompElement/src/test/java/org/eso/ias/asce/test/transfer/TestJep.java` |

---

## Step 1d — Value/Alarm Proto + IasIO in CompElement (DONE)

Extended the protobuf protocol with actual IAS data types beyond the PoC Ping RPC. Moved `Value` and `Alarm` messages into BasicTypes (where they logically belong alongside the enums), and `IasIO`/`IasioInfo` into CompElement (where the ASCE-specific serialization lives). Created bidirectional Scala conversion utilities with full test coverage.

### Why

The original plan (Step 1 in the doc) defined all messages in a single proto within CompElement. With Step 1c we moved the 5 enums to BasicTypes, but `IasIO` and `Value` still lived only in the plan. This step realizes them in the actual proto files, properly split across BasicTypes (fundamental types) and CompElement (ASCE-specific types).

### What was done

**BasicTypes `types.proto`** — added 4 messages on top of the 5 existing enums:
- `DoubleArray` — `repeated double values`
- `Int64Array` — `repeated int64 values`
- `Alarm` — `AlarmState state + Priority priority`
- `Value` — `oneof` with all 7 variants (`int64Value`, `doubleValue`, `boolValue`, `stringValue`, `doubleArrayValue`, `int64ArrayValue`, `alarmValue`). Comments document which IASTypes map to which variant.

**BasicTypes `ProtoUtil.scala`** — expanded from 5 enum conversions to full basic-type serialization:
- Added `alarmFrom`/`alarmTo` (uses `Alarm.valueOf` for deserialization)
- Added `valueFrom`/`valueTo` covering all 13 IAS types with proper narrowing (INT/SHORT/BYTE stored as `int64Value`, FLOAT as `doubleValue`, CHAR as `stringValue`)
- Added Scaladoc to all public methods
- ASCE-specific types remain in CompElement's ProtoUtil

**CompElement `pytf_service.proto`** — added `import "types.proto"`, then `IasioInfoProto` and full `IasIO` message. IasIO references `ias.types.OperationalMode`, `ias.types.IASType`, `ias.types.Validity`, and `ias.types.Value` from BasicTypes. Enum values in the proto use short names (e.g. `RELIABLE` not `VALIDITY_RELIABLE`, `LONG` not `IASTYPE_LONG`) since they're disambiguated by their parent enum type. Includes all 7 transport timestamps.

**CompElement `build.gradle.kts`** — added `addIncludeDir` pointing to BasicTypes proto sources, enabling the `import "types.proto"`. Updated `generatePythonProto` to also pass BasicTypes' proto path.

**CompElement `ProtoUtil.scala`** (new) — ASCE-level conversion utilities:
- `iasIOFrom`/`iasIOTo` — convert `InOut[?]` + `IASTypes` to/from proto `IasIO` (zero timestamps treated as absent)
- `iasioInfoFrom`/`iasioInfoTo` — convert `IasioInfo` to/from `IasioInfoProto`
- Delegates basic type conversions to `org.eso.ias.types.ProtoUtil`
- Full Scaladoc on all public methods

**Tests:**
- BasicTypes `TestProtoTypes.scala` — expanded from 6 enum tests to 18 tests covering all 13 IAS Value types (with narrowing), Alarm round-trip, and error handling for unset value case
- CompElement `TestProtobufIasIO.scala` (new) — 8 IasIO tests (all fields, no value, Alarm, zero timestamps, arrays, all timestamps, INT/SHORT/BYTE narrowing) + 2 IasioInfo tests

### Architecture Rationale

The split follows a clear ownership boundary:

| Layer | Module | Proto | ProtoUtil |
|---|---|---|---|
| **Basic types** | BasicTypes | `types.proto` (enums + `Value`, `Alarm`, arrays) | `org.eso.ias.types.ProtoUtil` (primitives) |
| **ASCE types** | CompElement | `pytf_service.proto` (`IasIO`, `IasioInfoProto`) | `org.eso.ias.asce.transfer.ProtoUtil` (ASCE wrappers) |

CompElement proto imports BasicTypes proto via `import "types.proto"` (enabled by Gradle `addIncludeDir`). CompElement's Scala ProtoUtil delegates to BasicTypes' ProtoUtil for basic conversions.

### Files changed (Step 1d — done)

| File | Change |
|---|---|
| `BasicTypes/src/main/proto/types.proto` | Added `DoubleArray`, `Int64Array`, `Alarm`, `Value` messages |
| `BasicTypes/src/main/scala/org/eso/ias/types/ProtoUtil.scala` | Added `alarmFrom`/`To`, `valueFrom`/`To`; Scaladoc on all methods |
| `BasicTypes/src/test/scala/org/eso/ias/basictypes/test/TestProtoTypes.scala` | Expanded to 18 tests (all 13 Value types, Alarm, errors) |
| `CompElement/src/main/proto/pytf_service.proto` | Added `import "types.proto"`, `IasioInfoProto`, `IasIO` messages |
| `CompElement/build.gradle.kts` | Added `addIncludeDir` for BasicTypes proto; updated Python proto gen |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/ProtoUtil.scala` | **NEW** — IasIO/IasioInfo conversions with Scaladoc |
| `CompElement/src/test/scala/org/eso/ias/asce/test/TestProtobufIasIO.scala` | **NEW** — 10 round-trip tests |

### Verification

- `./gradlew :BasicTypes:test` — SUCCESS (all 18 Scala tests pass)
- `./gradlew :BasicTypes:pytest` — SUCCESS (all Python tests pass)
- `./gradlew :CompElement:test` — SUCCESS (all Scala tests pass)
- `./gradlew build` — SUCCESS (full project)

### Remaining

- Add Initialize/Eval/Shutdown RPCs to `pytf_service.proto` (replacing or alongside Ping)
- Update `GrpcPythonExecutor` to call real IasIO-based RPCs
- Implement Python gRPC server (`PyTfServer`) with real TF execution

---

## Step 1 (simplified) — Minimal gRPC PoC (DONE)

Full IAS-type proto deferred. Minimal proto with a single `Ping(string) → bool` RPC to validate gRPC plumbing.

### What's being done

**Proto file:** `CompElement/src/main/proto/pytf_service.proto` — package `pytf`, `java_package = "org.eso.ias.generated.protobuf.tfservice"`. (Package restructuring done in Step 1b.)

**CompElement/build.gradle.kts** — added `com.google.protobuf` plugin (0.9.4), gRPC deps (`grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`, `protobuf-java`, `javax.annotation-api`), protobuf block for Java stub generation. Java source set limited to generated proto dirs (main Java files still compiled by Scala compiler). Explicit `dependsOn(tasks.generateProto)` on both `compileJava` and `ScalaCompile` to ensure proto stubs are generated before any compilation (fixes clean build failure where `compileScala` couldn't resolve `ias.pytf.*` imports).

**gradle/libs.versions.toml** — added `protobuf = "4.28.2"`, `grpc = "1.68.0"`, gRPC library entries.

**PyTfServer module:** new module in `settings.gradle.kts`. Structure follows IAS convention: `PyTfServer/src/main/python/server.py` (top-level script → `$IAS_ROOT/bin/`), `PyTfServer/src/main/python/IasPyTfServer/` (package → `$IAS_ROOT/lib/python/site-packages/`). Python proto stubs are generated by `CompElement` and installed as the `IasProtobufTfserver` package (see Step 1b).

**GrpcPythonExecutor.scala:** replaces the Java `GrpcPythonExecutorTF.java`. Scala class extending `PythonTransferExecutor[T]`, same 5-arg constructor. Reads `pyTfServerAddr` from properties (default `localhost:50051`). `initialize()`, `eval()`, `shutdown()` each call `Ping(compElementRunningId + " <operation>")`. Server returns `success=true` → operation proceeds; `success=false` → throws.

**Python path fully removed from Java types:** With JEP gone, the Python executor path no longer needs Java interop. `PythonTransferExecutor` now uses Scala types (`Map[String, IasIO[?]]`, `Set[IasioInfo]`) like `ScalaTransferExecutor`, eliminating the `IasIOJ` bridge. `PythonTransfer` trait no longer does `flushInputsOnJavaMap` / `CollectionConverters.asJava` — it wraps inputs directly as `IasIO[T]`. User-written Java TFs still use `IasIOJ`/`JavaTransferExecutor` (unchanged).

**TransferFunctionSetting.scala:** updated to load `GrpcPythonExecutor` class instead of `GrpcPythonExecutorTF`.

**Tests:** new `TestGrpcPythonExecutor.scala` with 6 unit tests covering constructor validation (valid params, default/custom server address, null/empty pythonClassName), full ASCE lifecycle (TFBroken when server unreachable), and TransferFunctionSetting integration. All tests run without IPC — no server needed.

### Files changed (Step 1 — done)

| File | Change |
|---|---|
| `CompElement/src/main/proto/pytf_service.proto` | **NEW** — minimal proto (Ping RPC) |
| `CompElement/build.gradle.kts` | Added protobuf plugin, gRPC deps, source set adjustment |
| `gradle/libs.versions.toml` | Added protobuf/gRPC versions and library entries |
| `settings.gradle.kts` | Added `PyTfServer` module |
| `PyTfServer/build.gradle.kts` | **NEW** — minimal module (base plugin) |
| `PyTfServer/src/main/python/server.py` | **NEW** — gRPC server script (Ping impl) |
| `PyTfServer/src/main/python/IasPyTfServer/__init__.py` | **NEW** |
| `PyTfServer/src/main/python/IasPyTfServer/pytf_service_pb2.py` | **GENERATED** (now generated by CompElement, see Step 1b) |
| `PyTfServer/src/main/python/IasPyTfServer/pytf_service_pb2_grpc.py` | **GENERATED** (now generated by CompElement, see Step 1b) |
| `CompElement/src/main/java/org/eso/ias/asce/transfer/PythonExecutorTF.java` | **DELETED** (was Phase 1 stub) |
| `CompElement/src/main/java/org/eso/ias/asce/transfer/GrpcPythonExecutorTF.java` | **DELETED** (replaced by Scala) |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/GrpcPythonExecutor.scala` | **NEW** — Scala gRPC client, 5-arg constructor |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/PythonTransferExecutor.scala` | **MODIFIED** — Java types (`JavaMap`/`IasIOJ`) replaced with Scala types (`Map`/`IasIO`) |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/PythonTransfer.scala` | **MODIFIED** — removed Java interop layer (`IasIOJ`, `flushInputsOnJavaMap`, `CollectionConverters`) |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/TransferFunctionSetting.scala` | **MODIFIED** — loads `GrpcPythonExecutor` instead of `GrpcPythonExecutorTF` |
| `CompElement/src/test/scala/org/eso/ias/asce/test/TestGrpcPythonExecutor.scala` | **NEW** — 6 unit tests (no IPC) |
| `test-reports/build.gradle.kts` | Excluded grpc guava to fix variant conflict |

### Remaining

- Add Initialize/Eval/Shutdown RPCs to `pytf_service.proto` (Step 2)
- Update `GrpcPythonExecutor` with real IasIO-based RPCs (Step 4)
- Implement Python gRPC server with real TF execution (Step 3/PyTfServer)
- End-to-end test: start Python server, run a Python TF, verify full roundtrip

### Build Fix

Clean build (`./gradlew clean build`) initially failed with `value ias is not a member of <root>` in `GrpcPythonExecutorTF.java`. The `generateProto` task wasn't wired into the `compileScala` dependency chain because `compileJava` was `NO-SOURCE` (Java srcDirs only include the generated proto dirs). Fix in `CompElement/build.gradle.kts:58-66`: explicit `dependsOn(tasks.generateProto)` on both `compileJava` and `ScalaCompile`.

---

## Step 1b — Proto Package Restructuring (DONE)

Moved generated proto output into proper packages rather than flat files. This follows the IAS convention of `org.eso.ias...` for Java and `Ias*` for Python.

### What was done

**Reason:** The initial gRPC PoC generated Java classes into `ias.pytf` and Python stubs as flat files with no package. These are both unidiomatic for an IAS project. Generated sources belong in proper packages so they integrate cleanly with the rest of the codebase and install correctly alongside other modules.

**Java package:** `ias.pytf` → `org.eso.ias.generated.protobuf.tfservice`. Updated `java_package` in `pytf_service.proto`. Generated files now land in `build/generated/source/proto/main/java/org/eso/ias/generated/protobuf/tfservice/` and `build/generated/source/proto/main/grpc/org/eso/ias/generated/protobuf/tfservice/`.

**Python package:** flat files → `IasProtobufTfserver`. New `generatePythonProto` task in `CompElement/build.gradle.kts` outputs to `build/generated/python/IasProtobufTfserver/`. A `CopyPyProtoMods` task copies the stubs (plus a hand-written `__init__.py`) into `build/lib/python3.12/site-packages/IasProtobufTfserver/`, where the IAS `InstallLib` task picks them up for installation to `$IAS_ROOT`.

**Checked-in stubs removed:** `PyTfServer/src/main/python/IasPyTfServer/pytf_service_pb2.py` and `pytf_service_pb2_grpc.py` were pre-generated and committed. They're now generated by `CompElement`'s build and installed as the `IasProtobufTfserver` package. `server.py` imports updated accordingly.

**Build.gradle.kts cleanup:** Removed duplicate `compileJava`/`ScalaCompile` dependency blocks that had been accidentally left from earlier editing.

### Flow

```
CompElement/src/main/proto/pytf_service.proto
  → generateProto (Gradle protobuf plugin)
    → Java: build/generated/source/proto/.../org/eso/ias/generated/protobuf/
  → generatePythonProto (grpc_tools.protoc)
    → Python: build/generated/python/IasProtobufTfserver/
  → CopyPyProtoMods
    → site-packages: build/lib/python3.12/site-packages/IasProtobufTfserver/
  → InstallLib
    → $IAS_ROOT/lib/python3.12/site-packages/IasProtobufTfserver/
```

### Files changed (Step 1b — done)

| File | Change |
|---|---|
| `CompElement/src/main/proto/pytf_service.proto` | `java_package` → `org.eso.ias.generated.protobuf.tfservice` |
| `CompElement/build.gradle.kts` | `generatePythonProto` outputs to `IasProtobufTfserver`; new `CopyPyProtoMods` task; removed duplicate dependency blocks |
| `CompElement/src/main/python/IasProtobufTfserver/__init__.py` | **NEW** — package init for generated stubs |
| `CompElement/src/main/scala/org/eso/ias/asce/transfer/GrpcPythonExecutor.scala` | imports → `org.eso.ias.generated.protobuf.tfservice` |
| `PyTfServer/src/main/python/server.py` | imports → `IasProtobufTfserver.pytf_service_pb2` |
| `PyTfServer/src/main/python/IasPyTfServer/pytf_service_pb2.py` | **DELETED** (now generated by CompElement) |
| `PyTfServer/src/main/python/IasPyTfServer/pytf_service_pb2_grpc.py` | **DELETED** (now generated by CompElement) |

---

## Step 1c — BasicTypes Proto Foundation (DONE)

Moved the shared IAS type proto definitions into BasicTypes, where the types they represent already live. BasicTypes now generates protobuf stubs for both Java and Python, following the same build/install pattern established in CompElement (Step 1b).

### Why

The types `IASType`, `Validity`, `OperationalMode`, `AlarmState`, and `Priority` are defined in BasicTypes (`IasBasicTypes/*` Python, `org.eso.ias.types.*` Java/Scala). The proto definitions for these enums logically belong there too — not in CompElement. This way any module that depends on BasicTypes gets access to the proto types, not just CompElement and PyTfServer.

### What was done

**Proto file:** `BasicTypes/src/main/proto/types.proto` — flat file (not in subdirectories) to ensure `grpc_tools.protoc` generates Python stubs directly in the `IasProtobufTypes` package, matching the CompElement convention. Package `ias.types`, `java_package = "org.eso.ias.generated.protobuf.iastypes"`, `java_outer_classname = "IasTypes"`.

Defines 5 protobuf enums (enum values named without type prefix, since they're disambiguated by their parent enum in the generated stubs):
- `IASType` (13 values: `LONG` through `ALARM`)
- `Validity` (2 values: `RELIABLE`, `UNRELIABLE`)
- `OperationalMode` (9 values: `CLOSING` through `MALFUNCTIONING`)
- `AlarmState` (4 values: `CLEAR_ACK` through `SET_ACK`)
- `Priority` (4 values: `LOW` through `CRITICAL`)

**BasicTypes/build.gradle.kts** — added `com.google.protobuf` plugin (0.9.4), gRPC dependencies (`grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`, `protobuf-java`, `javax.annotation`), protobuf block for Java stub generation. Java srcDirs limited to generated proto dirs (main Java files compiled by Scala compiler). Explicit `dependsOn(tasks.generateProto)` on `compileJava` and `ScalaCompile`. New `generatePythonProto` task (uses `grpc_tools.protoc` with recursive proto file discovery via `walk()`). New `CopyPyProtoMods` task copies generated stubs + `__init__.py` to site-packages.

**Python package:** `IasProtobufTypes` (`src/main/python/IasProtobufTypes/__init__.py` — empty). Generated stubs land flat in the package: `types_pb2.py`, `types_pb2_grpc.py`. Import: `from IasProtobufTypes import types_pb2`.

**Unit tests:**
- Scala: `TestProtoTypes.scala` — 6 tests, exhaustive round-trip conversion for all 5 enums (`IASTypeFrom`/`IASTypeTo` etc.), handles protobuf `UNRECOGNIZED` sentinel
- Python: `test_proto_types.py` — 6 tests, verifies bijective mapping between IAS enum values and proto enum values (does NOT assert numeric equality, since IAS enum values have semantic meaning while proto values are wire identifiers)

### Why the proto enum values don't match IAS enum values

The existing IAS Python enums have semantic numeric values (e.g. `IASType.LONG = 1`, `OperationalMode.CLOSING = 0`). Protobuf proto3 requires the first enum value to be 0. The proto enums use 0-based sequential values (`LONG = 0`, `INT = 1`, ...). This is fine — the proto values are only wire identifiers. The conversion logic (`IASTypeFrom`/`IASTypeTo`) maps between them explicitly. The tests verify the mapping is bijective (no collisions, no missing values), not that the numeric values match.

### Flow

```
BasicTypes/src/main/proto/types.proto
  → generateProto (Gradle protobuf plugin)
    → Java: build/generated/source/proto/.../org/eso/ias/generated/protobuf/iastypes/IasTypes.java
  → generatePythonProto (grpc_tools.protoc)
    → Python: build/generated/python/IasProtobufTypes/types_pb2.py
  → CopyPyProtoMods
    → site-packages: build/lib/python3.12/site-packages/IasProtobufTypes/
  → InstallLib
    → $IAS_ROOT/lib/python3.12/site-packages/IasProtobufTypes/
```

### Files changed (Step 1c — done)

| File | Change |
|---|---|
| `BasicTypes/src/main/proto/types.proto` | **NEW** — 5 shared IAS type enums |
| `BasicTypes/build.gradle.kts` | Added protobuf plugin, gRPC deps, `generatePythonProto`, `CopyPyProtoMods`, source set adjustment |
| `BasicTypes/src/main/python/IasProtobufTypes/__init__.py` | **NEW** — package init for generated stubs |
| `BasicTypes/src/test/scala/org/eso/ias/basictypes/test/TestProtoTypes.scala` | **NEW** — 6 Scala unit tests |
| `BasicTypes/src/test/python/test_proto_types.py` | **NEW** — 6 Python unit tests |

### Verification

- `./gradlew :BasicTypes:test` — SUCCESS (all Scala tests pass)
- `./gradlew :BasicTypes:pytest` — SUCCESS (28 Python tests pass, including 6 new proto tests)
- `./gradlew build` — SUCCESS (full project, 203 tasks)

---

## Implementation Order

1. ~~Phase 1: Remove JEP (DONE)~~ — zero JEP dependency, stub `PythonExecutorTF`, tests removed
2. ~~Step 0: Separate Scala / Java / Python execution paths (DONE)~~
3. ~~**Step 1 (simplified): Minimal gRPC PoC (DONE)**~~ — flat proto, Ping RPC, PyTfServer module, GrpcPythonExecutor (Scala), Python path free of Java types, unit tests
4. ~~**Step 1b: Proto package restructuring (DONE)**~~ — Java `org.eso.ias.generated.protobuf.{iastypes,tfservice}`, Python `IasProtobufTfserver`, checked-in stubs removed
5. ~~**Step 1c: BasicTypes proto foundation (DONE)**~~ — `types.proto` with 5 shared IAS enums, Java + Python generation, unit tests
6. ~~**Step 1d: Value/Alarm proto + IasIO in CompElement (DONE)**~~ — `Value`, `Alarm` in BasicTypes; full `IasIO`/`IasioInfo` in CompElement; bidirectional Scala conversions with tests; Scaladoc
7. **Step 2: gRPC service RPCs** — add Initialize/Eval/Shutdown RPCs to `pytf_service.proto`
8. **Step 3: GrpcPythonExecutor real RPCs** — replace Ping with Initialize/Eval/Shutdown calls using CompElement ProtoUtil
9. **Step 4: GrpcChannelManager** — shared gRPC channel with ref counting
10. **Step 5: PyTfServer implementation** — Python gRPC server with real TF execution (TfManager + service)
11. **Step 6: Configuration threading** — wire `pyTfServerAddr` from Supervisor → DASU → ASCE → executor
12. **Step 7: Re-enable Python TF tests** — with running PyTfServer
13. **Step 8: Integration tests** — full end-to-end with real Python TFs over gRPC
