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

// TODO(V3): track when these M3 Expressive APIs stabilize
/**
 * "Estimated A1C" card — same dimensions and visual style as the other dashboard stat cards.
 *
 * Displays an estimated HbA1c calculated via the ADAG formula
 * (Nathan et al. 2008, Diabetes Care) from Room CGM data.
 *
 * Layout (centered, top → bottom):
 *  - Bold title: "Estimated A1C"
 *  - [A1cState.InsufficientData]: "X/90 days" data progress
 *  - [A1cState.Ready]:            animated "%", formula note, data coverage
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun A1cCard(state: A1cState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(148.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ── Title — pinned to top so both cards share the same title baseline ─
            Text(
                text = "Est. A1C",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Value content centered in the remaining height
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                AnimatedContent(targetState = state, label = "A1cCardContent") { s ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(Modifier.height(10.dp))
                        when (s) {
                            // ── Calculating ─────────────────────────────────────────
                            A1cState.Calculating -> {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // ── Not enough days yet ──────────────────────────────────
                            is A1cState.InsufficientData -> {
                                Text(
                                    text = "${s.daysWithData} / ${A1cCalculator.TARGET_DAYS}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "days of data",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${A1cCalculator.APPROXIMATE_MIN_DAYS} days min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.50f
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // ── Ready ─────────────────────────────────────────────────
                            is A1cState.Ready -> {
                                val animated by animateFloatAsState(
                                    targetValue = s.a1cPercent.toFloat(),
                                    animationSpec =
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "A1cValueAnim"
                                )
                                val prefix = if (s.isApproximate) "~" else ""
                                Text(
                                    text = "$prefix%.1f%%".format(animated),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "ADAG formula",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${s.daysWithData} days of data",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.55f
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } // Box
        }
    }
}
