---
description: Core Android development guidelines and architecture rules for this project.
---

# Google Modern Android Architecture (MAD) & Project Rules

## Communication Protocol
- **Audience:** Senior Backend Engineer (10 YOE). 
- **Directness:** Zero handholding. Omit basic explanations, introductory filler, and boilerplate theory.
- **Responses:** Lead with direct code, architecture diagrams (Mermaid), or concise technical reasoning.
- **Code Generation:** Provide only the blocks that need changing or specifically requested files. Assume the user knows how to integrate them.

## Tech Stack & Dependencies
- **Core Principle:** Use 1st-party Google/Jetpack libraries exclusively unless fundamentally impossible. Avoid 3rd-party overhead.
- **UI:** Jetpack Compose (No XML).
- **Design System:** Material 3 (M3) and Material 3 Expressive. Focus on native dynamic theming, fluid motion, and edge-to-edge layouts.
- **Language:** Kotlin (Targeting JVM 11).
- **Minimum SDK:** 28
- **Target SDK:** 36

## Architecture Strategy (Unidirectional Data Flow)
1. **Layered Architecture:** Strictly separate concerns into UI Layer, Domain Layer (optional but recommended for complex business logic), and Data Layer.
2. **UI Layer (Presentation):** 
   - Use standard MVVM with Android's official `ViewModel`.
   - **State:** Expose a single immutable UI state via `StateFlow`.
   - **Events:** UI sends intents/events to the ViewModel; ViewModel mutates the state.
3. **Data Layer:** 
   - Repositories act as the single source of truth. 
   - Expose data streams using Kotlin `Flow`.
   - Local caching via **Room** or **Proto/Preferences DataStore**.
4. **Concurrency:** Kotlin Coroutines exclusively. No `Thread` or `AsyncTask`. Use `Dispatchers.IO` for disk/network ops.
5. **Dependency Injection:** Use **Hilt** for scoping and dependency management.
6. **Navigation:** Use Jetpack Compose Navigation framework (Type-safe arguments).

## UI/UX, M3 Expressive & Backward Compatibility
- **Edge-to-Edge:** Must draw behind system bars by default (`enableEdgeToEdge()` in Activity). Use Compose `WindowInsets` to pad layout content safely.
- **Material 3 Expressive (Android 16 / Pixel 10 UX):**
  - Emphasize high-contrast tonal colors and fluid Shared Element Transitions (`SharedTransitionLayout`).
  - Rely on bold typography scales and larger rounded corner radiuses (e.g., `RoundedCornerShape(24.dp)` or `28.dp` for major cards).
- **Android 9 (API 28) Fallbacks:**
  - `dynamicColor` requires API 31+. Must provide an ultra-premium static fallback theme using Material Theme Builder tokens.
  - Avoid `RenderEffect` (blur) API calls on API < 31; use graceful degradation for visual effects.
- **Components:** Restrict to official `androidx.compose.material3` APIs exclusively.

## Token Saving Directives
- Assume all standard AndroidX, Compose, and Kotlin standard library imports are present.
- Do not repeat unchanged classes/functions in refactoring diffs.
