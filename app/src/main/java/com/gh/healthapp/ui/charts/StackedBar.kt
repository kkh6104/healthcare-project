package com.gh.healthapp.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gh.healthapp.ui.theme.HealthAppTheme

data class Segment(val label: String, val value: Float, val color: Color)

object SleepColors {
    @Composable fun deep(): Color = MaterialTheme.colorScheme.primary
    @Composable fun light(): Color = MaterialTheme.colorScheme.secondary
    @Composable fun rem(): Color = MaterialTheme.colorScheme.tertiary
    @Composable fun awake(): Color = MaterialTheme.colorScheme.error
}

@Composable
fun StackedBar(segments: List<Segment>, modifier: Modifier = Modifier, height: Dp = 20.dp) {
    val total = segments.sumOf { it.value.coerceAtLeast(0f).toDouble() }.toFloat()
    if (total <= 0f) {
        Text("데이터 없음", modifier, style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2))) {
            segments.filter { it.value > 0f }.forEach {
                Box(Modifier.weight(it.value).height(height).background(it.color))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            segments.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(it.color, CircleShape))
                    Text(
                        "${it.label} ${Math.round(it.value.coerceAtLeast(0f) / total * 100)}%",
                        Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StackedBarPreview() {
    HealthAppTheme {
        StackedBar(
            listOf(
                Segment("깊은", 90f, SleepColors.deep()), Segment("얕은", 240f, SleepColors.light()),
                Segment("렘", 80f, SleepColors.rem()), Segment("깸", 30f, SleepColors.awake()),
            ),
        )
    }
}
