package com.eggyswarehouse.betterglucodash.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark theme — OLED-friendly deep navy ──────────────────────────────────────
// Layered elevation: Background (darkest) → Surface → SurfaceVariant (lightest)

val BackgroundDark = Color(0xFF060E1A) // near-black navy — page bg
val SurfaceDark = Color(0xFF0D1B2E) // card surfaces float above bg
val SurfaceVariantDark = Color(0xFF142843) // elevated inner surfaces (graph canvas etc.)

val PrimaryDark = Color(0xFF4FC3F7) // electric cyan-blue — accent & selections
val OnPrimaryDark = Color(0xFF002038) // dark text on primary buttons
val PrimaryContainerDark = Color(0xFF003355) // segmented button active fill
val OnPrimaryContainerDark = Color(0xFFBDE9FF) // text on primary containers

val OnSurfaceDark = Color(0xFFE1EAF6) // primary text — near-white with blue tint
val OnSurfaceVariantDark = Color(0xFF7B96B8) // secondary/muted text, axis labels
val ErrorDark = Color(0xFFF87171) // error states (low glucose too)

// ── Light theme — kept for future toggle, not active in V2 ───────────────────

val BackgroundLight = Color(0xFFF0F5FA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE4EDF6)
val PrimaryLight = Color(0xFF0277BD)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD6EAFF)
val OnPrimaryContainerLight = Color(0xFF003355)
val OnSurfaceLight = Color(0xFF0A1929)
val OnSurfaceVariantLight = Color(0xFF4A6478)
val ErrorLight = Color(0xFFDC2626)

// ── Glucose semantic colours — PRD §2.2 ──────────────────────────────────────
// Abbott MeasurementColor: 1=green(in-range), 2=yellow(slightly high),
//                          3=orange(high), 4=red(low/critical)

val GlucoseGreenDark = Color(0xFF34D399) // Emerald 400 — HSL(160°,64%,52%)  calm, safe
val GlucoseAmberDark = Color(0xFFFBBF24) // Amber 400  — HSL( 43°,96%,56%)  attention
val GlucoseOrangeDark = Color(0xFFF97316) // Orange 500 — HSL( 24°,95%,53%)  elevated (Libre orange)
val GlucoseRedDark = Color(0xFFEF4444) // Red 500    — HSL(  0°,84%,60%)  critical / low

val GlucoseGreenLight = Color(0xFF059669) // Emerald 600 — darker for light-bg contrast
val GlucoseAmberLight = Color(0xFFD97706) // Amber 600
val GlucoseOrangeLight = Color(0xFFEA580C) // Orange 600
val GlucoseRedLight = Color(0xFFDC2626) // Red 600
