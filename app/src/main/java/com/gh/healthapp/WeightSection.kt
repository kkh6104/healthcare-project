package com.gh.healthapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)
private val dayFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

@Composable
fun WeightCard(summary: WeightSummary, modifier: Modifier = Modifier) {
    val zone = ZoneId.systemDefault()
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("몸무게", style = MaterialTheme.typography.titleMedium)
            val latest = summary.latest
            if (latest == null) {
                Text("최근 30일 몸무게 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(String.format(Locale.KOREAN, "%.1f kg", latest.kg), style = MaterialTheme.typography.headlineMedium)
                Text(
                    timeFormatter.format(latest.time.atZone(zone)),
                    style = MaterialTheme.typography.bodySmall,
                )
                summary.changeKg?.let { c ->
                    val text = if (String.format(Locale.KOREAN, "%.1f", c).trimStart('-', '+').all { it == '0' || it == '.' }) {
                        "변화 없음"
                    } else {
                        "1주 전보다 " + String.format(Locale.KOREAN, "%+.1f kg", c)
                    }
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    summary.recent.take(7).forEach { e ->
                        Text(
                            dayFormatter.format(e.date) + "   " + String.format(Locale.KOREAN, "%.1f kg", e.kg),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
