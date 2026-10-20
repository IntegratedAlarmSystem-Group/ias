# Configuration Database (CDB)

The CDB stores the full system topology and parameters for the Integrated Alarm System. It is a file-based database under a `CDB/` directory that supports **JSON** (`.json`) and **YAML** (`.yaml`) interchangeably — the format is auto-detected per file, and mixing formats within the same tree is allowed. An RDB (Hibernate/Oracle/PostgreSQL) backend also exists as a fallback, activated when no `-jCdb` flag is given.

**CLI access:** `-jCdb <path>` or `-j <path>` to use JSON/YAML; `-cdbClass <class>` for a custom Java `CdbReader` implementation.

Enum `CdbFolders` in `Cdb/src/main/java/org/eso/ias/cdb/structuredtext/CdbFolders.java` defines the nine recognized subdirectories.

## Directory Layout

```
CDB/
  ias.json                       — global IAS configuration
  SUPERVISOR/<SupID>.json        — supervisor instances
  DASU/<DasuID>.json             — distributed alarm supernodes
  ASCE/<AsceID>.json             — computing elements
  IASIO/iasios.json              — I/O point registry
  TF/tfs.json                    — transfer function classes
  TEMPLATE/templates.json        — instance templates
  PLUGIN/<PluginID>.json         — external monitor plugins
  CLIENT/<ClientId>.conf         — client-specific configuration
```

File names encode the entity ID (e.g., `DasuTemperature.json` → ID = `DasuTemperature`). Consolidated files (`ias.json`, `tfs.json`, `templates.json`, `iasios.json`) contain arrays or a single object.

---

## 1. ROOT — `CDB/ias.json`

**Single object.** Global system-wide settings. No cross-references.

| Field | Type | Description |
|---|---|---|
| `logLevel` | String | System-wide log level (`"INFO"`, `"DEBUG"`, `"ERROR"`) |
| `refreshRate` | String | Global refresh interval in seconds |
| `validityThreshold` | String | Seconds before a data point is considered stale |
| `hbFrequency` | String | Heartbeat frequency in seconds |
| `bsdbUrl` | String | Kafka/BSDB backend URL, e.g. `"localhost:9092"` |
| `smtp` | String | Email credentials: `"user:password@smtp.server"` (optional) |
| `props` | Array | Arbitrary `{name, value}` property pairs |

**Example:**
```json
{
  "logLevel": "INFO",
  "refreshRate": "5",
  "validityThreshold": "11",
  "hbFrequency": "10",
  "bsdbUrl": "127.0.0.1:9092",
  "smtp": "acaproni:pswd@smtp.test.org",
  "props": [
    { "name": "PropName1", "value": "PropValue1" }
  ]
}
```

---

## 2. SUPERVISOR — `CDB/SUPERVISOR/<SupID>.json`

**One file per supervisor.** Defines which DASUs to deploy on a given host.

| Field | Type | Description |
|---|---|---|
| `id` | String | Unique supervisor ID |
| `hostName` | String | Hostname where the supervisor runs |
| `logLevel` | String | Per-supervisor log level |
| `dasusToDeploy` | Array | DASU deployment entries |

Each `dasusToDeploy` entry:

| Field | Type | Description |
|---|---|---|
| `dasuId` | String | Reference to a DASU in `CDB/DASU/` |
| `templateId` | String \| null | Reference to a Template, or `null` |
| `instance` | String \| null | Instance number within the template range |

**Example:**
```json
{
  "id": "SupervisorWithTemplates",
  "hostName": "almaias.eso.org",
  "logLevel": "INFO",
  "dasusToDeploy": [
    { "dasuId": "DasuTemplateID1", "templateId": "temp-ID1", "instance": "3" },
    { "dasuId": "Dasu1", "templateId": null, "instance": null }
  ]
}
```

**Cross-references:** `dasuId` → `DASU`, `templateId` → `TEMPLATE`

---

## 3. DASU — `CDB/DASU/<DasuID>.json`

**One file per DASU.** A Distributed Alarm Supernode aggregates outputs from child ASCEs into a single alarm output.

| Field | Type | Description |
|---|---|---|
| `id` | String | Unique DASU ID |
| `asceIDs` | Array<String> | ASCE IDs belonging to this DASU |
| `outputId` | String | IASIO ID for the aggregated alarm output |
| `logLevel` | String (optional) | Per-DASU log level |
| `templateId` | String (optional) | Reference to a Template for instantiation |

**Example:**
```json
{
  "id": "DasuTemperature",
  "asceIDs": ["AsceTemperature"],
  "outputId": "TemperatureAlarm",
  "logLevel": "ERROR"
}
```

**Cross-references:** `asceIDs[]` → `ASCE`, `outputId` → `IASIO`, `templateId` → `TEMPLATE`

---

## 4. ASCE — `CDB/ASCE/<AsceID>.json`

**One file per ASCE.** The fundamental alarm processing unit: reads input IASIOs, applies a Transfer Function, writes an output IASIO.

| Field | Type | Description |
|---|---|---|
| `id` | String | Unique ASCE ID |
| `dasuID` | String | Parent DASU this ASCE belongs to |
| `outputID` | String | IASIO ID written by this ASCE |
| `transferFunctionID` | String | FQCN of the TF class |
| `inputIDs` | Array<String> | IASIO IDs read as inputs |
| `props` | Array | TF config: `{name, value}` pairs (e.g. `"org.eso.ias.tf.minmaxthreshold.highOn": "30"`) |
| `templatedInputs` | Array (optional) | Dynamic inputs: `{iasioId, templateId, instanceNum}` |
| `templateId` | String (optional) | Reference to a Template for instantiation |

**Example:**
```json
{
  "id": "AsceTemperature",
  "dasuID": "DasuTemperature",
  "outputID": "TemperatureAlarm",
  "transferFunctionID": "org.eso.ias.asce.transfer.impls.MinMaxThresholdTF",
  "inputIDs": ["Temperature"],
  "props": [
    { "name": "org.eso.ias.tf.minmaxthreshold.highOn",  "value": "30" },
    { "name": "org.eso.ias.tf.minmaxthreshold.highOff", "value": "15" },
    { "name": "org.eso.ias.tf.minmaxthreshold.lowOn",   "value": "-20" },
    { "name": "org.eso.ias.tf.minmaxthreshold.lowOff",  "value": "-10" }
  ]
}
```

**Cross-references:** `dasuID` → `DASU`, `outputID` → `IASIO`, `inputIDs[]` → `IASIO`, `transferFunctionID` → `TF`, `templatedInputs` → `IASIO` + `TEMPLATE`

---

## 5. IASIO — `CDB/IASIO/iasios.json`

**Consolidated file (array).** Registry of all I/O data points — sensor readings, computed values, and alarm outputs.

| Field | Type | Description |
|---|---|---|
| `id` | String | Unique I/O point ID (e.g. `"Temperature"`, `"/test/alarm8"`) |
| `iasType` | String | Data type: `ALARM`, `BOOLEAN`, `DOUBLE`, `LONG`, `INT`, `SHORT`, `BYTE` |
| `shortDesc` | String (optional) | Human-readable description |
| `docUrl` | String (optional) | Documentation URL |
| `canShelve` | Boolean/String (optional) | Whether alarms can be shelved |
| `sound` | String (optional) | Alarm sound: `"NONE"`, `"TYPE2"`, etc. |
| `emails` | String (optional) | Email addresses for notifications |
| `templateId` | String (optional) | Reference to a Template |

**Example:**
```yaml
---
- id: "OUTID-DASU1"
  shortDesc: "Output ID for DASU1"
  iasType: "ALARM"
  docUrl: "http://wiki.alma.cl/temp"
- id: "Temperature"
  shortDesc: "Temperature input"
  iasType: "DOUBLE"
- id: "SoundInput"
  iasType: "ALARM"
  sound: "TYPE2"
  canShelve: "true"
  emails: "alerts@example.org"
```

**Referenced by:** `DASU.outputId`, `ASCE.outputID`, `ASCE.inputIDs[]`, `PLUGIN.values[].id`

---

## 6. TF — `CDB/TF/tfs.json`

**Consolidated file (array).** Registry of transfer function classes available to ASCEs.

| Field | Type | Description |
|---|---|---|
| `className` | String | FQCN of the TF implementation class |
| `implLang` | String | `"SCALA"` or `"JAVA"` — routes to the correct classloader |

**Example:**
```json
[
  { "className": "org.eso.ias.asce.transfer.impls.MinMaxThresholdTF", "implLang": "SCALA" },
  { "className": "org.eso.ias.transfer.MinMaxBool", "implLang": "JAVA" }
]
```

**Referenced by:** `ASCE.transferFunctionID`

---

## 7. TEMPLATE — `CDB/TEMPLATE/templates.json`

**Consolidated file (array).** Enables bulk instantiation of DASUs/ASCEs/IASIOs by cloning IDs across a numeric range.

| Field | Type | Description |
|---|---|---|
| `id` | String | Unique template ID |
| `min` | String | Minimum instance number (inclusive) |
| `max` | String | Maximum instance number (inclusive) |

When a SUPERVISOR deploys a DASU with a `templateId` + `instance`, the Supervisor generates concrete IDs suffixed with `[!#n!]` for each instance in the `[min, max]` range.

**Example:**
```json
[
  { "id": "temp-ID1", "min": "1", "max": "5" },
  { "id": "temp-ID2", "min": "0", "max": "10" }
]
```

**Referenced by:** `SUPERVISOR.dasusToDeploy[].templateId`, `DASU.templateId`, `ASCE.templateId`, `ASCE.templatedInputs[].templateId`, `IASIO[].templateId`

---

## 8. PLUGIN — `CDB/PLUGIN/<PluginID>.json`

**One file per plugin.** Defines an external data source that polls or receives instrument data and publishes it to the IAS message bus.

| Field | Type | Description |
|---|---|---|
| `id` | String | Unique plugin ID |
| `monitoredSystemId` | String | Monitored system identifier (e.g. `"ACS"`, `"LCU"`) |
| `props` | Array | `{name, value}` plugin-specific configuration |
| `values` | Array | I/O values produced by this plugin |

Each `values` entry:

| Field | Type | Description |
|---|---|---|
| `id` | String | IASIO ID produced by this value |
| `refreshTime` | String | Polling/refresh interval in milliseconds |
| `filter` | String | Filter applied (e.g. `"Average"`) |
| `filterOptions` | String (optional) | Filter parameters (e.g. `"1, 150, 5"`) |

**Example:**
```yaml
---
id: "PluginIDForTesting"
monitoredSystemId: "ACS"
props:
  - name: "a-key"
    value: "itsValue"
values:
  - id: "AlarmID"
    refreshTime: "500"
    filter: "TheFilter"
    filterOptions: "Options"
```

**Cross-references:** `values[].id` → `IASIO`

---

## 9. CLIENT — `CDB/CLIENT/<ClientId>.conf`

**Extension point.** Reserved for client-specific configuration. Convention is `<ClientId>.conf`, but JSON/YAML files are also supported. Schema is not yet fully defined.

---

## Data Flow

```
PLUGIN  →  produces IASIO values
  ↓
ASCE    →  reads input IASIOs, applies TF, writes output IASIO
  ↓
DASU    →  aggregates ASCE outputs into a single alarm IASIO
  ↓
SUPERVISOR → orchestrates deployment of DASUs on a host
```

## CDB Readers (`CdbReaderFactory`)

Three backends, selected by CLI parameters:

| Priority | Flag | Backend |
|---|---|---|
| 1 | `-cdbClass <class>` | Custom Java `CdbReader` (dynamic instantiation) |
| 2 | `-jCdb <path>` or `-j <path>` | JSON/YAML structured-text reader |
| 3 | (none) | RDB (Hibernate) — default fallback |

## Templating Mechanics

When the Supervisor deploys a templated DASU, it transforms each entity ID by appending `[!#n!]` where `n` is the instance number. For example, a DASU with ID `DasuTemp1` and instance `3` becomes `DasuTemp1[!#3!]`. All child ASCEs, inputs, and outputs follow the same pattern.

Sources should never parse this pattern directly. Use the `Identifier` class (`BasicTypes/src/scala/org/eso/ias/types/Identifier.scala`) which provides methods to extract base name and instance number from templated IDs.

More details in [#80](https://github.com/IntegratedAlarmSystem-Group/ias/issues/80) and [#124](https://github.com/IntegratedAlarmSystem-Group/ias/issues/124).
