package com.eggyswarehouse.betterglucodash.ui.dashboard.graph

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggyswarehouse.betterglucodash.ui.theme.glucoseColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── FreeStyle Libre sensor range constants ────────────────────────────────────
// TODO(V3): move to a shared GlucoseConstants.kt when additional screens need these.

/** Maximum value the FreeStyle Libre sensor reports in mmol/L. */
private const val LIBRE_MAX_MMOL = 27.8

/** Minimum sensor value in mmol/L (sensor-low). */
private const val LIBRE_MIN_MMOL = 2.2

/** In-range lower bound in mmol/L (PRD §4.2). */
private const val TARGET_LOW_MMOL = 3.9

/** In-range upper bound in mmol/L (PRD §4.2). */
private const val TARGET_HIGH_MMOL = 10.0

private const val LIBRE_MAX_MGDL = 400.0
private const val LIBRE_MIN_MGDL = 40.0
private const val TARGET_LOW_MGDL = 70.0
private const val TARGET_HIGH_MGDL = 180.0

/** Left-margin pixels reserved for Y-axis labels. */
private const val LABEL_MARGIN_PX = 38f

/** Bottom padding so the curve never clips the very bottom edge. */
private const val BOTTOM_PAD_PX = 12f

// TODO(V3): track when these M3 Expressive APIs stabilize
/**
 * Full-width card hosting the interactive, scrollable glucose trend graph (PRD §4.1–4.3).
 *
 * - Time-range is selected via [SingleChoiceSegmentedButtonRow].
 * - The chart canvas is horizontally scrollable; tap to inspect a point (crosshair),
 *   swipe left/right to pan through history.
 * - The tooltip is drawn directly on-canvas to scroll correctly with the chart.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlucoseGraphCard(
    state: GlucoseGraphUiState,
    onRangeSelected: (TimeRange) -> Unit,
    onCrosshairMoved: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedRange =
        when (state) {
            is GlucoseGraphUiState.Ready -> state.selectedRange
            is GlucoseGraphUiState.Loading -> TimeRange.ThreeHour
        }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = "Glucose Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // ── Time-range segmented buttons ──────────────────────────────────
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TimeRange.values.forEachIndexed { index, range ->
                    SegmentedButton(
                        selected = range == selectedRange,
                        onClick = { onRangeSelected(range) },
                        shape = SegmentedButtonDefaults.itemShape(index, TimeRange.values.size),
                        colors =
                        SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(range.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Chart area (dark-surface inner box) ───────────────────────────
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AnimatedContent(targetState = state, label = "GraphStateTransition") { target ->
                    when (target) {
                        is GlucoseGraphUiState.Loading -> GraphLoadingContent()
                        is GlucoseGraphUiState.Ready ->
                            ChartCanvas(
                                points = target.points,
                                crosshairIndex = target.crosshairIndex,
                                onCrosshairMoved = onCrosshairMoved,
                                animateEntry = target.animateEntry
                            )
                    }
                }
            }

            // ── Hint text ─────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap a point to inspect • Swipe to pan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/** Pulsing loading state shown while Room data first arrives. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GraphLoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LoadingIndicator(modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Loading readings…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Interactive, horizontally scrollable glucose chart.
 *
 * Architecture:
 *  1. [BoxWithConstraints] measures the visible width.
 *  2. Virtual canvas width = max(screen, points × 8dp) so data is spread naturally.
 *  3. [horizontalScroll] pans the canvas; swiping does NOT conflict with the crosshair
 *     because the crosshair is set only on tap, not drag.
 *  4. The crosshair tooltip is drawn directly on-canvas via [GlucoseGraphRenderer.drawTooltip]
 *     so it scrolls with the data — no Popup needed.
 *  5. [LaunchedEffect] auto-scrolls to the rightmost (most recent) data on load.
 *  6. Pixel mapping is computed in [remember] during composition — never inside the
 *     Canvas draw lambda — to avoid Compose ordering violations.
 */
@Composable
private fun ChartCanvas(points: List<GraphPoint>, crosshairIndex: Int?, onCrosshairMoved: (Int?) -> Unit, animateEntry: Boolean) {
    if (points.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No data in this range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val glucoseColors = MaterialTheme.glucoseColors
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipText = MaterialTheme.colorScheme.inverseOnSurface

    // Unit detection: use the region stored in each point — no value-threshold heuristic.
    val isMmol = points.first().region.isMetric
    val axisMin = if (isMmol) LIBRE_MIN_MMOL else LIBRE_MIN_MGDL
    val axisMax = if (isMmol) LIBRE_MAX_MMOL else LIBRE_MAX_MGDL
    val tgtLow = if (isMmol) TARGET_LOW_MMOL else TARGET_LOW_MGDL
    val tgtHigh = if (isMmol) TARGET_HIGH_MMOL else TARGET_HIGH_MGDL
    val gridStep = if (isMmol) 5.0 else 50.0

    val lineColor: Color =
        when (points.last().color) {
            2 -> glucoseColors.slightlyHigh
            3 -> glucoseColors.high
            4 -> glucoseColors.low
            else -> glucoseColors.inRange
        }

    val drawProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "GraphDrawProgress"
    )

    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Jump to most-recent (right) data when a new range loads
    LaunchedEffect(points.firstOrNull()?.timestamp) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    // Dismiss crosshair while the user is panning
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) onCrosshairMoved(null)
    }

    // Canvas size captured via onSizeChanged. Pixel mapping computed in remember()
    // so that the draw lambda only reads — never writes — state (Compose ordering rule).
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val mappedPoints: List<Offset> =
        remember(points, canvasSize, axisMin, axisMax) {
            if (canvasSize.width == 0f || canvasSize.height == 0f) return@remember emptyList()
            val chartWidth = canvasSize.width - LABEL_MARGIN_PX
            val chartHeight = canvasSize.height - BOTTOM_PAD_PX
            val yRange = axisMax - axisMin
            val timeStart = points.first().timestamp
            val timeEnd = points.last().timestamp
            val timeSpan = (timeEnd - timeStart).coerceAtLeast(1L)
            points.map { pt ->
                Offset(
                    x =
                    LABEL_MARGIN_PX +
                        ((pt.timestamp - timeStart).toFloat() / timeSpan) * chartWidth,
                    y =
                    canvasSize.height - BOTTOM_PAD_PX -
                        ((pt.value - axisMin) / yRange * chartHeight).toFloat()
                )
            }
        }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = constraints.maxWidth.toFloat()

        // Minimum 8dp per data point gives a natural, non-cramped chart
        val minSpacingPx = with(density) { 8.dp.toPx() }
        val virtualWidthPx =
            (points.size * minSpacingPx + LABEL_MARGIN_PX)
                .coerceAtLeast(screenWidthPx)
        val virtualWidthDp = with(density) { virtualWidthPx.toDp() }

        Row(
            modifier =
            Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier =
                Modifier
                    .width(virtualWidthDp)
                    .fillMaxHeight()
                    .onSizeChanged { sz ->
                        canvasSize = Size(sz.width.toFloat(), sz.height.toFloat())
                    }.pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            val idx = GlucoseGraphRenderer.findNearestPointIndex(
                                tapOffset.x,
                                mappedPoints
                            )
                            if (idx == crosshairIndex) {
                                onCrosshairMoved(null)
                            } else {
                                onCrosshairMoved(idx)
                            }
                        }
                    }
            ) {
                if (size.width == 0f || size.height == 0f) return@Canvas
                if (mappedPoints.isEmpty()) return@Canvas

                val chartWidth = size.width - LABEL_MARGIN_PX
                val chartHeight = size.height - BOTTOM_PAD_PX
                val yRange = axisMax - axisMin

                fun valueToY(v: Double): Float = size.height - BOTTOM_PAD_PX - ((v - axisMin) / yRange * chartHeight).toFloat()

                with(GlucoseGraphRenderer) {
                    // 1. Target band
                    drawTargetBand(
                        lowY = valueToY(tgtLow),
                        highY = valueToY(tgtHigh),
                        bandColor = glucoseColors.inRange.copy(alpha = 0.10f)
                    )

                    // 2. Gridlines + labels
                    drawGridlines(
                        minValue = axisMin,
                        maxValue = axisMax,
                        stepValue = gridStep,
                        targetLow = tgtLow,
                        targetHigh = tgtHigh,
                        gridColor = onSurface.copy(alpha = 0.18f),
                        targetColor = glucoseColors.inRange.copy(alpha = 0.55f),
                        textMeasurer = textMeasurer,
                        labelColor = onSurface.copy(alpha = 0.60f),
                        valueToY = ::valueToY
                    )

                    // 3. Bezier line + fill + glow
                    drawGlucoseLine(
                        points = mappedPoints,
                        lineColor = lineColor,
                        progress = if (animateEntry) drawProgress else 1f,
                        surfaceColor = surfaceColor
                    )

                    // 4. Crosshair + on-canvas tooltip
                    if (crosshairIndex != null && crosshairIndex in mappedPoints.indices) {
                        drawCrosshair(
                            x = mappedPoints[crosshairIndex].x,
                            color = onSurface.copy(alpha = 0.55f)
                        )
                        val pt = points[crosshairIndex]
                        drawTooltip(
                            x = mappedPoints[crosshairIndex].x,
                            y = mappedPoints[crosshairIndex].y,
                            valueLabel = "%.1f".format(pt.value),
                            timeLabel = timeFormat.format(Date(pt.timestamp)),
                            textMeasurer = textMeasurer,
                            bgColor = tooltipBg,
                            textColor = tooltipText
                        )
                    }
                }
            }
        }
    }
}
