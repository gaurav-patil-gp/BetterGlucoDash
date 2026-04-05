package com.eggyswarehouse.betterglucodash.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GlucoseColors(
    val inRange: Color,
    val slightlyHigh: Color,
    val high: Color,
    val low: Color
)

val LocalGlucoseColors = staticCompositionLocalOf {
    GlucoseColors(
        inRange = Color.Unspecified,
        slightlyHigh = Color.Unspecified,
        high = Color.Unspecified,
        low = Color.Unspecified
    )
}
