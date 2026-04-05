package com.eggyswarehouse.betterglucodash.ui.dashboard.average

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * "Last 24h Average" card — same dimensions and visual style as [CurrentGlucoseCard].
 *
 * Layout (centered, top → bottom):
 *  - Bold title: "Last 24h Average"
 *  - Content based on [AverageState]:
 *    - [AverageState.InsufficientData]: "14/24 hours" in muted text
 *    - [AverageState.IncompleteData]:   sensor gap alert
 *    - [AverageState.Ready]:            animated value + unit + coverage context
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AverageCard(state: AverageState, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape     = MaterialTheme.shapes.extraLarge,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text       = "Last 24h Average",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            AnimatedContent(targetState = state, label = "AvgCardContent") { s ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth()
                ) {
                    when (s) {
                        // ── Calculating ─────────────────────────────────────
                        AverageState.Calculating -> {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color    = MaterialTheme.colorScheme.primary
                            )
                        }

                        // ── Not enough hourly coverage yet ───────────────────
                        is AverageState.InsufficientData -> {
                            Text(
                                text       = "${s.hoursWithData}/24 hours",
                                style      = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                textAlign  = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = "of CGM data collected",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // ── Sensor gap — unreliable ──────────────────────────
                        AverageState.IncompleteData -> {
                            Text(
                                text      = "Sensor gap detected",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = "Check sensor connection",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // ── Ready ────────────────────────────────────────────
                        is AverageState.Ready -> {
                            val animated by animateFloatAsState(
                                targetValue   = s.displayAverage.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness    = Spring.StiffnessMedium
                                ),
                                label = "AvgValueAnim"
                            )
                            val prefix = if (s.isApproximate) "~" else ""
                            Text(
                                text       = "$prefix%.1f".format(animated),
                                style      = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                textAlign  = TextAlign.Center
                            )
                            Text(
                                text      = if (s.isMmol) "mmol/L" else "mg/dL",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (s.isApproximate) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text      = "${s.hoursWithData}/24 hours covered",
                                    style     = MaterialTheme.typography.labelSmall,
                                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
