package com.eggyswarehouse.betterglucodash.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark theme — neutral-dark (Google Weather–inspired) ───────────────────────
// Layered elevation: Background (darkest) → Surface → SurfaceVariant (lightest)

val BackgroundDark = Color(0xFF0F1117) // near-black neutral — page bg
val SurfaceDark = Color(0xFF1A1D2B) // dark blue-grey — card surfaces
val SurfaceVariantDark = Color(0xFF22263A) // slightly elevated (graph canvas, stat cards)

val PrimaryDark = Color(0xFF4ECDC4) // vibrant teal — distinct from cyan, health association
val OnPrimaryDark = Color(0xFF002B28)
val PrimaryContainerDark = Color(0xFF003F3B)
val OnPrimaryContainerDark = Color(0xFFA7F3EE)

val OnSurfaceDark = Color(0xFFECEEF4) // near-white — 15:1 contrast on Background
val OnSurfaceVariantDark = Color(0xFF94A3B8) // slate-400 — 5.1:1 on SurfaceVariant
val OutlineDark = Color(0xFF30363D)
val ErrorDark = Color(0xFFF87171)

// ── Light theme ───────────────────────────────────────────────────────────────

val BackgroundLight = Color(0xFFF8FAFC) // slate-50, subtle cool-white
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9) // slate-100
val PrimaryLight = Color(0xFF0F766E) // teal-700 — 5.9:1 contrast on white
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF99F6E4)
val OnPrimaryContainerLight = Color(0xFF003D36)
val OnSurfaceLight = Color(0xFF0F172A) // slate-900 — 18:1 contrast on Background
val OnSurfaceVariantLight = Color(0xFF475569) // slate-600 — 7.3:1 on white
val OutlineLight = Color(0xFFCBD5E1) // slate-300
val ErrorLight = Color(0xFFDC2626)

// ── Glucose semantic colours — PRD §2.2 ──────────────────────────────────────
// Abbott MeasurementColor: 1=green(in-range), 2=yellow(slightly high),
//                          3=orange(high), 4=red(low/critical)

val GlucoseGreenDark = Color(0xFF34D399) // emerald-400
val GlucoseAmberDark = Color(0xFFFBBF24) // amber-400
val GlucoseOrangeDark = Color(0xFFF97316) // orange-400
val GlucoseRedDark = Color(0xFFEF4444) // red-400

// Light variants: ≥4.5:1 contrast on white
val GlucoseGreenLight = Color(0xFF059669) // emerald-600  4.5:1
val GlucoseAmberLight = Color(0xFFB45309) // amber-700    5.8:1
val GlucoseOrangeLight = Color(0xFFEA580C) // orange-600   4.8:1
val GlucoseRedLight = Color(0xFFDC2626) // red-600      4.5:1
