package com.example.dopadopa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dopadopa.ui.theme.Theme
import kotlin.math.min

/** index.html の #ringProgress の SVG リングを Compose の Canvas で再現したもの。 */
@Composable
fun ProgressRing(
    ratio: Double,
    centerLabel: String,
    centerValue: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = min(size.width, size.height)
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f + strokeWidth / 2f,
                (size.height - diameter) / 2f + strokeWidth / 2f,
            )
            val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)

            drawArc(
                color = Color.Black.copy(alpha = 0.06f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            val sweep = (ratio.coerceIn(0.0, 1.0) * 360.0).toFloat()
            if (sweep > 0f) {
                drawArc(
                    brush = Theme.ringGradient,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
        }

        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = centerLabel, fontSize = 11.sp, color = Theme.subtleInk)
                Text(
                    text = centerValue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.ink,
                    maxLines = 1,
                )
            }
        }
    }
}
