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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val maxV = maxOf(items.maxOf { it.value }, goal ?: 0f).coerceAtLeast(1f)
    val textArea = 18.dp
    val plotH = height - textArea
    // 막대가 많으면 값 글자 생략, 막대 간격 축소 (좁은 칸에서 고정 여백 때문에 폭이 0이 되는 문제 방지)
    val many = items.size > 14
    val gap = when {
        items.size <= 8 -> 6.dp
        items.size <= 14 -> 3.dp
        else -> 0.5.dp
    }
    val goalH: Dp? = goal?.let { plotH * (it / maxV) }

    Row(modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().height(height)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                    items.forEach { item ->
                        val barH = plotH * (item.value / maxV)
                        // 값 글자 영역(막대 위 18dp)에 목표선이 걸리면 글자를 막대 안쪽 위로 옮긴다
                        val overlaps = goalH != null && goalH >= barH && goalH - barH < textArea
                        val inside = overlaps && barH >= textArea && !many
                        val barColor = if (item.highlight) primary else primary.copy(alpha = 0.4f)
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            if (!inside && !many) {
                                Text(
                                    valueFormatter(item.value),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1,
                                )
                            }
                            Box(
                                Modifier
                                    .padding(horizontal = gap)
                                    .fillMaxWidth()
                                    .height(barH)
                                    .background(barColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                if (inside) {
                                    Text(
                                        valueFormatter(item.value),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (item.highlight) onPrimary else onSurface,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
                if (goalH != null) {
                    Canvas(Modifier.fillMaxSize()) {
                        val y = size.height - goalH.toPx()
                        drawLine(
                            outline, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                items.forEach {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        // 칸보다 넓은 라벨도 잘리지 않고 칸 중앙에 그려지게 함
                        Text(
                            it.label,
                            Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
        if (goalH != null) {
            // 차트 영역 밖(오른쪽 여백)에 "목표" 라벨
            Box(Modifier.width(32.dp).height(height)) {
                Text(
                    "목표",
                    Modifier.padding(start = 4.dp, top = (height - goalH - 8.dp).coerceAtLeast(0.dp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = outline,
                    maxLines = 1,
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
