package com.gh.healthapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gh.healthapp.HeartRateSummary
import com.gh.healthapp.Section
import com.gh.healthapp.StepsUiState
import com.gh.healthapp.ui.charts.BarChart
import com.gh.healthapp.ui.charts.BarItem
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** 활동 탭: 걸음 카드(7일 막대 그래프) + 심박 카드 */
@Composable
fun ActivityTab(state: StepsUiState.Loaded) {
    val hr = (state.heartRate as? Section.Ok)?.data
    val steps = (state.steps as? Section.Ok)?.data.orEmpty()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (steps.isNotEmpty()) item { StepsCard(steps) }
        if (hr != null) item { HeartRateCard(hr) }
    }
}

@Composable
private fun StepsCard(steps: List<Pair<java.time.LocalDate, Long>>) {
    val fmt = NumberFormat.getInstance(Locale.KOREA)
    val dateFmt = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
    val ordered = steps.sortedBy { it.first }
    val today = ordered.last()
    val avg = ordered.sumOf { it.second } / ordered.size
    val best = ordered.maxByOrNull { it.second }!!
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("걸음", style = MaterialTheme.typography.titleMedium)
            Text("${fmt.format(today.second)} 걸음", style = MaterialTheme.typography.displaySmall)
            BarChart(
                items = ordered.map {
                    BarItem(
                        it.first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                        it.second.toFloat(),
                        highlight = it.first == today.first,
                    )
                },
                valueFormatter = { fmt.format(it.toLong()) },
                goal = 6000f,
            )
            Text(
                "7일 평균 ${fmt.format(avg)}걸음 · 최고 ${fmt.format(best.second)}걸음(${best.first.format(dateFmt)})",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun HeartRateCard(hr: HeartRateSummary) {
    val timeFmt = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN).withZone(ZoneId.systemDefault())
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("심박수 (오늘)", style = MaterialTheme.typography.titleMedium)
            if (hr.latestBpm == null) {
                Text("오늘 심박수 기록이 없습니다.")
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("최근 ${hr.latestBpm} bpm", style = MaterialTheme.typography.displaySmall)
                    if (hr.latestTime != null) {
                        Text(timeFmt.format(hr.latestTime), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("평균 ${hr.avg ?: "-"} · 최저 ${hr.min ?: "-"} · 최고 ${hr.max ?: "-"} bpm")
            }
        }
    }
}
