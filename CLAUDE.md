# BetterGlucoDash — Claude Code Rules

## Communication
- Audience: Senior engineer. Zero handholding.
- Lead with code or architecture diagrams (Mermaid). Skip theory.
- Provide only changed blocks, not full file reprints.
- Do not repeat unchanged classes/functions in refactoring diffs.

## Token Efficiency
- Assume all standard AndroidX, Compose, and Kotlin stdlib imports are present. Never emit import blocks unless adding a non-obvious 3rd-party dep.
- Omit `package` declarations in snippets unless the package is relevant to the change.
- Skip boilerplate `@Composable fun Preview` stubs unless explicitly asked.
- State-sealed-class structure is known — don't re-explain `Loading/Success/Error` pattern on every response.

## Project Overview
Privacy-first CGM analytics app. Reads FreeStyle Libre 3 data via the LibreLinkUp API (reverse-engineered, unofficial). No data ever leaves the device except to Abbott's own endpoints.

- Package: `com.eggyswarehouse.betterglucodash`
- App class: `GlucoDashApplication` — holds `AppContainer`

## Tech Stack
| Layer | Tech |
|---|---|
| Language | Kotlin, JVM 17 |
| UI | Jetpack Compose (no XML) |
| Design | Material 3 + M3 Expressive |
| Local DB | Room + KSP |
| Prefs | DataStore (Preferences) |
| Network | Retrofit + OkHttp + kotlinx.serialization |
| Async | Coroutines + Flow |
| DI | Manual `AppContainer` (NOT Hilt — intentional) |
| Nav | Compose Navigation (type-safe) |
| Min SDK | 28 / Target SDK 36 |

## Architecture
MVVM + UDF. One `StateFlow<UiState>` per screen. Sealed classes for async states (Loading / Success / Error).

```
UI Layer       →  ViewModel  →  Repository  →  Room / Retrofit
(Compose)         StateFlow     single SSOT      Dispatcher.IO
```

**No Hilt.** DI is manual via `AppContainer` (service-locator pattern). All deps are `by lazy` singletons scoped to the Application.

## Key Files
```
app/src/main/java/com/eggyswarehouse/betterglucodash/
├── GlucoDashApplication.kt          # App entry, creates AppContainer
├── MainActivity.kt                  # enableEdgeToEdge, NavHost root
├── di/AppContainer.kt               # All dependency wiring
├── data/
│   ├── network/
│   │   ├── LibreApiService.kt       # Retrofit interface
│   │   ├── LibreModels.kt           # @Serializable request/response models
│   │   ├── AuthInterceptor.kt       # Injects Bearer + account-id headers
│   │   ├── RegionInterceptor.kt     # Rewrites OkHttp host per-request
│   │   └── BaseUrlHolder.kt        # Mutable regional host (US/CA/EU/etc.)
│   ├── local/
│   │   ├── AuthManager.kt           # DataStore: token, accountId, patientId
│   │   └── db/
│   │       ├── GlucoseDatabase.kt
│   │       ├── GlucoseDao.kt
│   │       └── GlucoseReadingEntity.kt
│   └── repository/
│       ├── LibreRepository.kt       # Single source of truth
│       └── GlucoseFlowState.kt      # Sealed async state
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   └── AuthViewModel.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   ├── DashboardViewModel.kt
│   │   ├── CurrentGlucoseCard.kt
│   │   ├── graph/
│   │   │   ├── GlucoseGraphCard.kt
│   │   │   ├── GlucoseGraphViewModel.kt
│   │   │   ├── GlucoseGraphRenderer.kt  # Stateless DrawScope extension
│   │   │   └── GlucoseGraphState.kt
│   │   ├── a1c/
│   │   │   ├── A1cCard.kt
│   │   │   ├── A1cViewModel.kt
│   │   │   ├── A1cCalculator.kt         # Pure object, no Android deps
│   │   │   └── A1cState.kt
│   │   └── average/
│   │       ├── AverageCard.kt
│   │       ├── AverageViewModel.kt
│   │       ├── AverageCalculator.kt     # Pure object
│   │       └── AverageState.kt
│   ├── navigation/Screen.kt
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       ├── Type.kt
│       └── GlucoseColors.kt            # Semantic glucose range colors
```

## UI/UX Mandates
- Edge-to-edge always (`enableEdgeToEdge()` + `WindowInsets` padding)
- M3 Expressive: bold typography, `RoundedCornerShape(24.dp)`–`28.dp` for cards
- Spring physics animations (not tween)
- Dark-first OLED palette
- `dynamicColor` requires API 31+; static fallback theme for API 28–30
- Glucose semantic colors: `GlucoseColors.kt` — never hardcode glucose range colors inline

## Network / Regional Routing
- Abbott's API has regional endpoints (US, CA, EU, AP, AU, AE, JP, DE, FR, SE, FI)
- `BaseUrlHolder` holds the mutable host string
- `RegionInterceptor` rewrites the OkHttp request host at runtime — must be first interceptor in chain
- `AuthInterceptor` adds `Authorization: Bearer <token>` and `account-id` headers
- Login flow: try US → if `redirect=true`, retry with `data.region` → persist correct host
- HTTP body logging is **debug-only** (`BuildConfig.DEBUG`) to keep JWTs out of release logcat

## Data Conventions
- Room: `INSERT OR REPLACE` on PK (upsert pattern — no separate update queries)
- Calculators (`A1cCalculator`, `AverageCalculator`) are pure `object`s — no Android framework deps
- `GlucoseGraphRenderer` is a stateless `DrawScope` extension function
- `GlucoseMeasurement.Value` is already in the user's regional unit (mmol/L or mg/dL) — Abbott pre-converts server-side. No client conversion needed.

## Build Commands
```bash
# Verify build (no simulator needed)
./gradlew :app:assembleDebug

# Run on simulator → use Android Studio
```

## Current Status (V2 — Implemented)
- Login with regional auto-redirect
- Dashboard: current glucose reading + trend arrow
- 8-hour glucose graph (canvas renderer)
- 24-hour average glucose card
- Estimated A1c card (ADA formula, benchmarked ranges)

## V3 Future Scope
- Time-in-range (TIR) visualization
- Multi-patient support
- Notification/widget for current reading
- Historical trends beyond 8 hours
