package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.data.model.Report
import java.util.Locale

@Composable
fun GpaTrendChart(
    reports: List<Report>,
    modifier: Modifier = Modifier
) {
    if (reports.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No historical term reports on record yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 24f
        isAntiAlias = true
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val width = size.width
            val height = size.height - 30f // Reserve bottom space for labels

            // Reference grid lines for GPA 4.0, 3.0, 2.0
            val gpaLevels = listOf(4.0, 3.0, 2.0, 1.0)
            for (gpa in gpaLevels) {
                val y = height - (gpa.toFloat() / 4.0f) * height
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.1f", gpa),
                    0f,
                    y - 6f,
                    textPaint
                )
            }

            if (reports.size == 1) {
                // Single point
                val gpa = reports.first().termGpa.toFloat()
                val cy = height - (gpa / 4.0f) * height
                val cx = width / 2f
                drawCircle(color = primaryColor, radius = 7.dp.toPx(), center = Offset(cx, cy))
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.2f", gpa),
                    cx - 20f,
                    cy - 16f,
                    textPaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    reports.first().termName.take(12),
                    cx - 30f,
                    size.height,
                    textPaint
                )
                return@Canvas
            }

            // Multiple points: Plot line & filled curve
            val stepX = width / (reports.size - 1)
            val points = reports.mapIndexed { index, report ->
                val x = index * stepX
                val y = height - (report.termGpa.toFloat() / 4.0f) * height
                Offset(x, y)
            }

            // Path for gradient area
            val fillPath = Path().apply {
                moveTo(points.first().x, height)
                for (p in points) {
                    lineTo(p.x, p.y)
                }
                lineTo(points.last().x, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // Line Path
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path = strokePath,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw data circles and labels
            points.forEachIndexed { i, pt ->
                drawCircle(color = secondaryColor, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = pt)

                // GPA Value text
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.2f", reports[i].termGpa),
                    pt.x - 16f,
                    pt.y - 12f,
                    textPaint
                )

                // Term label text
                val label = reports[i].termName.take(8)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    pt.x - 20f,
                    size.height,
                    textPaint
                )
            }
        }
    }
}
