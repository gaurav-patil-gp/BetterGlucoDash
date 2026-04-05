package com.eggyswarehouse.betterglucodash.ui.dashboard.graph

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Stateless [DrawScope] extension functions for rendering the glucose graph (PRD §4.2).
 *
 * Each function is small and single-purpose so they can be composed and unit-tested
 * in isolation. No Compose state, ViewModel, or Android framework references here.
 *
 * Dark-mode contrast tuning:
 *  - Grid alpha bumped from 0.12 → 0.18 so gridlines are visible on deep navy.
 *  - Label alpha bumped from 0.35 → 0.60 for legibility.
 *  - Line stroke width 8 → 10 for visual weight on large screens.
 *  - The glow halo uses a wider spread (24f) for richness on dark backgrounds.
 */
object GlucoseGraphRenderer {
    // ── Public drawing functions ──────────────────────────────────────────────

    /**
     * Fills the in-range target band (3.9–10 mmol/L or 70–180 mg/dL).
     */
    fun DrawScope.drawTargetBand(lowY: Float, highY: Float, bandColor: Color) {
        drawRect(
            color = bandColor,
            topLeft = Offset(0f, highY),
            size = Size(size.width, lowY - highY)
        )
    }

    /**
     * Horizontal dashed gridlines at [stepValue] intervals with Y-axis value labels.
     *
     * Target boundary lines are drawn more prominently than generic gridlines.
     *
     * @param minValue     Axis minimum (mmol/L or mg/dL depending on region).
     * @param maxValue     Axis maximum.
     * @param stepValue    Interval between regular gridlines (5 for mmol/L, 50 for mg/dL).
     */
    fun DrawScope.drawGridlines(
        minValue: Double,
        maxValue: Double,
        stepValue: Double,
        targetLow: Double,
        targetHigh: Double,
        gridColor: Color,
        targetColor: Color,
        textMeasurer: TextMeasurer,
        labelColor: Color,
        valueToY: (Double) -> Float
    ) {
        val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
        val dashEffect = { on: Float, off: Float ->
            androidx.compose.ui.graphics.PathEffect
                .dashPathEffect(floatArrayOf(on, off), 0f)
        }

        // ── Step gridlines (regular 5 mmol / 50 mg/dL intervals) ─────────────
        // Skip drawing target lines here — they are drawn explicitly below so they
        // always appear at their exact position (3.9 mmol is NOT on a 5-step boundary).
        var level = ceil(minValue / stepValue) * stepValue
        while (level <= maxValue) {
            val y = valueToY(level)
            val isTarget = abs(level - targetLow) < 0.01 || abs(level - targetHigh) < 0.01
            if (!isTarget) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = dashEffect(8f, 10f)
                )
            }
            val labelText = "%.0f".format(level)
            val measured = textMeasurer.measure(labelText, labelStyle)
            drawText(textMeasurer, labelText, Offset(4f, y - measured.size.height - 2f), labelStyle)
            level += stepValue
        }

        // ── Target boundary lines — always drawn at their exact values ────────
        // This guarantees the 3.9 mmol/L low-target line is visible even though
        // it falls between the 0 and 5 step gridlines.
        for (target in listOf(targetLow, targetHigh)) {
            if (target in minValue..maxValue) {
                val y = valueToY(target)
                drawLine(
                    color = targetColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f,
                    pathEffect = dashEffect(12f, 6f)
                )
                // Add a label for targets that don't coincide with a step gridline
                val onStep = (target % stepValue) < 0.05 || (stepValue - target % stepValue) < 0.05
                if (!onStep) {
                    val text = "%.1f".format(target)
                    val measured = textMeasurer.measure(text, labelStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        topLeft = Offset(4f, y - measured.size.height - 2f),
                        style = labelStyle.copy(color = targetColor)
                    )
                }
            }
        }
    }

    /**
     * Smooth cubic Bezier line through [points] with gradient fill and glow halo.
     *
     * Animatable via [progress] (0→1) for the left-to-right entry draw.
     * A bullseye dot marks the most recent (rightmost) data point.
     * No per-point dots — the smooth line is the primary visual element.
     */
    fun DrawScope.drawGlucoseLine(
        points: List<Offset>,
        lineColor: Color,
        progress: Float = 1f,
        // Default matches the OLED dark-navy surface colour used inside the chart canvas.
        surfaceColor: Color = Color(0xFF0D1B2E)
    ) {
        if (points.isEmpty()) return

        val curvePath = buildBezierPath(points)

        val visiblePath =
            if (progress < 1f) {
                val measure = PathMeasure()
                measure.setPath(curvePath, false)
                Path().also { measure.getSegment(0f, measure.length * progress, it, true) }
            } else {
                curvePath
            }

        val visibleCount = (points.size * progress).toInt().coerceIn(1, points.size)

        // Gradient area fill
        val fillPath =
            Path().apply {
                moveTo(points.first().x, size.height)
                lineTo(points.first().x, points.first().y)
                for (i in 0 until visibleCount - 1) {
                    val ctrl = (points[i].x + points[i + 1].x) / 2f
                    cubicTo(
                        ctrl,
                        points[i].y,
                        ctrl,
                        points[i + 1].y,
                        points[i + 1].x,
                        points[i + 1].y
                    )
                }
                lineTo(points[visibleCount - 1].x, size.height)
                close()
            }
        drawPath(
            path = fillPath,
            brush =
            Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0f)),
                startY = 0f,
                endY = size.height
            )
        )

        // Glow halo — wider on dark backgrounds for visual richness
        drawPath(
            path = visiblePath,
            color = lineColor.copy(alpha = 0.20f),
            style = Stroke(width = 24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Main line — 10f for visual weight on dark substrate
        drawPath(
            path = visiblePath,
            color = lineColor,
            style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bullseye tip dot at latest reading
        val tip = points[visibleCount - 1]
        drawCircle(color = lineColor, radius = 11f, center = tip)
        drawCircle(color = surfaceColor, radius = 5f, center = tip)
    }

    /**
     * Dashed vertical crosshair line at [x] for the tap-to-inspect interaction.
     */
    fun DrawScope.drawCrosshair(x: Float, color: Color) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 2f,
            pathEffect =
            androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(12f, 8f),
                0f
            )
        )
    }

    /**
     * Draws the crosshair tooltip directly on the canvas (no Popup needed).
     *
     * Rendering on-canvas instead of using a floating Popup means the tooltip
     * scrolls naturally with the chart when horizontal-scroll is active.
     *
     * The tooltip is clamped to the canvas bounds so it never clips off-screen.
     */
    fun DrawScope.drawTooltip(
        x: Float,
        y: Float,
        valueLabel: String,
        timeLabel: String,
        textMeasurer: TextMeasurer,
        bgColor: Color,
        textColor: Color
    ) {
        val valueMeasured =
            textMeasurer.measure(
                valueLabel,
                TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
            )
        val timeMeasured =
            textMeasurer.measure(
                timeLabel,
                TextStyle(fontSize = 10.sp, color = textColor.copy(alpha = 0.65f))
            )

        val padH = 14f
        val padV = 10f
        val contentW = max(valueMeasured.size.width, timeMeasured.size.width).toFloat()
        val contentH = valueMeasured.size.height + timeMeasured.size.height + 4f
        val tooltipW = contentW + padH * 2
        val tooltipH = contentH + padV * 2

        // Position above the crosshair point, clamped to canvas bounds
        var tx = x - tooltipW / 2f
        var ty = y - tooltipH - 18f
        tx = tx.coerceIn(4f, size.width - tooltipW - 4f)
        ty = ty.coerceAtLeast(4f)

        drawRoundRect(
            color = bgColor,
            topLeft = Offset(tx, ty),
            size = Size(tooltipW, tooltipH),
            cornerRadius = CornerRadius(10f)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = valueLabel,
            topLeft = Offset(tx + padH, ty + padV),
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = timeLabel,
            topLeft = Offset(tx + padH, ty + padV + valueMeasured.size.height + 4f),
            style = TextStyle(fontSize = 10.sp, color = textColor.copy(alpha = 0.65f))
        )
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /**
     * Returns the index of the point whose X-coordinate is closest to [x].
     * Returns -1 if [points] is empty.
     */
    fun findNearestPointIndex(x: Float, points: List<Offset>): Int {
        if (points.isEmpty()) return -1
        return points.indices.minByOrNull { abs(points[it].x - x) } ?: -1
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildBezierPath(points: List<Offset>): Path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val ctrl = (points[i].x + points[i + 1].x) / 2f
            cubicTo(ctrl, points[i].y, ctrl, points[i + 1].y, points[i + 1].x, points[i + 1].y)
        }
    }
}
