package com.eggyswarehouse.betterglucodash.ui.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eggyswarehouse.betterglucodash.ui.theme.glucoseColors
import kotlinx.coroutines.delay

// TODO(V3): track when these M3 Expressive APIs stabilize
/**
 * "Current Glucose" hero card at the top of the Dashboard.
 *
 * Layout (all centered, top → bottom):
 *  - Card title: "Current Glucose"
 *  - Time label: "Just now" / "Updated X min ago"
 *  - Large number + trend arrow, unit stacked below arrow
 *  - Combined status + trend: "High Glucose  ·  Steady"
 *
 * Same [surfaceVariant] container colour as all other dashboard cards for
 * visual consistency — the vivid status-coloured arrow and text provide
 * the glucose-status cue without needing a background tint or accent stripe.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CurrentGlucoseCard(state: DashboardUiState, modifier: Modifier = Modifier) {
    val glucoseColors = MaterialTheme.glucoseColors

    val statusColor: Color =
        when (state.glucoseColor) {
            2 -> glucoseColors.slightlyHigh
            3 -> glucoseColors.high
            4 -> glucoseColors.low
            else -> glucoseColors.inRange
        }

    // "High Glucose · Steady" — status only when outside range, trend always shown
    val statusLabel: String? =
        when (state.glucoseColor) {
            2 -> "Slightly Elevated"
            3 -> "High Glucose"
            4 -> "Low Glucose"
            else -> null
        }
    val statusAndTrend =
        buildString {
            if (statusLabel != null) {
                append(statusLabel)
                append("  ·  ")
            }
            append(trendDescription(state.trendCode))
        }

    // Minute-ticking time label
    val minutesAgo by produceState(initialValue = 0L, key1 = state.lastReadingMs) {
        while (true) {
            value =
                if (state.lastReadingMs == 0L) {
                    0L
                } else {
                    ((System.currentTimeMillis() - state.lastReadingMs) / 60_000L).coerceAtLeast(0)
                }
            delay(60_000L)
        }
    }
    val timeLabel =
        when {
            state.isLoading -> ""
            state.lastReadingMs == 0L -> ""
            minutesAgo < 1L -> "Just now"
            minutesAgo == 1L -> "Updated 1 min ago"
            else -> "Updated $minutesAgo min ago"
        }

    Card(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(180.dp),
        shape = MaterialTheme.shapes.extraLarge,
        // Same containerColor as every other dashboard card — consistent surface
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Card title ────────────────────────────────────────────────────
            Text(
                text = "Current Glucose",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            if (state.isLoading) {
                LoadingHero()
            } else {
                // ── Time label — above the number ─────────────────────────────
                if (timeLabel.isNotEmpty()) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // ── Number (left) + [arrow / unit] (right) ───────────────────
                val numericValue = state.currentGlucose.toFloatOrNull() ?: 0f
                val animatedValue by animateFloatAsState(
                    targetValue = numericValue,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "HeroGlucoseAnim"
                )
                val displayStr =
                    when {
                        numericValue == 0f -> state.currentGlucose
                        state.unit == "mmol/L" -> "%.1f".format(animatedValue)
                        else -> animatedValue.toInt().toString()
                    }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayStr,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    // Arrow stacked above unit — unit sits directly beneath arrow
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = state.trendArrow,
                            style = MaterialTheme.typography.headlineSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ── "High Glucose  ·  Steady" (or just "Steady" when in-range) ──
                Text(
                    text = statusAndTrend,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = if (statusLabel !=
                        null
                    ) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Pulsing loading state while the first API response is in-flight. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingHero() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LoadingIndicator(modifier = Modifier.size(24.dp))
        Text(
            "Fetching reading…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Maps the Abbott TrendArrow integer to the Dexcom/ADA industry-standard term.
 *
 * Reference: Dexcom G7 labelling + ADA CGM consensus 2023.
 */
private fun trendDescription(code: Int): String = when (code) {
    1 -> "Falling Rapidly"
    2 -> "Falling"
    3 -> "Steady"
    4 -> "Rising"
    5 -> "Rising Rapidly"
    else -> ""
}
