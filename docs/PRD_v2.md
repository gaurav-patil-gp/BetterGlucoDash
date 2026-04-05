# BetterGlucoDash: Product Requirements Document — Version 2 (MVP)

## 1. Overview

V2 elevates BetterGlucoDash from a raw data display (V1) into a **visually premium analytics experience**. The theme is Motion + Insight — every screen should feel alive, and every widget should surface meaningful context at a glance.

Two non-negotiable foundations power V2:
1. **Material 3 Expressive** — a complete design system replacement from V1's placeholder theme
2. **Local persistence via Room** — required to deliver 24h analytics since the API only returns ~8h of data per poll

**Design mandate:** All UI must adhere to Material 3 Expressive. Interactions that reference Libre or Dexcom apps are functional inspirations only — visual design is BetterGlucoDash's own.

---

## 2. Material 3 Expressive — Design System Overhaul

> **Status in V1:** The current theme uses the default Gemini-generated stub (Purple80/PurpleGrey/Pink, `FontFamily.Default`, stock `CircularProgressIndicator`). This does NOT meet M3 Expressive standards and must be replaced in V2 as a prerequisite before building new features.

### 2.1 Required M3 Expressive Upgrades

| Component | V1 (existing) | V2 (required) |
|---|---|---|
| Loading (full screen) | `CircularProgressIndicator()` | `LoadingIndicator()` — shape-morphing blob |
| Loading (inline/button) | `CircularProgressIndicator(size=24)` | `CircularWavyProgressIndicator` |
| Linear progress | Not used | `LinearWavyProgressIndicator` — squiggly wavy line |
| Typography | `FontFamily.Default` | **Inter** via downloadable Google Fonts |
| Color palette | Purple stub | Custom glucose-themed palette (see §2.2) |
| Shape system | Ad-hoc `RoundedCornerShape` | M3 `ShapeDefaults` tokens |
| Motion | None | Spring physics on all entry/transition animations |
| Filter chips (time range) | Not used | M3 Segmented Button with spring-animated selection |

### 2.2 Glucose-Themed Color Palette

The color language should evoke clinical precision meets modern wellness — deep navy/slate background with a glucose-aware semantic accent system:

| Token | Light | Dark | Purpose |
|---|---|---|---|
| `Primary` | `#0077CC` | `#66B5FF` | Interactive elements, selected states |
| `PrimaryContainer` | `#D6EAFF` | `#004A8F` | Card backgrounds |
| `GlucoseGreen` | `#22C55E` | `#4ADE80` | In-range readings (MeasurementColor=1) |
| `GlucoseYellow` | `#EAB308` | `#FACC15` | Slightly high (MeasurementColor=2) |
| `GlucoseOrange` | `#F97316` | `#FB923C` | High (MeasurementColor=3) |
| `GlucoseRed` | `#EF4444` | `#F87171` | Low (MeasurementColor=4) |
| `Background` | `#F8FAFC` | `#0F172A` | Screen background (dark = deep navy) |
| `Surface` | `#FFFFFF` | `#1E293B` | Card surfaces |

> **Note:** `dynamicColor` will be set to `false` so the glucose semantic palette always renders correctly regardless of the user's wallpaper.

### 2.3 Animation & Physics Requirements

All animations must use **spring physics** (not linear/ease curves):
- **Entry animations:** Cards slide up + fade in with `spring(dampingRatio=0.8, stiffness=300)`
- **Filter chip selection:** Scale bounce on tap (`spring(stiffness=400, dampingRatio=0.6)`)
- **Graph crosshair:** Smooth drag with `animateFloatAsState(spring(stiffness=200))`
- **Glucose number updates:** `animateFloatAsState` when value changes between polls
- **Card appearance:** `AnimatedVisibility` with `slideInVertically + fadeIn`

---

## 3. Local Data Persistence — Room Database

### 3.1 Why Room Is Required in V2

The Abbott LibreLinkUp API's `/graph` endpoint returns **only ~8 hours** of historical data per response. Without local persistence:

| Feature | Without DB | With DB |
|---|---|---|
| 3h / 6h graph | ✅ | ✅ |
| 12h graph | ⚠️ Partial | ✅ |
| 24h graph | ❌ Impossible | ✅ |
| **24h average card** | **❌ Impossible** | **✅** |
| Future: HbA1c, TIR, 7/14/30/90d | ❌ | ✅ |

Room is the correct choice: it is SQLite-backed (extremely fast on-device), natively supports Kotlin `Flow`, works on API 28+, and its annotation processor (KSP) integrates cleanly with the existing build setup.

### 3.2 Database Schema

**Single table for MVP:**
```
glucose_readings
├── timestamp_utc     INTEGER PRIMARY KEY   ← epoch millis from FactoryTimestamp (sensor UTC)
├── value_mgdl        INTEGER               ← raw ValueInMgPerDl — always stored as mg/dL
├── value_display     REAL                  ← Abbott's pre-converted regional value (mmol/L or mg/dL)
├── trend_arrow       INTEGER               ← 1-5
├── measurement_color INTEGER               ← 1=green, 2=yellow, 3=orange, 4=red
└── region            TEXT                  ← "CA" or "US"

Index: timestamp_utc (for fast time-range queries)
```

**Why `timestamp_utc` as primary key?** The sensor produces exactly one reading per 5 minutes. Using the UTC sensor timestamp as the natural primary key prevents duplicates automatically — inserting the same reading twice (e.g. on back-to-back app opens) is a safe `REPLACE` no-op.

### 3.3 Data Retention Policy

- Keep **90 days** of readings (max ~26,000 rows — negligible storage)
- Prune readings older than 90 days on each successful poll
- 90 days chosen to support future HbA1c estimates (which require 90-day averages)

### 3.4 Write Strategy

On each 5-minute poll, `LibreRepository` inserts the `graphData[]` array (all ~8h of readings) using `INSERT OR REPLACE`. This:
- Naturally handles overlapping windows between polls with no duplicates
- Automatically fills in up to 8h of gap if the app was offline

### 3.5 Interim Backup (V2)

Android's built-in **Auto Backup** will be enabled to back up the Room database file to the user's Google account automatically — zero code required. This provides basic device-transfer recovery (similar to WhatsApp's auto-backup).

**V3 will implement** explicit Google Drive backup with a user-initiated export/import flow for full control.

---

## 4. V2 Feature Scope

### Widget 1: Continuous Glucose Graph (Primary / Full Width)

**Purpose:** A time-series line chart giving full visibility into the user's glucose trend over a selectable window.

#### 4.1 Time Range Filter
- **M3 `SingleChoiceSegmentedButtonRow`** at the top of the card
- Options: **3h | 6h | 12h | 24h**
- Default: **3h**
- Filter queries Room DB for readings within the selected window — no API call
- 12h and 24h views work once sufficient data has been accumulated in Room

#### 4.2 Graph Rendering
- **Custom Jetpack Compose `Canvas` renderer** — no third-party charting library
- Smooth **cubic bezier curve** through data points (not hard polyline)
- Line color maps to the most recent reading's `MeasurementColor`; transitions smoothly on change
- Target band (3.9–10.0 mmol/L / 70–180 mg/dL) rendered as a subtle translucent filled zone
- Y-axis auto-scales to the visible data range; X-axis labeled at sensible intervals
- Subtle dashed gridlines at target low/high thresholds
- **Entry animation:** Line draws in left-to-right using `PathMeasure` + animated path fraction

#### 4.3 Interactive Crosshair (Scrub Line)
- Tap anywhere on the graph → vertical dashed crosshair appears, snapping to the nearest data point
- Floating tooltip shows: glucose value, color indicator dot, timestamp
- Drag gesture moves the crosshair with spring damping
- Tap outside or tap again → dismisses with fade-out
- Implementation: `pointerInput { detectTapGestures + detectHorizontalDragGestures }` on Canvas

#### 4.4 Code Organization
```
ui/dashboard/graph/
  GlucoseGraphCard.kt        ← Composable (UI only)
  GlucoseGraphViewModel.kt   ← Selected range, crosshair, Room query results
  GlucoseGraphState.kt       ← TimeRange enum, GraphPoint, sealed UiState
  GlucoseGraphRenderer.kt    ← Canvas DrawScope extensions (testable)
```

---

### Widget 2: 24-Hour Average Card (Secondary / Square, Half-Width)

**Purpose:** A single meaningful number representing glycaemic control over the past day.

#### 4.5 Layout
- Square card, ~half screen width; right half reserved for V3 widgets
- Sits below the graph card in a `LazyColumn`

#### 4.6 Average Calculation Rules

| Condition | Behaviour |
|---|---|
| < 24h of data in Room | "24h data not yet available" + `LinearWavyProgressIndicator` showing fill progress |
| ≥ 24h available, ≤ 1h total gap | Report average normally |
| ≥ 24h available, > 1h gap but ≥ 23h data | Report average with "~" prefix + footnote |
| > 1h gap and < 23h data remaining | "Incomplete data — check sensor connection" |

- Average computed from `value_mgdl` (raw integers, consistent across regions)
- Display in regional unit after averaging
- MVP: average only. HbA1c estimation deferred to V3.

#### 4.7 Code Organization
```
ui/dashboard/average/
  AverageCard.kt           ← Composable (UI only)
  AverageViewModel.kt      ← Collects Room flow, drives AverageCalculator
  AverageCalculator.kt     ← Pure Kotlin object, fully unit-testable, no Android deps
  AverageState.kt          ← Sealed: Calculating / InsufficientData / Ready / Incomplete
```

---

## 5. Data Architecture (V2)

```
LibreLinkUp API  ──poll every 5min──▶  LibreRepository
                                              │
                            ┌─────────────────┴──────────────────┐
                            ▼                                     ▼
                     Room Database                         GlucoseFlowState
                   (glucose_readings)                   (current reading UI)
                            │
              ┌─────────────┴──────────────┐
              ▼                            ▼
    GlucoseGraphViewModel         AverageViewModel
    (queries by time range)       (queries last 24h)
```

---

## 6. Dashboard Layout (V2)

```
┌─────────────────────────────────────────┐
│  TopAppBar: "BetterGlucoDash"  [Logout] │
├─────────────────────────────────────────┤
│                                         │
│   [3h] [6h] [12h] [24h]  ← segmented   │
│  ┌───────────────────────────────────┐  │
│  │  Glucose Graph (Canvas, full width)│ │
│  │  ── target band ──────────────── │  │
│  │     crosshair tooltip             │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ┌────────────┐  ┌────────────────────┐ │
│  │  24h Avg   │  │  (V3 widget slot)  │ │
│  │   10.2     │  │                    │ │
│  │  mmol/L    │  │                    │ │
│  └────────────┘  └────────────────────┘ │
│                                         │
│  ⚠️ Not for medical decisions.           │
└─────────────────────────────────────────┘
```

---

## 7. Excluded from V2

- HbA1c / eA1c estimation (needs 90d accumulation — data layer ready, computation V3)
- Time-in-range (TIR) percentage
- 7/14/30/90 day graph views (data accumulates in Room; views added in V3)
- Google Drive manual backup/restore (Auto Backup covers interim recovery)
- Android home screen widgets
- Manual unit switching (still locked to region at login)
- Dexcom G7 support

---

## 8. Success Metrics for V2

1. **Graph loads in < 300ms** from the time data is available in the ViewModel
2. **Crosshair drag latency < 1 frame** (16ms) — feels physical
3. **24h average is arithmetically exact** — matches manual calculation from raw DB values
4. **Room writes are non-blocking** — UI never hiccups during DB insert
5. **M3 Expressive compliance** — `LoadingIndicator`, spring animations, correct glucose palette in all states
6. **Zero new API calls** — all V2 features powered by the existing `/graph` poll
7. **Auto Backup enabled** — Room DB backed up to user's Google account transparently
