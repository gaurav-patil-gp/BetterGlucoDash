package com.eggyswarehouse.betterglucodash.ui.theme

import androidx.compose.material3.Typography

/**
 * App typography using the system font (Roboto on Pixel/AOSP).
 *
 * We intentionally use [Typography] defaults rather than loading a custom network font
 * (Inter via the Google Fonts SDK). This eliminates a network round-trip on first launch,
 * reduces APK size, and matches the native Pixel aesthetic the user expects.
 *
 * All M3 type-scale sizes and weights come from the Material 3 specification and are
 * correct out-of-the-box from the default [Typography] constructor.
 */
val Typography = Typography()
