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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val bodyDateFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

private fun fmt1(v: Double) = String.format(Locale.KOREAN, "%.1f", v)

private fun metricLine(s: MetricSummary): String {
    val latest = s.latest ?: return "${s.metric.label}   기록 없음"
    val sb = StringBuilder("${s.metric.label}   ${fmt1(latest.value)} ${s.metric.unit}   (${bodyDateFormatter.format(latest.date)})")
    s.changeFromWeekAgo?.let { c ->
        val zero = fmt1(c).trimStart('-', '+').all { it == '0' || it == '.' }
        sb.append("   ")
        sb.append(if (zero) "변화 없음" else "1주 전보다 " + String.format(Locale.KOREAN, "%+.1f", c))
    }
    return sb.toString()
}

@Composable
fun BodyCompositionCard(summaries: List<MetricSummary>, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("체성분", style = MaterialTheme.typography.titleMedium)
            summaries.forEach { Text(metricLine(it), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
