package com.gh.healthapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gh.healthapp.BodyMetric
import com.gh.healthapp.MetricEntry
import com.gh.healthapp.MetricSummary
import com.gh.healthapp.Section
import com.gh.healthapp.StepsUiState
import com.gh.healthapp.WeightSummary
import com.gh.healthapp.ui.detail.DetailHeader
import com.gh.healthapp.ui.detail.DetailKind
import com.gh.healthapp.ui.charts.LinePoint
import com.gh.healthapp.ui.charts.LineChart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("M/d", Locale.KOREAN)
private val longDate = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

private fun f1(v: Double) = String.format(Locale.KOREAN, "%.1f", v)

private fun changeText(c: Double?): String? {
    c ?: return null
    val zero = f1(c).trimStart('-', '+').all { it == '0' || it == '.' }
    return if (zero) "변화 없음" else "1주 전보다 " + String.format(Locale.KOREAN, "%+.1f", c)
}

private fun points(entries: List<Pair<LocalDate, Double>>): List<LinePoint> =
    entries.sortedBy { it.first }.map { (d, v) -> LinePoint(d.toEpochDay().toFloat(), v.toFloat(), d.format(shortDate)) }

/** 몸 탭: 체중 그래프 + 체성분 목록(체지방률 그래프) */
@Composable
fun BodyTab(state: StepsUiState.Loaded, onOpenDetail: (DetailKind) -> Unit) {
    val weight = (state.weight as? Section.Ok)?.data
    val body = state.body.filterIsInstance<Section.Ok<MetricSummary>>().map { it.data }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (weight != null) WeightChartCard(weight) { onOpenDetail(DetailKind.WEIGHT) }
        if (body.isNotEmpty()) BodyChartCard(body) { onOpenDetail(DetailKind.BODY_COMPOSITION) }
    }
}

@Composable
private fun WeightChartCard(w: WeightSummary, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailHeader("체중")
            val latest = w.latest
            if (latest == null) {
                Text("최근 30일 몸무게 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("${f1(latest.kg)} kg", style = MaterialTheme.typography.displaySmall)
                Text(latest.date.format(longDate), style = MaterialTheme.typography.bodySmall)
                changeText(w.changeKg)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                LineChart(points(w.recent.map { it.date to it.kg }), yFormatter = { f1(it.toDouble()) }, minYSpan = 6f)
            }
        }
    }
}

@Composable
private fun BodyChartCard(list: List<MetricSummary>, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailHeader("체성분")
            list.forEach { s ->
                val latest: MetricEntry? = s.latest
                if (latest == null) {
                    Text("${s.metric.label}   기록 없음", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val change = changeText(s.changeFromWeekAgo)?.let { "   $it" } ?: ""
                    Text(
                        "${s.metric.label}   ${f1(latest.value)} ${s.metric.unit}   (${latest.date.format(longDate)})$change",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (s.metric == BodyMetric.BODY_FAT) {
                        LineChart(
                            points(s.recent.map { it.date to it.value }),
                            height = 120.dp,
                            minYSpan = 10f,
                            yFormatter = { f1(it.toDouble()) },
                        )
                    }
                }
            }
        }
    }
}
