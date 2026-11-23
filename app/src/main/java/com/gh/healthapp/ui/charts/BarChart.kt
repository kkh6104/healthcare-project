package com.gh.healthapp.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gh.healthapp.ui.theme.HealthAppTheme

data class BarItem(val label: String, val value: Float, val highlight: Boolean = false)

@Composable
fun BarChart(
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
    goal: Float? = null,
) {
    if (items.isEmpty() || items.all { it.value <= 0f }) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text("데이터 없음", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val maxV = maxOf(items.maxOf { it.value }, goal ?: 0f).coerceAtLeast(1f)
    val textArea = 18.dp
    val plotH = height - textArea

    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(height)) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                items.forEach { item ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            valueFormatter(item.value),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1,
                        )
                        Box(
                            Modifier
                                .padding(horizontal = 6.dp)
                                .fillMaxWidth()
                                .height(plotH * (item.value / maxV))
                                .background(
                                    if (item.highlight) primary else primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                ),
                        )
                    }
                }
            }
            if (goal != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height - plotH.toPx() * (goal / maxV)
                    drawLine(
                        outline, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                    )
                }
                Text(
                    "목표",
                    Modifier.align(Alignment.TopEnd).padding(top = (height - plotH * (goal / maxV) - 14.dp).coerceAtLeast(0.dp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = outline,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            items.forEach {
                Text(
                    it.label, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BarChartPreview() {
    HealthAppTheme {
        BarChart(
            listOf(
                BarItem("월", 4200f), BarItem("화", 8100f), BarItem("수", 6500f),
                BarItem("목", 9800f), BarItem("금", 3000f), BarItem("토", 7000f), BarItem("일", 5200f, true),
            ),
            goal = 8000f,
        )
    }
}
