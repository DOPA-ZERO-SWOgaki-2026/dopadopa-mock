package com.example.dopadopa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.dopadopa.state.UsageSlice

/**
 * 「使用時間の内訳」用のドーナツグラフ。Swift Charts の SectorMark 相当を
 * Compose の Canvas で自前描画したもの。
 */
@Composable
fun DonutChart(slices: List<UsageSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.seconds }
    Canvas(modifier = modifier) {
        if (total <= 0.0) return@Canvas
        val strokeWidth = size.minDimension * (1f - 0.62f)
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)

        var startAngle = -90f
        val gapDegrees = 2f
        slices.filter { it.seconds > 0 }.forEach { slice ->
            val sweep = (slice.seconds / total * 360.0).toFloat()
            val drawnSweep = (sweep - gapDegrees).coerceAtLeast(0f)
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = drawnSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
            )
            startAngle += sweep
        }
    }
}
