package com.gh.healthapp.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gh.healthapp.ui.theme.HealthAppTheme
import java.util.Locale

data class LinePoint(val x: Float, val y: Float, val label: String? = null)

@Composable
fun LineChart(
    points: List<LinePoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    yFormatter: (Float) -> String = { String.format(Locale.KOREAN, "%.1f", it) },
    minYSpan: Float? = null,
) {
    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text("데이터 없음", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    val sorted = points.sortedBy { it.x }
    var minY = sorted.minOf { it.y }
    var maxY = sorted.maxOf { it.y }
    // 값 범위가 minYSpan보다 좁으면 중앙 기준으로 넓힘 (변화 과장 방지)
    if (minYSpan != null && maxY - minY < minYSpan) {
        val mid = (maxY + minY) / 2f
        minY = mid - minYSpan / 2f
        maxY = mid + minYSpan / 2f
    }
    if (maxY - minY == 0f) { minY -= 1f; maxY += 1f }
    // 상하 10% 여백
    val margin = (maxY - minY) * 0.1f
    minY -= margin
    maxY += margin
    val minX = sorted.first().x
    val maxX = sorted.last().x
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(height)) {
            Column(
                Modifier.width(48.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(yFormatter(maxY), style = MaterialTheme.typography.labelSmall)
                Text(yFormatter(minY), style = MaterialTheme.typography.labelSmall)
            }
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val pad = 6.dp.toPx()
                val w = size.width - 2 * pad
                val h = size.height - 2 * pad
                drawLine(axisColor, Offset(0f, pad), Offset(size.width, pad), 1f)
                drawLine(axisColor, Offset(0f, size.height - pad), Offset(size.width, size.height - pad), 1f)
                fun pos(p: LinePoint): Offset {
                    val fx = if (maxX == minX) 0.5f else (p.x - minX) / (maxX - minX)
                    val fy = (p.y - minY) / (maxY - minY)
                    return Offset(pad + w * fx, pad + h * (1f - fy))
                }
                val offs = sorted.map(::pos)
                for (i in 0 until offs.size - 1) {
                    drawLine(lineColor, offs[i], offs[i + 1], strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                }
                offs.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = it) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 48.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(sorted.first().label ?: "", style = MaterialTheme.typography.labelSmall)
            if (sorted.size > 1) Text(sorted.last().label ?: "", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LineChartPreview() {
    HealthAppTheme {
        LineChart(
            listOf(
                LinePoint(0f, 73.1f, "11월 1일"), LinePoint(5f, 72.6f), LinePoint(12f, 72.9f),
                LinePoint(20f, 72.0f), LinePoint(30f, 71.8f, "11월 23일"),
            ),
        )
    }
}
