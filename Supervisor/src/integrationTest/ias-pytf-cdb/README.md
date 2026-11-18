# Supervisor gRPC Python TF Integration Test CDB

This CDB configures a single Supervisor with three DASUs for integration testing
the gRPC-based Python TF executor. Two DASUs run Python transfer functions via an
external PyTfServer process over gRPC; the third runs a Scala TF as control.

## Data Flow

```
min_max_input_val (DOUBLE)  ──→  pytf_minmax_a (Python: MinMaxThreshold)    ──→  alarm_min_max (ALARM)
                                                                      │
al1_multi_input (ALARM)       ┌────────────────────────────────────────┘
al2_multi_input (ALARM)  ─────┤  pytf_multi_a (Python: MultiplicityThreshold)    ──→  alarm_multi (ALARM)
al3_multi_input (ALARM)       └────────────────────────────────────────┐
                                                                      │
bool_input_val (BOOLEAN)   ──→  bool_alarm_a (Scala: BoolToAlarm)      ──→  alarm_bool (ALARM)
```

## DASUs

| DASU | ASCE | Transfer Function | Language |
|---|---|---|---|
| `pytf_minmax_d` | `pytf_minmax_a` | `IasTransferFunction.MinMaxThreshold` | Python |
| `pytf_multi_d` | `pytf_multi_a` | `IasTransferFunction.MultiplicityThreshold` | Python |
| `bool_alarm_d` | `bool_alarm_a` | `org.eso.ias.tranfer.BoolToAlarm` | Scala |

## ASCEs

### `pytf_minmax_a` (Python — MinMaxThreshold)

Single DOUBLE input, ALARM output. The output alarm activates when the input value
exceeds a configurable threshold (set to 20 via the `IasTransferFunction.MinMaxThreshold.py`
property).

| Input | Output |
|---|---|
| `min_max_input_val` (DOUBLE) | `alarm_min_max` (ALARM) |

**Properties:** `IasTransferFunction.MinMaxThreshold.py = 20`

### `pytf_multi_a` (Python — MultiplicityThreshold)

Three ALARM inputs, one ALARM output. The output alarm activates when the number of
SET alarms meets or exceeds a configurable threshold (defaults per the TF implementation).
Properties from SET inputs are propagated and merged on the output.

| Inputs | Output |
|---|---|
| `al1_multi_input`, `al2_multi_input`, `al3_multi_input` (ALARM) | `alarm_multi` (ALARM) |

### `bool_alarm_a` (Scala — BoolToAlarm)

Single BOOLEAN input, ALARM output. Converts a boolean value to an alarm (no gRPC,
runs within the JVM). Serves as a control to confirm non-Python TFs are unaffected.

| Input | Output |
|---|---|
| `bool_input_val` (BOOLEAN) | `alarm_bool` (ALARM) |

## Test Requirements

- A PyTfServer process must be running and its address configured in the Supervisor
  CDB entry as `pyTfServerAddr` (e.g. `"localhost:50051"`).

## Files

| File | Description |
|---|---|
| `CDB/ias.json` | Global IAS config (refresh rate, Kafka URL, validity threshold) |
| `CDB/SUPERVISOR/supervisor.json` | Supervisor config with three DASUs |
| `CDB/DASU/*.json` | DASU definitions |
| `CDB/ASCE/*.json` | ASCE definitions with TF class and properties |
| `CDB/TF/tfs.json` | Transfer function registry (language: PYTHON or SCALA) |
| `CDB/IASIO/iasios.json` | All IASIO definitions (inputs and outputs) |
| `CDB/TEMPLATE/templates.json` | Empty (no templated inputs) |
