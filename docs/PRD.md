# BetterGlucoDash: Product Requirements Document (PRD)

## 1. Product Overview
BetterGlucoDash is an advanced, non-medical Android application designed to provide superior analytics and data visualization for Continuous Glucose Monitor (CGM) users (specifically targeting FreeStyle Libre 3 and Dexcom G7 users, starting with Libre 3). 
Unlike the native 1st-party apps that provide basic charts, BetterGlucoDash consumes data via reverse-engineered sharing APIs (e.g., LibreLinkUp) to deliver rich, customizable insights (e.g., Dawn Phenomenon detection, HbA1c estimation) completely processed directly on the user's device.

**Key Tenet:** All analytics and math computations happen *locally* on the device. Data is pulled down but never sent back up or given to third-party servers.

## 2. Target Audience
- Users located in the **US and Canada** with a FreeStyle Libre 3 (MVP) or Dexcom G7 (Future). 
- Users dissatisfied with the limited trend reporting in native apps.
- Users who want customized widgets on their Android home screens.

## 3. Disclaimers & Regulatory (Critical)
- **NOT FOR MEDICAL DECISIONS:** The app is strictly for supplemental analytics. It will not instruct users on insulin dosing.
- **Data Source:** Pulls data from LibreLinkUp APIs. It acts merely as a data consumer.
- Prominent disclaimers must exist on the Login screen and Dashboard.

---

## 4. MVP (Version 1) Scope
*Goal: Successfully authenticate, retrieve, parse, and prominently display the current glucose data on a modern, Material 3 Expressive UI.*

### 4.1 Feature Set
1. **Authentication Flow:**
   - A single login screen requiring LibreLinkUp Email and Password.
   - Saves credentials/tokens locally using `EncryptedSharedPreferences` or `DataStore` (Preferences).
2. **Data Sync Engine:**
   - Background/Foreground capability to hit the LibreLinkUp API:
     - `POST /llu/auth/login` (Get Token)
     - `GET /llu/connections` (Map patient ID)
     - `GET /llu/connections/{patientId}/graph` (Get glucose metrics)
   - **Regional Scope:** Strictly limited to US and Canada. EU and other global endpoints are out of scope.
   - **Units:** Units are **locked to region at login** — Abbott's API pre-converts the `Value` field to the regional unit. No manual conversion is performed in the app.
     - Canada (`api-ca.libreview.io`): `Value` is delivered in `mmol/L`. App displays mmol/L only.
     - US (`api-us.libreview.io`): `Value` is delivered in `mg/dL`. App displays mg/dL only.
3. **Core Dashboard:**
   - Displays the **Current Glucose Value** prominently, formatted correctly for the selected region's unit type.
   - Trend Arrow (Rising, Falling, Stable).
   - "Last Updated" timestamp.
4. **Local processing only:**
   - No external backend setup or Firebase auth. Zero remote logging of user health data.

### 4.2 Excluded from MVP (V2+)
- Advanced Analytics (Dawn Phenomenon, HbA1c calculators).
- Data visualizer charts (7/14/30/90 day historical graphs).
- Android Home Screen Widgets.
- **Dynamic unit switching** (e.g. toggling between mmol/L and mg/dL mid-session). Units are locked to the region selected at login. Manual conversion logic (`1 mmol/L = 18.018 mg/dL`) is deferred to V2.

---

## 5. Technical Requirements

### 5.1 Tech Stack (Per `android_rules.md`)
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3 Expressive, Edge-to-Edge)
- **Architecture:** MVVM + UDF (Unidirectional Data Flow)
- **State Management:** `StateFlow` + `ViewModel`
- **Network:** Retrofit + Kotlinx Serialization (Standard for API ingestion)
- **Dependency Injection:** Hilt
- **Storage:** Proto DataStore or Encrypted SharedPreferences for JWT/Credentials.

### 5.2 API Mechanics (LibreLinkUp)
The MVP requires interacting with the unofficial LibreLinkUp API. Key fields from the `/graph` response:
- `Value`: Pre-converted glucose reading in the **user's regional unit** (mmol/L for CA, mg/dL for US). This is a `Double`.
- `ValueInMgPerDl`: Raw mg/dL integer reading from the sensor. Used for internal reference only in MVP.
- `TrendArrow`: Integer representing direction (1=falling fast, 2=falling, 3=flat, 4=rising, 5=rising fast).
- `Timestamp`: Local time of the reading.
- `MeasurementColor`: Color indicator (1=in range, 2=slightly high, 3=high, 4=low) — used for future UI color coding.

## 6. Success Metrics for MVP
1. Can the user login successfully?
2. Is the current glucose reading identical to the 1st party Libre app at the time of refresh?
3. Does the UI adhere strictly to Google's modern M3 guidelines?
