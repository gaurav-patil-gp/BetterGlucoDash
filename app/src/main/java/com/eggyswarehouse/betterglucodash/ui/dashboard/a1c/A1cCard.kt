package com.eggyswarehouse.betterglucodash.ui.dashboard.a1c

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
 * "Estimated A1C" card — same dimensions and visual style as the other dashboard stat cards.
 *
 * Displays an estimated HbA1c calculated via the ADAG formula
 * (Nathan et al. 2008, Diabetes Care) from Room CGM data.
 *
 * Layout (centered, top → bottom):
 *  - Bold title: "Estimated A1C"
 *  - [A1cState.InsufficientData]: "45/60 days" data progress
 *  - [A1cState.Ready]:            animated "%", formula note, data coverage
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun A1cCard(state: A1cState, modifier: Modifier = Modifier) {
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
                text       = "Estimated A1C",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            AnimatedContent(targetState = state, label = "A1cCardContent") { s ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth()
                ) {
                    when (s) {
                        // ── Calculating ─────────────────────────────────────────
                        A1cState.Calculating -> {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color    = MaterialTheme.colorScheme.primary
                            )
                        }

                        // ── Not enough days yet ──────────────────────────────────
                        is A1cState.InsufficientData -> {
                            Text(
                                text       = "${s.daysWithData}/90 days",
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
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = "90 days needed for ADAG estimate",
                                style     = MaterialTheme.typography.labelSmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center
                            )
                        }

                        // ── Ready ─────────────────────────────────────────────────
                        is A1cState.Ready -> {
                            val animated by animateFloatAsState(
                                targetValue   = s.a1cPercent.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness    = Spring.StiffnessMedium
                                ),
                                label = "A1cValueAnim"
                            )
                            val prefix = if (s.isApproximate) "~" else ""
                            Text(
                                text       = "$prefix%.1f%%".format(animated),
                                style      = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                textAlign  = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = "eA1C (ADAG formula)",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text      = "${s.daysWithData} days of data",
                                style     = MaterialTheme.typography.labelSmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
